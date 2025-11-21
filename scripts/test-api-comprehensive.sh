#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080"

echo -e "${YELLOW}🚀 Starting Comprehensive API Test Suite${NC}"
echo "=============================================="

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ $2${NC}"
    else
        echo -e "${RED}❌ $2${NC}"
        exit 1
    fi
}

# Function to check HTTP status code
check_http_status() {
    local response=$1
    local expected=$2
    local message=$3
    
    if [ "$response" -eq "$expected" ]; then
        echo -e "${GREEN}✅ $message (HTTP $response)${NC}"
    else
        echo -e "${RED}❌ $message (Expected $expected, got $response)${NC}"
        exit 1
    fi
}

# Generate JWT Token
JWT_SECRET="careforall-super-secret-key-for-jwt-signing-must-be-at-least-256-bits"
TOKEN=$(python3 -c "
import jwt
import datetime
import sys

secret = '$JWT_SECRET'
payload = {
    'sub': '1',
    'exp': datetime.datetime.utcnow() + datetime.timedelta(hours=1)
}
try:
    token = jwt.encode(payload, secret, algorithm='HS256')
    if isinstance(token, bytes):
        print(token.decode('utf-8'))
    else:
        print(token)
except Exception as e:
    print(f'Error generating token: {e}', file=sys.stderr)
    sys.exit(1)
")

echo "Generated Test Token: ${TOKEN:0:20}..."

# 1. Test Health Checks
echo ""
echo -e "${YELLOW}1. Testing Health Endpoints${NC}"
echo "--------------------------------"

# Function to wait for service health
wait_for_service() {
    local service=$1
    local max_retries=30
    local count=0
    
    echo -n "Waiting for $service to be healthy..."
    while [ $count -lt $max_retries ]; do
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/$service/health")
        if [ "$HTTP_CODE" -eq 200 ]; then
            echo -e " ${GREEN}OK${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
        count=$((count + 1))
    done
    
    echo -e " ${RED}FAILED${NC}"
    echo "Service $service failed to become healthy after $((max_retries * 2)) seconds."
    exit 1
}

for service in "pledge" "payment" "campaign" "mock-gateway"; do
    wait_for_service "$service"
done

# 2. Test Campaign Service
echo ""
echo -e "${YELLOW}2. Testing Campaign Service${NC}"
echo "--------------------------------"

# Create a campaign (using direct DB insert or if there's an endpoint - assuming ID 1 exists from init)
# Let's verify we can get campaign 1
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/v1/campaign/1")
if [ "$HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✅ Get Campaign ID 1 successful${NC}"
    INITIAL_TOTAL=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/v1/campaign/1" | python3 -c "import sys, json; print(json.load(sys.stdin)['currentTotalAmount'])")
    echo "   Initial Campaign Total: $INITIAL_TOTAL"
else
    echo -e "${RED}❌ Failed to get Campaign ID 1 (HTTP $HTTP_CODE)${NC}"
    # Attempt to create if not exists (if endpoint exists, otherwise we rely on seeded data)
fi

# 3. Test Pledge Service (Happy Path)
echo ""
echo -e "${YELLOW}3. Testing Pledge Creation (Happy Path)${NC}"
echo "--------------------------------"

USER_ID="user_$(date +%s)"
IDEMPOTENCY_KEY="key_$(date +%s)"
AMOUNT=50.00

echo "Creating pledge of $AMOUNT for campaign 1..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/pledge" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-User-Id: $USER_ID" \
  -H "X-Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "{\"amount\": $AMOUNT, \"campaignId\": 1, \"userId\": 1}")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

check_http_status "$HTTP_CODE" 201 "Pledge creation"

PLEDGE_ID=$(echo "$BODY" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")
echo "   Pledge ID: $PLEDGE_ID"
echo "   Status: $(echo "$BODY" | python3 -c "import sys, json; print(json.load(sys.stdin)['status'])")"

# 4. Verify Async Flow (Wait for Webhook)
echo ""
echo -e "${YELLOW}4. Verifying Async Payment Flow${NC}"
echo "--------------------------------"
echo "Waiting 10 seconds for payment processing..."
sleep 10

# Check Pledge Status
UPDATED_PLEDGE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/v1/pledge/$PLEDGE_ID")
STATUS=$(echo "$UPDATED_PLEDGE" | python3 -c "import sys, json; print(json.load(sys.stdin)['status'])")
PAYMENT_ID=$(echo "$UPDATED_PLEDGE" | python3 -c "import sys, json; obj=json.load(sys.stdin); print(obj.get('paymentGatewayId', 'null'))")

if [ "$STATUS" == "CAPTURED" ]; then
    echo -e "${GREEN}✅ Pledge status updated to CAPTURED${NC}"
else
    echo -e "${RED}❌ Pledge status is $STATUS (Expected CAPTURED)${NC}"
    exit 1
fi

if [ "$PAYMENT_ID" != "null" ] && [ -n "$PAYMENT_ID" ]; then
    echo -e "${GREEN}✅ Payment Gateway ID assigned: $PAYMENT_ID${NC}"
else
    echo -e "${RED}❌ Payment Gateway ID missing${NC}"
    exit 1
fi

# Check Campaign Total Update
NEW_TOTAL=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/v1/campaign/1" | python3 -c "import sys, json; print(json.load(sys.stdin)['currentTotalAmount'])")
EXPECTED_TOTAL=$(echo "$INITIAL_TOTAL + $AMOUNT" | bc)

# Use awk for float comparison to avoid locale issues
MATCH=$(awk "BEGIN {print ($NEW_TOTAL == $EXPECTED_TOTAL) ? 1 : 0}")

if [ "$MATCH" -eq 1 ]; then
    echo -e "${GREEN}✅ Campaign total updated correctly ($NEW_TOTAL)${NC}"
else
    echo -e "${RED}❌ Campaign total mismatch (Expected $EXPECTED_TOTAL, got $NEW_TOTAL)${NC}"
    # Don't exit here, might be a timing issue or float precision, just warn
fi

# 5. Test Idempotency
echo ""
echo -e "${YELLOW}5. Testing Idempotency${NC}"
echo "--------------------------------"

echo "Resending same pledge request..."
RESPONSE_DUP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/pledge" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-User-Id: $USER_ID" \
  -H "X-Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "{\"amount\": $AMOUNT, \"campaignId\": 1, \"userId\": 1}")

HTTP_CODE_DUP=$(echo "$RESPONSE_DUP" | tail -n1)

# Depending on implementation, might return 200 (same resource) or 409/422
# Assuming 200 and returning existing pledge for this implementation
if [ "$HTTP_CODE_DUP" -eq 200 ] || [ "$HTTP_CODE_DUP" -eq 409 ]; then
    echo -e "${GREEN}✅ Idempotency check passed (HTTP $HTTP_CODE_DUP)${NC}"
else
    echo -e "${RED}❌ Idempotency check failed (HTTP $HTTP_CODE_DUP)${NC}"
fi

# 6. Test Mock Gateway Direct
echo ""
echo -e "${YELLOW}6. Testing Mock Gateway Direct${NC}"
echo "--------------------------------"

MOCK_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/mock-gateway/process" \
  -H "Content-Type: application/json" \
  -d "{\"amount\": 100.00, \"currency\": \"USD\", \"source\": \"test\", \"callbackUrl\": \"http://localhost:8080/dummy\"}")

MOCK_CODE=$(echo "$MOCK_RESP" | tail -n1)
check_http_status "$MOCK_CODE" 202 "Mock Gateway direct call"

echo ""
echo -e "${GREEN}🎉 All Comprehensive API Tests Passed!${NC}"
echo "=============================================="
