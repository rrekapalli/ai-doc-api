-- Migration: Switch usage_tracking FK from subscribers.user_id to subscribers.email
-- 1) Add email column
ALTER TABLE usage_tracking ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- 2) Drop old FK on user_id (if exists)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_usage_user'
          AND table_name = 'usage_tracking'
    ) THEN
        ALTER TABLE usage_tracking DROP CONSTRAINT fk_usage_user;
    END IF;
END $$;

-- 3) Make user_id nullable to avoid insert failures when not provided
ALTER TABLE usage_tracking ALTER COLUMN user_id DROP NOT NULL;

-- 4) Add new FK on email to subscribers(email) (if not already)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_usage_email'
          AND table_name = 'usage_tracking'
    ) THEN
        ALTER TABLE usage_tracking
            ADD CONSTRAINT fk_usage_email FOREIGN KEY (email) REFERENCES subscribers(email);
    END IF;
END $$;

-- 5) Backfill email values from user_id where possible (when user_id looks like an email and exists in subscribers)
UPDATE usage_tracking ut
SET email = ut.user_id
FROM subscribers s
WHERE ut.email IS NULL
  AND ut.user_id IS NOT NULL
  AND position('@' in ut.user_id) > 1
  AND s.email = ut.user_id;

-- 6) Index to speed up lookups by (email, month_year)
CREATE INDEX IF NOT EXISTS idx_usage_email_month ON usage_tracking(email, month_year);
