-- Fix genre table to match GenreEntity
DO $$
BEGIN
    -- Rename title to name if title exists and name doesn't
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = current_schema() 
        AND table_name = 'genre' 
        AND column_name = 'title'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = current_schema() 
        AND table_name = 'genre' 
        AND column_name = 'name'
    ) THEN
        -- Drop the unique constraint on title first
        IF EXISTS (
            SELECT 1 FROM information_schema.table_constraints 
            WHERE table_schema = current_schema() 
            AND table_name = 'genre' 
            AND constraint_type = 'UNIQUE'
            AND constraint_name LIKE '%title%'
        ) THEN
            ALTER TABLE genre DROP CONSTRAINT IF EXISTS genre_title_key;
            ALTER TABLE genre DROP CONSTRAINT IF EXISTS genre_title_key1;
        END IF;
        
        -- Rename title to name
        ALTER TABLE genre RENAME COLUMN title TO name;
        
        -- Change column type to match entity (VARCHAR(64))
        ALTER TABLE genre ALTER COLUMN name TYPE VARCHAR(64);
    END IF;
    
    -- If name column exists but is not the right type, fix it
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = current_schema() 
        AND table_name = 'genre' 
        AND column_name = 'name'
    ) THEN
        -- Ensure name is VARCHAR(64) and NOT NULL
        ALTER TABLE genre
            ALTER COLUMN name TYPE VARCHAR(64),
            ALTER COLUMN name SET NOT NULL;
        
        -- Add unique constraint on name if it doesn't exist
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints 
            WHERE table_schema = current_schema() 
            AND table_name = 'genre' 
            AND constraint_type = 'UNIQUE'
            AND (constraint_name = 'genre_name_key' OR constraint_name LIKE '%name%')
        ) THEN
            ALTER TABLE genre ADD CONSTRAINT genre_name_key UNIQUE (name);
        END IF;
    END IF;
END $$;
