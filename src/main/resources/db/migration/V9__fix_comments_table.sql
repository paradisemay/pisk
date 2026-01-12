DO $$
DECLARE
    v_schema_name TEXT;
BEGIN
    SELECT current_schema() INTO v_schema_name;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = v_schema_name
                   AND table_name = 'comments' 
                   AND column_name = 'id') THEN
        CREATE SEQUENCE IF NOT EXISTS comments_id_seq;
        
        ALTER TABLE comments
            ADD COLUMN id BIGINT;
        
        UPDATE comments
        SET id = nextval('comments_id_seq')
        WHERE id IS NULL;
        
        PERFORM setval('comments_id_seq', COALESCE((SELECT MAX(id) FROM comments), 0) + 1, false);
        
        ALTER TABLE comments
            ALTER COLUMN id SET DEFAULT nextval('comments_id_seq'),
            ALTER COLUMN id SET NOT NULL;
        
        ALTER SEQUENCE comments_id_seq OWNED BY comments.id;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = v_schema_name
               AND table_name = 'comments' 
               AND column_name = 'id')
       AND EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE table_schema = v_schema_name
                   AND table_name = 'comments' 
                   AND constraint_type = 'PRIMARY KEY'
                   AND constraint_name = 'comments_pkey') THEN
        ALTER TABLE comments DROP CONSTRAINT comments_pkey;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = v_schema_name
               AND table_name = 'comments' 
               AND column_name = 'id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                       WHERE table_schema = v_schema_name
                       AND table_name = 'comments' 
                       AND constraint_type = 'PRIMARY KEY') THEN
        ALTER TABLE comments ADD PRIMARY KEY (id);
    END IF;
    
    ALTER TABLE comments
        ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS complaint_count INTEGER NOT NULL DEFAULT 0;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = v_schema_name
               AND table_name = 'comments' 
               AND column_name = 'track_time') THEN
        ALTER TABLE comments
            ALTER COLUMN track_time DROP NOT NULL;
    END IF;
END $$;
