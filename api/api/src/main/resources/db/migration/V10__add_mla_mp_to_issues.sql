-- Add MLA and MP information columns to issues table

ALTER TABLE issues ADD COLUMN IF NOT EXISTS mla_id BIGINT;
ALTER TABLE issues ADD COLUMN IF NOT EXISTS mla_name VARCHAR(255);
ALTER TABLE issues ADD COLUMN IF NOT EXISTS mla_party VARCHAR(100);
ALTER TABLE issues ADD COLUMN IF NOT EXISTS mp_id BIGINT;
ALTER TABLE issues ADD COLUMN IF NOT EXISTS mp_name VARCHAR(255);
ALTER TABLE issues ADD COLUMN IF NOT EXISTS mp_party VARCHAR(100);

-- Add indexes for MLA/MP lookups
CREATE INDEX IF NOT EXISTS idx_issue_mla ON issues(mla_id);
CREATE INDEX IF NOT EXISTS idx_issue_mp ON issues(mp_id);
