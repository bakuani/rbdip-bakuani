package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PricingCalculator {

    private static final int BULK_QUANTITY_THRESHOLD = 10;
    private static final int MONEY_SCALE = 2;
    private static final String SAVE_FIXED_AMOUNT_COUPON = "SAVE10";
    private static final String SAVE_PERCENT_COUPON = "SAVE20PERCENT";
    private static final BigDecimal BULK_DISCOUNT_FACTOR = new BigDecimal("0.95");
    private static final BigDecimal FIXED_COUPON_AMOUNT = BigDecimal.TEN;
    private static final BigDecimal PERCENT_COUPON_FACTOR = new BigDecimal("0.8");
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal HIGH_VALUE_DISCOUNT_FACTOR = new BigDecimal("0.98");
    private static final Map<String, BigDecimal> CUSTOMER_DISCOUNT_FACTORS = Map.of(
            "vip", new BigDecimal("0.9"),
            "wholesale", new BigDecimal("0.85"));

    public record LineItem(BigDecimal price, int quantity) {
    }

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = calculateItemsTotal(items);
        total = applyPercentage(total, customerDiscountFactor(customerType));
        total = applyCoupon(total, couponCode);
        total = total.max(BigDecimal.ZERO);
        if (total.compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            total = applyPercentage(total, HIGH_VALUE_DISCOUNT_FACTOR);
        }
        return total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateItemsTotal(List<LineItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (LineItem item : items) {
            BigDecimal linePrice = item.price().multiply(BigDecimal.valueOf(item.quantity()));
            if (item.quantity() > BULK_QUANTITY_THRESHOLD) {
                linePrice = applyPercentage(linePrice, BULK_DISCOUNT_FACTOR);
            }
            total = total.add(linePrice);
        }
        return total;
    }

    private BigDecimal applyCoupon(BigDecimal total, String couponCode) {
        if (SAVE_FIXED_AMOUNT_COUPON.equals(couponCode)) {
            return total.subtract(FIXED_COUPON_AMOUNT);
        }
        if (SAVE_PERCENT_COUPON.equals(couponCode)) {
            return applyPercentage(total, PERCENT_COUPON_FACTOR);
        }
        return total;
    }

    private BigDecimal customerDiscountFactor(String customerType) {
        return customerType == null ? null : CUSTOMER_DISCOUNT_FACTORS.get(customerType);
    }

    private BigDecimal applyPercentage(BigDecimal amount, BigDecimal factor) {
        return factor == null ? amount : amount.multiply(factor);
    }
}
