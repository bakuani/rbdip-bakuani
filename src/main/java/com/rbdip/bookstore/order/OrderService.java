package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final String REGULAR_CUSTOMER = "regular";

    private final OrderRequestValidator requestValidator;
    private final OrderItemResolver itemResolver;
    private final PricingCalculator pricingCalculator;
    private final OrderPersistenceService persistenceService;
    private final OrderConfirmationSender confirmationSender;

    public OrderService(
            OrderRequestValidator requestValidator,
            OrderItemResolver itemResolver,
            PricingCalculator pricingCalculator,
            OrderPersistenceService persistenceService,
            OrderConfirmationSender confirmationSender) {
        this.requestValidator = requestValidator;
        this.itemResolver = itemResolver;
        this.pricingCalculator = pricingCalculator;
        this.persistenceService = persistenceService;
        this.confirmationSender = confirmationSender;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        requestValidator.validate(request);
        List<ResolvedOrderItem> items = itemResolver.resolve(request.items());
        BigDecimal total = pricingCalculator.calculateOrderTotal(
                items.stream().map(ResolvedOrderItem::toLineItem).toList(),
                customerTypeOrDefault(request.customerType()),
                request.couponCode());
        Order order = persistenceService.save(request, items);
        confirmationSender.send(request.customerFullName(), order.getId(), total);
        return order;
    }

    private String customerTypeOrDefault(String customerType) {
        return customerType == null ? REGULAR_CUSTOMER : customerType;
    }
}
