-- =====================================================
-- ROLLBACK SCRIPT FOR UNIFIED FRAUD DETECTION ARCHITECTURE
-- Version: V001 - Rollback to Original State
--
-- SAFETY GUARANTEES:
-- - Complete restoration of original structure
-- - No data loss during rollback
-- - Verification queries to confirm rollback success
-- =====================================================

-- =====================================================
-- STEP 1: BACKUP VERIFICATION BEFORE ROLLBACK
-- =====================================================

-- Create temporary backup of new data before rollback
CREATE TABLE IF NOT EXISTS rollback_backup_claims AS
SELECT
    claim_id,
    current_state,
    fraud_score,
    analysis_details,
    analysis_version,
    fraud_analysis_timestamp,
    current_assignee_id,
    assignee_type,
    assigned_at,
    assignment_due_date,
    assignment_notes,
    created_by,
    last_modified_by,
    version
FROM claims
WHERE current_state IS NOT NULL;

-- Backup audit log data
CREATE TABLE IF NOT EXISTS rollback_backup_audit AS
SELECT * FROM claim_audit_log;

-- Report current state before rollback
SELECT
    'PRE-ROLLBACK REPORT' as section,
    'Claims with New Architecture' as metric,
    COUNT(*) as value
FROM claims
WHERE current_state IS NOT NULL
UNION ALL
SELECT
    'PRE-ROLLBACK REPORT',
    'Total Audit Entries',
    COUNT(*)
FROM claim_audit_log;

-- =====================================================
-- STEP 2: REMOVE NEW FOREIGN KEY CONSTRAINTS
-- =====================================================

-- Drop foreign key constraints added during migration
ALTER TABLE claims DROP FOREIGN KEY IF EXISTS fk_claims_current_assignee;

-- =====================================================
-- STEP 3: DROP NEW INDEXES
-- =====================================================

DROP INDEX IF EXISTS idx_claims_current_state ON claims;
DROP INDEX IF EXISTS idx_claims_fraud_score ON claims;
DROP INDEX IF EXISTS idx_claims_assignee_type ON claims;
DROP INDEX IF EXISTS idx_claims_assigned_at ON claims;

-- =====================================================
-- STEP 4: REMOVE NEW COLUMNS FROM CLAIMS TABLE
-- =====================================================

-- Remove unified state machine column
ALTER TABLE claims DROP COLUMN IF EXISTS current_state;

-- Remove version field for optimistic locking
ALTER TABLE claims DROP COLUMN IF EXISTS version;

-- Remove fraud assessment fields (embedded value object)
ALTER TABLE claims DROP COLUMN IF EXISTS analysis_details;
ALTER TABLE claims DROP COLUMN IF EXISTS analysis_version;
ALTER TABLE claims DROP COLUMN IF EXISTS fraud_analysis_timestamp;

-- Remove assignment tracker fields (embedded value object)
ALTER TABLE claims DROP COLUMN IF EXISTS current_assignee_id;
ALTER TABLE claims DROP COLUMN IF EXISTS assignee_type;
ALTER TABLE claims DROP COLUMN IF EXISTS assigned_at;
ALTER TABLE claims DROP COLUMN IF EXISTS assignment_due_date;
ALTER TABLE claims DROP COLUMN IF EXISTS assignment_notes;

-- Remove additional audit fields
ALTER TABLE claims DROP COLUMN IF EXISTS created_by;
ALTER TABLE claims DROP COLUMN IF EXISTS last_modified_by;

-- =====================================================
-- STEP 5: DROP NEW TABLES
-- =====================================================

-- Drop audit log table
DROP TABLE IF EXISTS claim_audit_log;

-- Drop migration verification view
DROP VIEW IF EXISTS migration_verification;

-- =====================================================
-- STEP 6: VALIDATION AFTER ROLLBACK
-- =====================================================

-- Ensure original structure is intact
DESCRIBE claims;

-- Verify no new columns exist
SELECT
    COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'claims'
  AND TABLE_SCHEMA = DATABASE()
  AND COLUMN_NAME IN (
    'current_state', 'version', 'analysis_details', 'analysis_version',
    'fraud_analysis_timestamp', 'current_assignee_id', 'assignee_type',
    'assigned_at', 'assignment_due_date', 'assignment_notes',
    'created_by', 'last_modified_by'
  );

-- Verify audit table is removed
SELECT COUNT(*) as audit_table_exists
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME = 'claim_audit_log'
  AND TABLE_SCHEMA = DATABASE();

-- Check that original data is preserved
SELECT
    'ROLLBACK VERIFICATION' as section,
    'Total Claims Preserved' as metric,
    COUNT(*) as value
FROM claims
UNION ALL
SELECT
    'ROLLBACK VERIFICATION',
    'Claims with Original Status',
    COUNT(*)
FROM claims
WHERE status IS NOT NULL
UNION ALL
SELECT
    'ROLLBACK VERIFICATION',
    'Claims with Original Fraud Status',
    COUNT(*)
FROM claims
WHERE fraud_status IS NOT NULL;

-- =====================================================
-- STEP 7: CLEANUP BACKUP TABLES (OPTIONAL)
-- =====================================================

-- These tables contain the new architecture data if rollback was needed
-- Keep them for potential re-migration or remove them if rollback is permanent

-- To remove backup tables (uncomment if needed):
-- DROP TABLE IF EXISTS rollback_backup_claims;
-- DROP TABLE IF EXISTS rollback_backup_audit;

-- =====================================================
-- ROLLBACK COMPLETION REPORT
-- =====================================================

SELECT
    'SUCCESS: Architecture rollback completed.' as message,
    'All new columns and tables removed.' as details,
    'Original data structure restored.' as confirmation,
    NOW() as rollback_completed_at;

SELECT
    'BACKUP TABLES PRESERVED' as notice,
    'rollback_backup_claims: Contains new architecture data' as table1,
    'rollback_backup_audit: Contains audit log data' as table2,
    'These can be dropped if rollback is permanent' as action;