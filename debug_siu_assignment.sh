#!/bin/bash

# SIU Assignment Testing Script
# This script helps debug the SIU assignment flow end-to-end

echo "=== SIU Assignment Debugging Script ==="
echo "Date: $(date)"
echo

BASE_URL="http://localhost:8080"

# Function to make API calls with JWT token
make_api_call() {
    local endpoint="$1"
    local method="${2:-GET}"
    local data="$3"
    local description="$4"

    echo "🔄 Testing: $description"
    echo "   Endpoint: $method $endpoint"

    if [ "$method" = "POST" ] && [ -n "$data" ]; then
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
            -X "$method" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE" \
            -d "$data" \
            "$endpoint" 2>/dev/null)
    else
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
            -X "$method" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE" \
            "$endpoint" 2>/dev/null)
    fi

    http_code=$(echo "$response" | grep "HTTP_STATUS" | cut -d: -f2)
    body=$(echo "$response" | sed '/HTTP_STATUS/d')

    echo "   Status: $http_code"
    if [ "$http_code" = "200" ]; then
        echo "✅ SUCCESS"
        echo "   Response: $(echo "$body" | jq -r . 2>/dev/null || echo "$body")"
    else
        echo "❌ FAILED"
        echo "   Response: $body"
    fi
    echo "----------------------------------------"
}

echo "## Step 1: Check SIU Investigators"
make_api_call "$BASE_URL/api/fraud/siu/investigators" "GET" "" "Get all SIU investigators"

echo "## Step 2: Check All SIU Cases"
make_api_call "$BASE_URL/api/fraud/siu/cases" "GET" "" "Get all SIU cases"

echo "## Step 3: Check Investigator-Specific Cases (ID=1)"
make_api_call "$BASE_URL/api/fraud/siu/investigator/1/cases" "GET" "" "Get cases for investigator ID 1"

echo "## Step 4: Debug Investigator Data (ID=1)"
make_api_call "$BASE_URL/api/fraud/siu/debug/investigator/1" "GET" "" "Debug investigator 1 data"

echo "## Step 5: Get Investigator Statistics (ID=1)"
make_api_call "$BASE_URL/api/fraud/siu/investigator/1/statistics" "GET" "" "Get statistics for investigator ID 1"

echo "## Step 6: Get Investigation Cases (ID=1)"
make_api_call "$BASE_URL/api/fraud/siu/investigator/1/assigned-cases" "GET" "" "Get assigned investigation cases for investigator ID 1"

echo
echo "=== Test Complete ==="
echo
echo "📋 Instructions:"
echo "1. Replace 'YOUR_JWT_TOKEN_HERE' with actual JWT token"
echo "2. Get JWT by logging in as SIU investigator via /api/auth/login"
echo "3. Look for discrepancies in the data between endpoints"
echo "4. Check if assigned claims appear in investigator-specific endpoints"
echo
echo "🐛 Common Issues to Look For:"
echo "- Investigator ID mismatch between assignment and retrieval"
echo "- Claims assigned but not appearing in investigator-specific queries"
echo "- Permission errors (403) indicating authentication issues"
echo "- Empty responses indicating data not persisted properly"