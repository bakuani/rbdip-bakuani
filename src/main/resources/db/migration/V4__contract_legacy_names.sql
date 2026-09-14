DROP TRIGGER orders_customer_sync ON orders;
DROP TRIGGER order_items_product_sync ON order_items;
DROP TRIGGER customers_name_sync ON customers;

DROP FUNCTION sync_order_customer();
DROP FUNCTION sync_order_item_product();
DROP FUNCTION sync_customer_names();

ALTER TABLE orders DROP COLUMN customer_full_name;
ALTER TABLE orders DROP COLUMN customer_address;
ALTER TABLE orders DROP COLUMN customer_phone;

ALTER TABLE order_items DROP COLUMN product_name;
ALTER TABLE order_items DROP COLUMN product_price;

ALTER TABLE customers DROP CONSTRAINT uq_customer_contact;
ALTER TABLE customers DROP COLUMN full_name;
ALTER TABLE customers ADD CONSTRAINT uq_customer_name_contact
    UNIQUE NULLS NOT DISTINCT (first_name, last_name, address, phone);
