#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🚀 CareForAll Platform - End-to-End Test Script"
echo "================================================"
echo ""

# Check if infrastructure is running
echo "📋 Checking infrastructure..."
if ! docker ps | grep -q careforall-rabbitmq; then
    echo -e "${RED}❌ Infrastructure not running. Please run: docker-compose up -d${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Infrastructure is running${NC}"
echo ""

# Base URLs
PAYMENT_URL="http://localhost:8081/api/v1/payment"
CAMPAIGN_URL="http://localhost:8082/api/v1/campaign"
PLEDGE_URL="http://localhost:8083/api/v1/pledge"

# Test 1: Health Checks
echo "🏥 Test 1: Health Checks"
echo "------------------------"
echo "Checking Payment Service..."
PAYMENT_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" $PAYMENT_URL/health)
if [ "$PAYMENT_HEALTH" == "200" ]; then
    echo -e "${GREEN}✅ Payment Service is healthy${NC}"
else
    echo -e "${RED}❌ Payment Service is down (HTTP $PAYMENT_HEALTH)${NC}"
    exit 1
fi

echo "Checking Campaign Service..."
CAMPAIGN_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" $CAMPAIGN_URL/health)
if [ "$CAMPAIGN_HEALTH" == "200" ]; then
    echo -e "${GREEN}✅ Campaign Service is healthy${NC}"
else
    echo -e "${RED}❌ Campaign Service is down (HTTP $CAMPAIGN_HEALTH)${NC}"
    exit 1
fi
echo ""

# Test 2: Create a Campaign
echo "📝 Test 2: Create a Campaign"
echo "----------------------------"
CAMPAIGN_RESPONSE=$(curl -s -X POST $CAMPAIGN_URL \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test-user-123" \
  -d '{
    "name": "Medical Emergency Fund",
    "description": "Help John recover from surgery",
    "goalAmount": 50000.00
  }')

CAMPAIGN_ID=$(echo $CAMPAIGN_RESPONSE | sed -n 's/.*"id":\([0-9]*\),.*/\1/p')
if [ "$CAMPAIGN_ID" != "null" ] && [ -n "$CAMPAIGN_ID" ]; then
    echo -e "${GREEN}✅ Campaign created with ID: $CAMPAIGN_ID${NC}"
    echo "Campaign Details:"
    echo $CAMPAIGN_RESPONSE
else
    echo -e "${RED}❌ Failed to create campaign${NC}"
    echo $CAMPAIGN_RESPONSE
    exit 1
fi
echo ""

# Test 3: Create a Pledge
echo "💳 Test 3: Create a Pledge"
echo "---------------------------"
PLEDGE_RESPONSE=$(curl -s -X POST $PLEDGE_URL \
  -H "Content-Type: application/json" \
  -H "X-User-Id: donor-456" \
  -d "{
    \"amount\": 5000.00,
    \"campaignId\": $CAMPAIGN_ID
  }")

PLEDGE_ID=$(echo $PLEDGE_RESPONSE | sed -n 's/.*"id":\([0-9]*\),.*/\1/p')
if [ "$PLEDGE_ID" != "null" ] && [ -n "$PLEDGE_ID" ]; then
    echo -e "${GREEN}✅ Pledge created with ID: $PLEDGE_ID${NC}"
    echo "Pledge Details:"
    echo $PLEDGE_RESPONSE
else
    echo -e "${RED}❌ Failed to create pledge${NC}"
    echo $PLEDGE_RESPONSE
    exit 1
fi
echo ""

# Wait for event propagation
echo "⏳ Waiting 10 seconds for event propagation via RabbitMQ..."
sleep 10
echo ""

# Test 4: Get Payment and Simulate Webhook
echo "🔔 Test 4: Get Payment and Simulate Webhook"
echo "-------------------------------------------"
RETRY_COUNT=0
MAX_RETRIES=5
PAYMENT_GATEWAY_ID=""
while [ -z "$PAYMENT_GATEWAY_ID" ] && [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    PAYMENT_DETAILS=$(curl -s $PAYMENT_URL/pledge/$PLEDGE_ID)
    PAYMENT_GATEWAY_ID=$(echo $PAYMENT_DETAILS | sed -n 's/.*"paymentGatewayId":"\([^"]*\)".*/\1/p')
    if [ -z "$PAYMENT_GATEWAY_ID" ]; then
        echo "Could not retrieve paymentGatewayId, retrying in 5 seconds..."
        sleep 5
        RETRY_COUNT=$((RETRY_COUNT+1))
    fi
done

if [ -z "$PAYMENT_GATEWAY_ID" ]; then
    echo -e "${RED}❌ Could not retrieve paymentGatewayId after $MAX_RETRIES retries${NC}"
    echo "Payment Details: $PAYMENT_DETAILS"
    exit 1
fi

echo "Simulating AUTHORIZED webhook for payment gateway ID: $PAYMENT_GATEWAY_ID"
WEBHOOK_RESPONSE=$(curl -s -X POST $PAYMENT_URL/webhooks/status \
  -H "Content-Type: application/json" \
  -d "{
    \"paymentGatewayId\": \"$PAYMENT_GATEWAY_ID\",
    \"status\": \"AUTHORIZED\",
    \"amount\": 5000.00,
    \"campaignId\": $CAMPAIGN_ID,
    \"userId\": \"donor-456\"
  }")

echo "Simulating CAPTURED webhook for payment gateway ID: $PAYMENT_GATEWAY_ID"
WEBHOOK_RESPONSE=$(curl -s -X POST $PAYMENT_URL/webhooks/status \
  -H "Content-Type: application/json" \
  -d "{
    \"paymentGatewayId\": \"$PAYMENT_GATEWAY_ID\",
    \"status\": \"CAPTURED\",
    \"amount\": 5000.00,
    \"campaignId\": $CAMPAIGN_ID,
    \"userId\": \"donor-456\"
  }")

# Wait for event propagation
echo "⏳ Waiting 5 seconds for event propagation via RabbitMQ..."
sleep 5
echo ""

# Test 5: Verify Campaign Total Updated
echo "📊 Test 5: Verify Campaign Total Updated"
echo "-----------------------------------------"
CAMPAIGN_DETAILS=$(curl -s $CAMPAIGN_URL/$CAMPAIGN_ID)
CURRENT_TOTAL=$(echo $CAMPAIGN_DETAILS | sed -n 's/.*"currentTotalAmount":\([0-9.]*\).*/\1/p')

echo "Campaign Total: $CURRENT_TOTAL"
if (( $(echo "$CURRENT_TOTAL >= 5000" | bc -l) )); then
    echo -e "${GREEN}✅ Campaign total updated correctly!${NC}"
    echo "Full campaign details:"
    echo $CAMPAIGN_DETAILS
else
    echo -e "${RED}❌ Campaign total not updated (expected >= 5000, got $CURRENT_TOTAL)${NC}"
    exit 1
fi
echo ""

echo "🎉 ALL TESTS PASSED!"
