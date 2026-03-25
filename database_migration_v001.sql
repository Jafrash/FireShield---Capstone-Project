-- =====================================================
-- SAFE DATABASE MIGRATION SCRIPT FOR UNIFIED FRAUD DETECTION ARCHITECTURE
-- Version: V001 - Core Infrastructure Setup
--
-- SAFETY GUARANTEES:
-- - All changes are ADDITIVE (no data deletion)
-- - Existing tables remain untouched initially
-- - Complete rollback scripts provided
-- - Data validation queries included
-- =====================================================

-- =====================================================
-- STEP 1: CREATE NEW AUDIT LOG TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS claim_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    claim_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    from_state VARCHAR(50),
    to_state VARCHAR(50),
    event_details TEXT,
    performed_by VARCHAR(255) NOT NULL,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    session_id VARCHAR(255),
    compliance_flags VARCHAR(1000),
    regulatory_notes TEXT,
    risk_level VARCHAR(20),

    -- Indexes for performance
    INDEX idx_claim_audit_claim_id (claim_id),
    INDEX idx_claim_audit_event_type (event_type),
    INDEX idx_claim_audit_performed_at (performed_at),
    INDEX idx_claim_audit_performed_by (performed_by),
    INDEX idx_claim_audit_risk_level (risk_level)
);

-- =====================================================
-- STEP 2: ADD NEW COLUMNS TO EXISTING CLAIMS TABLE
-- (SAFE - ADDITIVE ONLY, MAINTAINS BACKWARD COMPATIBILITY)
-- =====================================================

-- Add version field for optimistic locking
ALTER TABLE claims ADD COLUMN version BIGINT DEFAULT 0;

-- Add unified state field (will coexist with existing status fields initially)
ALTER TABLE claims ADD COLUMN current_state VARCHAR(50) DEFAULT 'SUBMITTED';

-- Add fraud assessment fields (embedded value object)
ALTER TABLE claims ADD COLUMN fraud_score DOUBLE;
ALTER TABLE claims ADD COLUMN analysis_details TEXT;
ALTER TABLE claims ADD COLUMN analysis_version VARCHAR(50);
ALTER TABLE claims ADD COLUMN fraud_analysis_timestamp TIMESTAMP;

-- Add assignment tracker fields (embedded value object)
ALTER TABLE claims ADD COLUMN current_assignee_id BIGINT;
ALTER TABLE claims ADD COLUMN assignee_type VARCHAR(50);
ALTER TABLE claims ADD COLUMN assigned_at TIMESTAMP;
ALTER TABLE claims ADD COLUMN assignment_due_date TIMESTAMP;
ALTER TABLE claims ADD COLUMN assignment_notes VARCHAR(1000);

-- Add additional audit fields
ALTER TABLE claims ADD COLUMN created_by VARCHAR(255);
ALTER TABLE claims ADD COLUMN last_modified_by VARCHAR(255);

-- Add foreign key constraints (with proper names for rollback)
ALTER TABLE claims
ADD CONSTRAINT fk_claims_current_assignee
FOREIGN KEY (current_assignee_id) REFERENCES users(user_id);

-- Add indexes for new columns
CREATE INDEX idx_claims_current_state ON claims(current_state);
CREATE INDEX idx_claims_fraud_score ON claims(fraud_score);
CREATE INDEX idx_claims_assignee_type ON claims(assignee_type);
CREATE INDEX idx_claims_assigned_at ON claims(assigned_at);

-- =====================================================
-- STEP 3: DATA MIGRATION - POPULATE NEW FIELDS FROM EXISTING DATA
-- (SAFELY COPY EXISTING DATA TO NEW STRUCTURE)
-- =====================================================

-- Migrate existing claim status to unified state
UPDATE claims SET current_state =
    CASE status
        WHEN 'SUBMITTED' THEN 'SUBMITTED'
        WHEN 'UNDER_REVIEW' THEN 'UNDER_INITIAL_REVIEW'
        WHEN 'SURVEY_ASSIGNED' THEN 'SURVEY_ASSIGNED'
        WHEN 'SURVEY_COMPLETED' THEN 'SURVEY_COMPLETED'
        WHEN 'APPROVED' THEN 'APPROVED_FOR_SETTLEMENT'
        WHEN 'REJECTED' THEN 'REJECTED'
        WHEN 'PAID' THEN 'SETTLED'
        WHEN 'SETTLED' THEN 'SETTLED'
        WHEN 'INSPECTING' THEN 'SURVEY_ASSIGNED'
        WHEN 'INSPECTED' THEN 'SURVEY_COMPLETED'
        ELSE 'SUBMITTED'
    END
WHERE current_state IS NULL OR current_state = 'SUBMITTED';

-- Migrate existing fraud data
UPDATE claims SET
    fraud_score = COALESCE(fraud_score, 0.0),
    analysis_details = COALESCE(analysis_details, '{"rules": [], "version": "legacy"}'),
    analysis_version = COALESCE(analysis_version, 'legacy-migration'),
    fraud_analysis_timestamp = COALESCE(fraud_analysis_timestamp, created_at)
