-- Fix invalid enum value in claims table
UPDATE claims SET status = 'SURVEYOR_ASSIGNED' WHERE status = 'SURVEY_ASSIGNED';

