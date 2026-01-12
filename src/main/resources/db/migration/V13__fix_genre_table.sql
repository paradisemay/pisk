DO $$
BEGIN
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
        
        ALTER TABLE genre RENAME COLUMN title TO name;
        
        ALTER TABLE genre ALTER COLUMN name TYPE VARCHAR(64);
    END IF;
    
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = current_schema() 
        AND table_name = 'genre' 
        AND column_name = 'name'
    ) THEN
        ALTER TABLE genre
            ALTER COLUMN name TYPE VARCHAR(64),
            ALTER COLUMN name SET NOT NULL;
        
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
