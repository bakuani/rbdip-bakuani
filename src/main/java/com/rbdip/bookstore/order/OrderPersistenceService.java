package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.stereotype.Component;

/** Persists an order and its items. */
@Component
public class OrderPersistenceService {

    private static final String NEW_ORDER_STATUS = "new";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderPersistenceService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public Order save(CreateOrderRequest request, List<ResolvedOrderItem> items) {
        Order order = orderRepository.save(new Order(
                request.customerFullName(),
                request.customerAddress(),
                request.customerPhone(),
                NEW_ORDER_STATUS));
        for (ResolvedOrderItem item : items) {
            orderItemRepository.save(new OrderItem(
                    order.getId(), item.product().getName(), item.product().getPrice(), item.quantity()));
        }
        return order;
    }
}
