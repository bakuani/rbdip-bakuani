package com.rbdip.bookstore.order;

import org.springframework.stereotype.Component;

/** Validates data supplied by a customer when an order is placed. */
@Component
public class OrderRequestValidator {

    private static final int DEFAULT_QUANTITY = 1;

    public void validate(CreateOrderRequest request) {
        requireText(request.customerFullName(), "customerFullName is required");
        requireText(request.customerAddress(), "customerAddress is required");
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
    }

    public int validateQuantity(Integer quantity) {
        int normalizedQuantity = quantity == null ? DEFAULT_QUANTITY : quantity;
        if (normalizedQuantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return normalizedQuantity;
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
