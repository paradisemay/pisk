-- Fix comments table structure to match CommentEntity
DO $$
DECLARE
    v_schema_name TEXT;
BEGIN
    -- Get current schema name
    SELECT current_schema() INTO v_schema_name;
    
    -- Add id column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = v_schema_name
                   AND table_name = 'comments' 
                   AND column_name = 'id') THEN
        -- Create sequence for id
        CREATE SEQUENCE IF NOT EXISTS comments_id_seq;
        
        -- Add id column
        ALTER TABLE comments
            ADD COLUMN id BIGINT;
        
        -- Populate id with sequential values for existing rows
        UPDATE comments
        SET id = nextval('comments_id_seq')
        WHERE id IS NULL;
        
        -- Set sequence to start from max(id) + 1
        PERFORM setval('comments_id_seq', COALESCE((SELECT MAX(id) FROM comments), 0) + 1, false);
        
        -- Make id use the sequence
        ALTER TABLE comments
            ALTER COLUMN id SET DEFAULT nextval('comments_id_seq'),
            ALTER COLUMN id SET NOT NULL;
        
        -- Set sequence ownership
        ALTER SEQUENCE comments_id_seq OWNED BY comments.id;
    END IF;
    
    -- Drop the old composite primary key if it exists (but only if id column exists)
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
    
    -- Add primary key on id if it doesn't exist
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
    
    -- Add missing columns
    ALTER TABLE comments
        ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS complaint_count INTEGER NOT NULL DEFAULT 0;
    
    -- Make track_time nullable since it's not in the entity
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = v_schema_name
               AND table_name = 'comments' 
               AND column_name = 'track_time') THEN
        ALTER TABLE comments
            ALTER COLUMN track_time DROP NOT NULL;
    END IF;
END $$;