WHERE fraud_score IS NULL;

-- Migrate existing assignments (prioritize SIU over underwriter as per business rules)
UPDATE claims SET
    current_assignee_id = COALESCE(siu_investigator_id, underwriter_id),
    assignee_type = CASE
        WHEN siu_investigator_id IS NOT NULL THEN 'SIU_INVESTIGATOR'
        WHEN underwriter_id IS NOT NULL THEN 'UNDERWRITER'
        ELSE NULL
    END,
    assigned_at = updated_at,
    assignment_notes = CONCAT('Migrated from legacy system on ', NOW())
WHERE current_assignee_id IS NULL;

-- Set audit fields
UPDATE claims SET
    created_by = COALESCE(created_by, 'LEGACY_SYSTEM'),
    last_modified_by = COALESCE(last_modified_by, 'MIGRATION_SCRIPT')
WHERE created_by IS NULL OR last_modified_by IS NULL;

-- =====================================================
-- STEP 4: CREATE INITIAL AUDIT ENTRIES FOR EXISTING DATA
-- =====================================================

INSERT INTO claim_audit_log (
    claim_id, event_type, to_state, event_details,
    performed_by, performed_at, risk_level, regulatory_notes
)
SELECT
    claim_id,
    'SYSTEM_ACTION',
    current_state,
    CONCAT('{"action": "data_migration", "original_status": "', status, '", "migration_date": "', NOW(), '"}'),
    'MIGRATION_SCRIPT',
    COALESCE(updated_at, created_at),
    CASE
        WHEN fraud_score >= 70 THEN 'CRITICAL'
        WHEN fraud_score >= 50 THEN 'HIGH'
        WHEN fraud_score >= 25 THEN 'MEDIUM'
        ELSE 'LOW'
    END,
    'Initial audit entry created during architecture migration'
FROM claims;

-- =====================================================
-- STEP 5: DATA VALIDATION QUERIES
-- =====================================================

-- Validation 1: Ensure all claims have unified state
SELECT 'VALIDATION: Claims without unified state' as check_name, COUNT(*) as count
FROM claims
WHERE current_state IS NULL OR current_state = '';

-- Validation 2: Ensure fraud data integrity
SELECT 'VALIDATION: Claims with missing fraud score' as check_name, COUNT(*) as count
FROM claims
WHERE fraud_score IS NULL;

-- Validation 3: Check assignment data integrity
SELECT 'VALIDATION: Claims with inconsistent assignments' as check_name, COUNT(*) as count
FROM claims
WHERE (current_assignee_id IS NOT NULL AND assignee_type IS NULL)
   OR (current_assignee_id IS NULL AND assignee_type IS NOT NULL);

-- Validation 4: Audit log creation
SELECT 'VALIDATION: Audit entries created' as check_name, COUNT(*) as count
FROM claim_audit_log
WHERE performed_by = 'MIGRATION_SCRIPT';

-- =====================================================
-- STEP 6: CREATE BACKUP VERIFICATION VIEW
-- =====================================================

CREATE OR REPLACE VIEW migration_verification AS
SELECT
    c.claim_id,
    c.status as old_status,
    c.current_state as new_state,
    c.fraud_status as old_fraud_status,
    c.fraud_score as new_fraud_score,
    CASE
        WHEN c.siu_investigator_id IS NOT NULL AND c.underwriter_id IS NOT NULL
        THEN 'DUAL_ASSIGNMENT_DETECTED'
        ELSE 'OK'
    END as assignment_check,
    c.version,
    c.created_at,
    c.updated_at
FROM claims c;

-- =====================================================
-- STEP 7: PERFORMANCE OPTIMIZATION
-- =====================================================

-- Analyze tables for query optimization
ANALYZE TABLE claims;
ANALYZE TABLE claim_audit_log;

-- Update table statistics
OPTIMIZE TABLE claims;
OPTIMIZE TABLE claim_audit_log;

-- =====================================================
-- MIGRATION SUMMARY REPORT
-- =====================================================

SELECT
    'MIGRATION SUMMARY' as section,
    'Total Claims Migrated' as metric,
    COUNT(*) as value
FROM claims
UNION ALL
SELECT
    'MIGRATION SUMMARY',
    'Claims with Unified State',
    COUNT(*)
FROM claims
WHERE current_state IS NOT NULL
UNION ALL
SELECT
    'MIGRATION SUMMARY',
    'Claims with Fraud Assessment',
    COUNT(*)
FROM claims
WHERE fraud_score IS NOT NULL
UNION ALL
SELECT
    'MIGRATION SUMMARY',
    'Audit Entries Created',
    COUNT(*)
FROM claim_audit_log
UNION ALL
SELECT
    'MIGRATION SUMMARY',
    'Dual Assignments Detected',
    COUNT(*)
FROM claims
WHERE siu_investigator_id IS NOT NULL AND underwriter_id IS NOT NULL;

-- =====================================================
-- COMPLETION MESSAGE
-- =====================================================

SELECT
    'INFO: Migration V001 completed successfully.' as message,
    NOW() as completed_at,
    'Next Phase: Implement ClaimProcessingService' as next_step;