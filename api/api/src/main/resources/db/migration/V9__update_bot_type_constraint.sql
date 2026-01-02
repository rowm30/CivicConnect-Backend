-- Update bot_type check constraint to include new types: MLA_SYNC and MLA_MYNETA

-- First, drop the existing constraint
ALTER TABLE bots DROP CONSTRAINT IF EXISTS bots_bot_type_check;

-- Recreate with all bot types
ALTER TABLE bots ADD CONSTRAINT bots_bot_type_check
CHECK (bot_type IN ('MLA_SCRAPER', 'MLA_SYNC', 'MLA_MYNETA', 'MP_SCRAPER', 'ELECTION_RESULT', 'CONSTITUENCY_DATA', 'BENEFIT_SCRAPER', 'NEWS_AGGREGATOR'));
