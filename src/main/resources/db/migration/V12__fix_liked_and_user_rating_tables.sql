DO $$
DECLARE
    v_schema_name TEXT;
BEGIN
    SELECT current_schema() INTO v_schema_name;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = v_schema_name
                   AND table_name = 'liked' 
                   AND column_name = 'id') THEN
        CREATE SEQUENCE IF NOT EXISTS liked_id_seq;
        
        ALTER TABLE liked
            ADD COLUMN id BIGINT;
        
        UPDATE liked
        SET id = nextval('liked_id_seq')
        WHERE id IS NULL;
        
        PERFORM setval('liked_id_seq', COALESCE((SELECT MAX(id) FROM liked), 0) + 1, false);
        
        ALTER TABLE liked
            ALTER COLUMN id SET DEFAULT nextval('liked_id_seq'),
            ALTER COLUMN id SET NOT NULL;
        
        ALTER SEQUENCE liked_id_seq OWNED BY liked.id;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE table_schema = v_schema_name
               AND table_name = 'liked' 
               AND constraint_type = 'PRIMARY KEY'
               AND constraint_name = 'liked_pkey') THEN
        ALTER TABLE liked DROP CONSTRAINT liked_pkey;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = v_schema_name
               AND table_name = 'liked' 
               AND column_name = 'id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                       WHERE table_schema = v_schema_name
                       AND table_name = 'liked' 
                       AND constraint_type = 'PRIMARY KEY') THEN
        ALTER TABLE liked ADD PRIMARY KEY (id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE table_schema = v_schema_name
                   AND table_name = 'liked' 
                   AND constraint_type = 'UNIQUE'
                   AND constraint_name = 'uk_liked_track_user') THEN
        ALTER TABLE liked ADD CONSTRAINT uk_liked_track_user UNIQUE (track_id, user_id);
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = v_schema_name
               AND table_name = 'liked' 
               AND column_name = 'created_at') THEN
        ALTER TABLE liked
            ALTER COLUMN created_at SET NOT NULL,
            ALTER COLUMN created_at SET DEFAULT now();
    END IF;
END $$;

DO $$
DECLARE
    v_schema_name TEXT;
BEGIN
    SELECT current_schema() INTO v_schema_name;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = v_schema_name
                   AND table_name = 'user_rating' 
                   AND column_name = 'id') THEN
        CREATE SEQUENCE IF NOT EXISTS user_rating_id_seq;
        
        ALTER TABLE user_rating
            ADD COLUMN id BIGINT;
        
        UPDATE user_rating
        SET id = nextval('user_rating_id_seq')
        WHERE id IS NULL;
        
        PERFORM setval('user_rating_id_seq', COALESCE((SELECT MAX(id) FROM user_rating), 0) + 1, false);
        
        ALTER TABLE user_rating
            ALTER COLUMN id SET DEFAULT nextval('user_rating_id_seq'),
            ALTER COLUMN id SET NOT NULL;
        
        ALTER SEQUENCE user_rating_id_seq OWNED BY user_rating.id;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE table_schema = v_schema_name
               AND table_name = 'user_rating' 
               AND constraint_type = 'PRIMARY KEY'
               AND constraint_name = 'user_rating_pkey') THEN
        ALTER TABLE user_rating DROP CONSTRAINT user_rating_pkey;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = v_schema_name
               AND table_name = 'user_rating' 
               AND column_name = 'id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                       WHERE table_schema = v_schema_name
                       AND table_name = 'user_rating' 
                       AND constraint_type = 'PRIMARY KEY') THEN
        ALTER TABLE user_rating ADD PRIMARY KEY (id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE table_schema = v_schema_name
                   AND table_name = 'user_rating' 
                   AND constraint_type = 'UNIQUE'
                   AND constraint_name = 'uk_user_rating_track_user') THEN
        ALTER TABLE user_rating ADD CONSTRAINT uk_user_rating_track_user UNIQUE (track_id, user_id);
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = v_schema_name
               AND table_name = 'user_rating' 
               AND column_name = 'rate')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                       WHERE table_schema = v_schema_name
                       AND table_name = 'user_rating' 
                       AND column_name = 'value') THEN
        ALTER TABLE user_rating RENAME COLUMN rate TO value;
    END IF;
    
    ALTER TABLE user_rating
        ADD COLUMN IF NOT EXISTS comment VARCHAR(1024),
        ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT now();
END $$;
