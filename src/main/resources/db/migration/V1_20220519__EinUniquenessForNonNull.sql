-- Remove the existing UNIQUE constraint and add a new UNIQUE(when not null) constraint

ALTER TABLE business
    DROP CONSTRAINT IF EXISTS business_employer_identification_number_key;

CREATE UNIQUE INDEX business_employer_identification_number
    ON business (employer_identification_number)
    WHERE (employer_identification_number IS NOT NULL);