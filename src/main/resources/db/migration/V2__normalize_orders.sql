CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(50),
    CONSTRAINT uq_customer_contact UNIQUE NULLS NOT DISTINCT (full_name, address, phone)
);

INSERT INTO customers (full_name, address, phone)
SELECT DISTINCT customer_full_name, customer_address, customer_phone
FROM orders;

ALTER TABLE orders ADD COLUMN customer_id BIGINT;

UPDATE orders AS o
SET customer_id = c.id
FROM customers AS c
WHERE o.customer_full_name = c.full_name
  AND o.customer_address IS NOT DISTINCT FROM c.address
  AND o.customer_phone IS NOT DISTINCT FROM c.phone;

ALTER TABLE orders ALTER COLUMN customer_id SET NOT NULL;
ALTER TABLE orders ADD CONSTRAINT fk_orders_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);

INSERT INTO products (name, price)
SELECT DISTINCT oi.product_name, oi.product_price
FROM order_items AS oi
WHERE NOT EXISTS (
    SELECT 1 FROM products AS p
    WHERE p.name = oi.product_name AND p.price = oi.product_price
);

ALTER TABLE order_items ADD COLUMN product_id BIGINT;

UPDATE order_items AS oi
SET product_id = (
    SELECT MIN(p.id) FROM products AS p
    WHERE p.name = oi.product_name AND p.price = oi.product_price
);

ALTER TABLE order_items ALTER COLUMN product_id SET NOT NULL;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_product
    FOREIGN KEY (product_id) REFERENCES products(id);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

CREATE FUNCTION sync_order_customer() RETURNS trigger AS $$
DECLARE
    contact customers%ROWTYPE;
BEGIN
    IF NEW.customer_id IS NULL THEN
        INSERT INTO customers (full_name, address, phone)
        VALUES (NEW.customer_full_name, NEW.customer_address, NEW.customer_phone)
        ON CONFLICT ON CONSTRAINT uq_customer_contact
        DO UPDATE SET full_name = EXCLUDED.full_name
        RETURNING * INTO contact;
        NEW.customer_id := contact.id;
    ELSE
        SELECT * INTO contact FROM customers WHERE id = NEW.customer_id;
        NEW.customer_full_name := contact.full_name;
        NEW.customer_address := contact.address;
        NEW.customer_phone := contact.phone;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER orders_customer_sync
BEFORE INSERT OR UPDATE ON orders
FOR EACH ROW EXECUTE FUNCTION sync_order_customer();

CREATE FUNCTION sync_order_item_product() RETURNS trigger AS $$
DECLARE
    catalog_product products%ROWTYPE;
BEGIN
    IF NEW.product_id IS NULL THEN
        SELECT * INTO catalog_product FROM products
        WHERE name = NEW.product_name AND price = NEW.product_price
        ORDER BY id LIMIT 1;
        IF NOT FOUND THEN
            INSERT INTO products (name, price)
            VALUES (NEW.product_name, NEW.product_price)
            RETURNING * INTO catalog_product;
        END IF;
        NEW.product_id := catalog_product.id;
    ELSE
        SELECT * INTO catalog_product FROM products WHERE id = NEW.product_id;
        NEW.product_name := catalog_product.name;
        NEW.product_price := catalog_product.price;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER order_items_product_sync
BEFORE INSERT OR UPDATE ON order_items
FOR EACH ROW EXECUTE FUNCTION sync_order_item_product();
