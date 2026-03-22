#!/bin/bash

# FireShield Email Test Script
# This script tests the email functionality of the FireShield Insurance System

echo "🔥 FireShield Insurance - Email Test Script"
echo "============================================"

# Wait for backend to start
echo "⏳ Waiting for backend to start..."
sleep 10

# Test email configuration endpoint
echo "📧 Testing email configuration..."
curl -s http://localhost:8080/api/test/email-config | python -m json.tool 2>/dev/null || curl -s http://localhost:8080/api/test/email-config

echo -e "\n"

# Prompt for email address
read -p "Enter your email address to test: " EMAIL_ADDRESS

if [ -z "$EMAIL_ADDRESS" ]; then
    echo "❌ Email address is required!"
    exit 1
fi

echo "📤 Sending test email to: $EMAIL_ADDRESS"

# Send test email
RESPONSE=$(curl -s -X POST http://localhost:8080/api/test/email \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$EMAIL_ADDRESS\"}")

echo "Response:"
echo "$RESPONSE" | python -m json.tool 2>/dev/null || echo "$RESPONSE"

echo -e "\n✅ Email test completed! Check your inbox (including spam folder)."
echo "📊 Check the backend logs for detailed email sending information."