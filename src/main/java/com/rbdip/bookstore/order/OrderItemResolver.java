package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderItemResolver {

    private final ProductRepository productRepository;
    private final OrderRequestValidator requestValidator;

    public OrderItemResolver(ProductRepository productRepository, OrderRequestValidator requestValidator) {
        this.productRepository = productRepository;
        this.requestValidator = requestValidator;
    }

    public List<ResolvedOrderItem> resolve(List<CreateOrderRequest.Item> requestedItems) {
        List<ResolvedOrderItem> resolvedItems = new ArrayList<>();
        for (CreateOrderRequest.Item requestedItem : requestedItems) {
            Product product = findProduct(requestedItem.productId());
            int quantity = requestValidator.validateQuantity(requestedItem.quantity());
            resolvedItems.add(new ResolvedOrderItem(product, quantity));
        }
        return resolvedItems;
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("product " + productId + " not found"));
    }
}
