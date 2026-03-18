#!/bin/bash
# Test your Spring Boot JWT authentication

echo "=== Testing Backend JWT Authentication ==="
echo ""

# Step 1: Login and capture token
echo "1. Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}')

echo "Login Response: $LOGIN_RESPONSE"
echo ""

# Extract token (assumes response is {"token":"..."})
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ ERROR: No token received from backend!"
  exit 1
fi

echo "✅ Token received: ${TOKEN:0:50}..."
echo ""

# Step 2: Decode token (base64) to see payload
echo "2. Decoding token payload..."
PAYLOAD=$(echo $TOKEN | cut -d'.' -f2)
# Add padding if needed
PADDING=$((4 - ${#PAYLOAD} % 4))
if [ $PADDING -ne 4 ]; then
  PAYLOAD="${PAYLOAD}$(printf '%*s' $PADDING | tr ' ' '=')"
fi
echo "Token Payload:"
echo $PAYLOAD | base64 -d 2>/dev/null | jq '.' 2>/dev/null || echo $PAYLOAD | base64 -d
echo ""

# Step 3: Test protected endpoint
echo "3. Testing protected endpoint with token..."
curl -v -X GET http://localhost:8080/api/admin/dashboard/stats \
  -H "Authorization: Bearer $TOKEN" \
  2>&1 | grep -E "< HTTP|< Content-Type|^{"

echo ""
echo "=== Test Complete ==="
