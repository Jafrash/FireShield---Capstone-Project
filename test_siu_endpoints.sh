#!/bin/bash

# SIU API Endpoint Test Script
# This script tests the SIU investigator endpoints affected by the 403 errors

echo "=== Testing SIU Investigator API Endpoints ==="
echo "Date: $(date)"
echo

BASE_URL="http://localhost:8080/api/fraud"

# Function to make authenticated API calls
make_api_call() {
    local endpoint="$1"
    local method="${2:-GET}"
    local description="$3"

    echo "Testing: $description"
    echo "Endpoint: $method $endpoint"

    # Note: Replace with actual JWT token when testing
    response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
        -X "$method" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE" \
        "$endpoint" 2>/dev/null)

    http_code=$(echo "$response" | grep "HTTP_STATUS" | cut -d: -f2)
    body=$(echo "$response" | sed '/HTTP_STATUS/d')

    echo "Status: $http_code"
    if [ "$http_code" = "200" ]; then
        echo "✅ SUCCESS"
        echo "Response preview: $(echo "$body" | head -c 100)..."
    elif [ "$http_code" = "403" ]; then
        echo "❌ FORBIDDEN - Security issue persists"
    else
        echo "⚠️  Status $http_code"
        echo "Response: $body"
    fi
    echo "----------------------------------------"
}

# Test the main SIU endpoints that were failing
echo "1. Testing SIU Investigators endpoint (main issue):"
make_api_call "$BASE_URL/siu/investigators" "GET" "Get all SIU investigators"

echo "2. Testing SIU Cases endpoint:"
make_api_call "$BASE_URL/siu/cases" "GET" "Get all SIU cases"

echo "3. Testing SIU Cases overview:"
make_api_call "$BASE_URL/siu/cases/all" "GET" "Get SIU cases for dashboard"

echo "4. Testing investigator-specific endpoints:"
make_api_call "$BASE_URL/siu/investigator/1/cases" "GET" "Get cases for investigator ID 1"
make_api_call "$BASE_URL/siu/investigator/1/assigned-cases" "GET" "Get assigned cases for investigator ID 1"
make_api_call "$BASE_URL/siu/investigator/1/statistics" "GET" "Get statistics for investigator ID 1"

echo
echo "=== Test Complete ==="
echo "Note: Replace 'YOUR_JWT_TOKEN_HERE' with actual JWT token from login"
echo "To get JWT token:"
echo "1. Login via /api/auth/login with SIU_INVESTIGATOR credentials"
echo "2. Use the returned token in Authorization header"