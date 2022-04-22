UPDATE business_owner
SET relationship_owner = false
WHERE relationship_owner IS NULL;

UPDATE business_owner
SET relationship_representative = false
WHERE relationship_representative IS NULL;

UPDATE business_owner
SET relationship_executive = false
WHERE relationship_executive IS NULL;

UPDATE business_owner
SET relationship_director = false
WHERE relationship_director IS NULL;

ALTER TABLE business_owner
ALTER COLUMN relationship_owner SET DEFAULT false,
ALTER COLUMN relationship_owner SET NOT NULL,
ALTER COLUMN relationship_representative SET DEFAULT false,
ALTER COLUMN relationship_representative SET NOT NULL,
ALTER COLUMN relationship_executive SET DEFAULT false,
ALTER COLUMN relationship_executive SET NOT NULL,
ALTER COLUMN relationship_director SET DEFAULT false,
ALTER COLUMN relationship_director SET NOT NULL;