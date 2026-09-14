package com.rbdip.bookstore.order;

import com.rbdip.bookstore.purchase.OrderPurchasePort;
import org.springframework.stereotype.Component;

@Component
public class OrderPurchasePortAdapter implements OrderPurchasePort {

    private final OrderItemRepository orderItemRepository;

    public OrderPurchasePortAdapter(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public boolean hasPurchaseForProduct(Long productId) {
        return orderItemRepository.existsByProduct_Id(productId);
    }
}