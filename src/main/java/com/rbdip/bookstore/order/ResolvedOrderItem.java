package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;

public record ResolvedOrderItem(Product product, int quantity) {

    public PricingCalculator.LineItem toLineItem() {
        return new PricingCalculator.LineItem(product.getPrice(), quantity);
    }
}
