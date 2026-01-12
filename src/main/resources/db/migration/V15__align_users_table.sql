BEGIN;

ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash_tmp VARCHAR(255);
UPDATE users SET password_hash_tmp = CASE
    WHEN pg_typeof(password_hash) = 'bytea'::regtype THEN encode(password_hash, 'hex')
    ELSE password_hash::text
END
WHERE password_hash_tmp IS NULL;
ALTER TABLE users DROP COLUMN password_hash;
ALTER TABLE users RENAME COLUMN password_hash_tmp TO password_hash;

ALTER TABLE users ADD COLUMN IF NOT EXISTS password_salt_tmp VARCHAR(64);
UPDATE users SET password_salt_tmp = CASE
    WHEN pg_typeof(password_salt) = 'bytea'::regtype THEN encode(password_salt, 'hex')
    ELSE password_salt::text
END
WHERE password_salt_tmp IS NULL;
ALTER TABLE users DROP COLUMN password_salt;
ALTER TABLE users RENAME COLUMN password_salt_tmp TO password_salt;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email VARCHAR(128),
    ADD COLUMN IF NOT EXISTS status VARCHAR(16) DEFAULT 'PENDING' NOT NULL,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT now() NOT NULL,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT now() NOT NULL,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;

UPDATE users SET email = CONCAT(username, '@example.com') WHERE email IS NULL;
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'registration_date'
    ) THEN
        EXECUTE 'UPDATE users SET created_at = COALESCE(created_at, registration_date, now())';
    ELSE
        EXECUTE 'UPDATE users SET created_at = COALESCE(created_at, now())';
    END IF;
END $$;
UPDATE users SET updated_at = COALESCE(updated_at, created_at);

ALTER TABLE users ADD COLUMN IF NOT EXISTS role_tmp VARCHAR(16) DEFAULT 'USER' NOT NULL;
UPDATE users SET role_tmp = CASE
    WHEN role::text = '1' THEN 'ADMIN'
    WHEN role::text = '2' THEN 'ARTIST'
    WHEN role::text = '3' THEN 'MODERATOR'
    ELSE 'USER'
END;
ALTER TABLE users DROP COLUMN role;
ALTER TABLE users RENAME COLUMN role_tmp TO role;

ALTER TABLE users ALTER COLUMN email SET NOT NULL;
ALTER TABLE users ALTER COLUMN username TYPE VARCHAR(64);
ALTER TABLE users ALTER COLUMN password_salt TYPE VARCHAR(64);
ALTER TABLE users ALTER COLUMN password_hash TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'USER';
ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);

COMMIT;
