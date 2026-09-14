ALTER TABLE customers ADD COLUMN first_name VARCHAR(255);
ALTER TABLE customers ADD COLUMN last_name VARCHAR(255);

UPDATE customers
SET first_name = CASE
        WHEN POSITION(' ' IN full_name) = 0 THEN full_name
        ELSE SUBSTRING(full_name FROM 1 FOR POSITION(' ' IN full_name) - 1)
    END,
    last_name = CASE
        WHEN POSITION(' ' IN full_name) = 0 THEN ''
        ELSE SUBSTRING(full_name FROM POSITION(' ' IN full_name) + 1)
    END;

CREATE FUNCTION sync_customer_names() RETURNS trigger AS $$
BEGIN
    IF NEW.first_name IS NULL OR NEW.last_name IS NULL
            OR (TG_OP = 'UPDATE' AND NEW.full_name IS DISTINCT FROM OLD.full_name
                AND NEW.first_name IS NOT DISTINCT FROM OLD.first_name
                AND NEW.last_name IS NOT DISTINCT FROM OLD.last_name) THEN
        IF POSITION(' ' IN NEW.full_name) = 0 THEN
            NEW.first_name := NEW.full_name;
            NEW.last_name := '';
        ELSE
            NEW.first_name := SUBSTRING(NEW.full_name FROM 1 FOR POSITION(' ' IN NEW.full_name) - 1);
            NEW.last_name := SUBSTRING(NEW.full_name FROM POSITION(' ' IN NEW.full_name) + 1);
        END IF;
    ELSE
        NEW.full_name := CASE
            WHEN NEW.last_name = '' THEN NEW.first_name
            ELSE NEW.first_name || ' ' || NEW.last_name
        END;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER customers_name_sync
BEFORE INSERT OR UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION sync_customer_names();

ALTER TABLE customers ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE customers ALTER COLUMN last_name SET NOT NULL;
