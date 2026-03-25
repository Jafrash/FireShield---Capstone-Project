# FireShield Blacklist Detection Fix Guide

## Problem Diagnosis
Your blacklist detection is implemented correctly with case-insensitive matching, but **existing claims need manual recalculation** after adding users to the blacklist.

## Quick Fix Steps

### 1. Start Your Applications
```bash
# Backend
cd FireInsurance-Backend
./mvnw spring-boot:run

# Frontend (separate terminal)
cd FireInsurance-Frontend/FireInsurance-Frontend
ng serve
```

### 2. Verify Blacklist Entry
- Open H2 Console: http://localhost:8080/h2-console
- Login: JDBC URL: `jdbc:h2:file:./data/demo`, Username: `sa`
- Run: `SELECT * FROM BLACKLIST WHERE is_active = true;`

### 3. Recalculate Existing Claims
**Method A: Via Frontend (Easiest)**
1. Login as admin: http://localhost:4200
2. Go to Admin → Claims Management
3. Find claims from blacklisted user
4. Click "View Analysis" (triggers recalculation)
5. Should now show 100% fraud score + CRITICAL risk

**Method B: Via API**
```bash
# Get admin JWT token first, then:
curl -X POST "http://localhost:8080/api/fraud/analysis/{claimId}/recalculate" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Expected Results After Fix
- ✅ Fraud Score: 100%
- ✅ Risk Level: CRITICAL (red badge)
- ✅ BLACKLISTED_ENTITY Rule: TRIGGERED
- ✅ Auto-SIU Assignment
- ✅ New claims automatically detected

## Test Case-Insensitive Matching
Add blacklist entry with different case than user data - should still match!

## Files Modified
- ✅ FraudDetectionService.java - Already uses case-insensitive methods
- ✅ BlacklistRepository.java - Has `existsActiveByTypeAndValueIgnoreCase()`

The system is working correctly - just needs recalculation for existing claims!