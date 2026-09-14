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
ALTER TABLE orders DROP COLUMN customer_full_name;
ALTER TABLE orders DROP COLUMN customer_address;
ALTER TABLE orders DROP COLUMN customer_phone;

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
ALTER TABLE order_items DROP COLUMN product_name;
ALTER TABLE order_items DROP COLUMN product_price;

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
