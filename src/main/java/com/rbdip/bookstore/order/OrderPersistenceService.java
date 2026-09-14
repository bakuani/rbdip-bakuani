package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceService {

    private static final String NEW_ORDER_STATUS = "new";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;

    public OrderPersistenceService(
            OrderRepository orderRepository, OrderItemRepository orderItemRepository,
            CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.customerRepository = customerRepository;
    }

    public Order save(CreateOrderRequest request, List<ResolvedOrderItem> items) {
        Customer customer = customerRepository.findByFullNameAndAddressAndPhone(
                        request.customerFullName(), request.customerAddress(), request.customerPhone())
                .orElseGet(() -> customerRepository.save(new Customer(
                        request.customerFullName(), request.customerAddress(), request.customerPhone())));
        Order order = orderRepository.save(new Order(customer, NEW_ORDER_STATUS));
        for (ResolvedOrderItem item : items) {
            orderItemRepository.save(new OrderItem(order.getId(), item.product(), item.quantity()));
        }
        return order;
    }
}
