#!/bin/bash

echo "🧪 Quick System Test"
echo "===================="
echo ""

# Test 1: Payment Service Health
echo "1. Testing Payment Service..."
PAYMENT_HEALTH=$(curl -s http://localhost:8081/api/v1/payment/health)
echo "Response: $PAYMENT_HEALTH"
echo ""

# Test 2: Create Campaign
echo "2. Creating a campaign..."
CAMPAIGN=$(curl -s -X POST http://localhost:8082/api/v1/campaign \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test-user" \
  -d '{"name":"Test Campaign","description":"Testing","goalAmount":10000}' 2>&1)

if echo "$CAMPAIGN" | grep -q "Failed to connect"; then
    echo "❌ Campaign service not running. Starting it..."
    cd campaign-service && java -jar target/campaign-service-1.0.0.jar &
    CAMPAIGN_PID=$!
    sleep 8
    echo "Campaign service started (PID: $CAMPAIGN_PID)"
    
    CAMPAIGN=$(curl -s -X POST http://localhost:8082/api/v1/campaign \
      -H "Content-Type: application/json" \
      -H "X-User-Id: test-user" \
      -d '{"name":"Test Campaign","description":"Testing","goalAmount":10000}')
fi

echo "Campaign Response: $CAMPAIGN"
CAMPAIGN_ID=$(echo $CAMPAIGN | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', 'ERROR'))" 2>/dev/null || echo "1")
echo "Campaign ID: $CAMPAIGN_ID"
echo ""

# Test 3: Create Pledge
echo "3. Creating a pledge..."
PLEDGE=$(curl -s -X POST http://localhost:8081/api/v1/payment/pledge \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: test-$(date +%s)" \
  -d "{\"paymentGatewayId\":\"pg-test-123\",\"amount\":1000,\"campaignId\":$CAMPAIGN_ID,\"status\":\"CREATED\"}")
echo "Pledge Response: $PLEDGE"
echo ""

# Test 4: Webhook - Capture
echo "4. Simulating webhook (CAPTURED)..."
curl -s -X POST http://localhost:8081/api/v1/payment/webhook \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: webhook-$(date +%s)" \
  -d "{\"paymentGatewayId\":\"pg-test-123\",\"status\":\"CAPTURED\",\"amount\":1000,\"campaignId\":$CAMPAIGN_ID}"
echo ""
echo ""

# Wait for event propagation
echo "5. Waiting for event propagation..."
sleep 5

# Check campaign total
echo "6. Checking campaign total..."
curl -s http://localhost:8082/api/v1/campaign/$CAMPAIGN_ID | python3 -m json.tool 2>/dev/null || curl -s http://localhost:8082/api/v1/campaign/$CAMPAIGN_ID
echo ""

echo "✅ Test complete!"
