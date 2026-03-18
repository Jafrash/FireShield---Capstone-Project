-- ==========================================
-- POLICY RENEWAL & NCB IMPLEMENTATION
-- Database Migration Script
-- Date: March 11, 2026
-- ==========================================

-- Add Renewal Workflow Fields
-- ==========================================

-- Renewal eligibility flag (marks policies within 30 days of expiry)
ALTER TABLE policy_subscriptions 
ADD COLUMN IF NOT EXISTS renewal_eligible BOOLEAN DEFAULT FALSE;

-- Link to previous subscription for renewal chain tracking
ALTER TABLE policy_subscriptions 
ADD COLUMN IF NOT EXISTS previous_subscription_id BIGINT;

-- Count of how many times this policy has been renewed
ALTER TABLE policy_subscriptions 
ADD COLUMN IF NOT EXISTS renewal_count INTEGER DEFAULT 0;

-- Flag to track if renewal reminder was sent to customer
ALTER TABLE policy_subscriptions 
ADD COLUMN IF NOT EXISTS renewal_reminder_sent BOOLEAN DEFAULT FALSE;

-- Add No Claim Bonus (NCB) Fields
-- ==========================================

-- Number of consecutive claim-free years
ALTER TABLE policy_subscriptions 
ADD COLUMN IF NOT EXISTS claim_free_years INTEGER DEFAULT 0;

-- NCB discount percentage (0.10 = 10%, 0.50 = 50% max)
ALTER TABLE policy_subscriptions 
ADD COLUMN IF NOT EXISTS ncb_discount DOUBLE PRECISION DEFAULT 0.0;

-- Timestamp of last approved claim (for NCB reset tracking)
ALTER TABLE policy_subscriptions 
ADD COLUMN IF NOT EXISTS last_claim_date TIMESTAMP;

-- Update Existing Records
-- ==========================================

-- Set default values for all existing records
UPDATE policy_subscriptions 
SET 
    renewal_eligible = FALSE,
    renewal_count = 0,
    renewal_reminder_sent = FALSE,
    claim_free_years = 0,
    ncb_discount = 0.0,
    last_claim_date = NULL
WHERE renewal_eligible IS NULL;

-- Add Foreign Key Constraint (Optional but Recommended)
-- ==========================================

-- This creates a self-referencing foreign key for renewal chain
ALTER TABLE policy_subscriptions 
ADD CONSTRAINT fk_previous_subscription 
FOREIGN KEY (previous_subscription_id) 
REFERENCES policy_subscriptions(subscription_id) 
ON DELETE SET NULL;

-- Create Indexes for Performance
-- ==========================================

-- Index for finding renewal-eligible subscriptions
CREATE INDEX IF NOT EXISTS idx_renewal_eligible 
ON policy_subscriptions(renewal_eligible) 
WHERE renewal_eligible = TRUE;

-- Index for finding subscriptions by status (used in updateRenewalEligibility)
CREATE INDEX IF NOT EXISTS idx_subscription_status 
ON policy_subscriptions(status);

-- Index for NCB queries
CREATE INDEX IF NOT EXISTS idx_claim_free_years 
ON policy_subscriptions(claim_free_years);

-- Index for renewal chains
CREATE INDEX IF NOT EXISTS idx_previous_subscription 
ON policy_subscriptions(previous_subscription_id);

-- Verification Queries
-- ==========================================

-- Check if columns were added successfully
SELECT 
    column_name, 
    data_type, 
    column_default, 
    is_nullable 
FROM information_schema.columns 
WHERE table_name = 'policy_subscriptions' 
AND column_name IN (
    'renewal_eligible',
    'previous_subscription_id', 
    'renewal_count',
    'renewal_reminder_sent',
    'claim_free_years',
    'ncb_discount',
    'last_claim_date'
)
ORDER BY column_name;

-- Count existing subscriptions that were updated
SELECT 
    COUNT(*) as total_subscriptions,
    SUM(CASE WHEN renewal_eligible = FALSE THEN 1 ELSE 0 END) as not_eligible_for_renewal,
    SUM(CASE WHEN renewal_count = 0 THEN 1 ELSE 0 END) as never_renewed,
    SUM(CASE WHEN claim_free_years = 0 THEN 1 ELSE 0 END) as no_ncb_benefit
FROM policy_subscriptions;

-- ==========================================
-- ROLLBACK SCRIPT (Use only if needed)
-- ==========================================

/*
-- WARNING: This will delete all renewal and NCB data!
-- Only use for testing or development environments

ALTER TABLE policy_subscriptions DROP CONSTRAINT IF EXISTS fk_previous_subscription;
DROP INDEX IF EXISTS idx_renewal_eligible;
DROP INDEX IF EXISTS idx_subscription_status;
DROP INDEX IF EXISTS idx_claim_free_years;
DROP INDEX IF EXISTS idx_previous_subscription;

ALTER TABLE policy_subscriptions DROP COLUMN IF EXISTS renewal_eligible;
ALTER TABLE policy_subscriptions DROP COLUMN IF EXISTS previous_subscription_id;
ALTER TABLE policy_subscriptions DROP COLUMN IF EXISTS renewal_count;
ALTER TABLE policy_subscriptions DROP COLUMN IF EXISTS renewal_reminder_sent;
ALTER TABLE policy_subscriptions DROP COLUMN IF EXISTS claim_free_years;
ALTER TABLE policy_subscriptions DROP COLUMN IF EXISTS ncb_discount;
ALTER TABLE policy_subscriptions DROP COLUMN IF EXISTS last_claim_date;
*/

-- ==========================================
-- END OF MIGRATION SCRIPT
-- ==========================================

-- Next Steps:
-- 1. Backup your database before running this script
-- 2. Run this script in your PostgreSQL database
-- 3. Verify columns were added using the verification queries above
-- 4. Restart your Spring Boot application
-- 5. Test renewal endpoints: /api/subscriptions/{id}/renew
-- 6. Test NCB calculator: /api/subscriptions/ncb-calculator/{years}
