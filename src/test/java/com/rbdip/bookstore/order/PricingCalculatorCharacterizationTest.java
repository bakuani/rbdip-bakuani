package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingCalculatorCharacterizationTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void bulkDiscountStartsAboveTenItems() {
        assertTotal("100.00", "10.00", 10, "regular", null);
        assertTotal("104.50", "10.00", 11, "regular", null);
    }

    @Test
    void customerDiscountsAreExclusive() {
        assertTotal("90.00", "100.00", 1, "vip", null);
        assertTotal("85.00", "100.00", 1, "wholesale", null);
        assertTotal("100.00", "100.00", 1, "other", null);
        assertTotal("100.00", "100.00", 1, null, null);
    }

    @Test
    void couponsApplyAfterCustomerDiscount() {
        assertTotal("80.00", "100.00", 1, "regular", "SAVE20PERCENT");
        assertTotal("80.00", "100.00", 1, "vip", "SAVE10");
        assertTotal("100.00", "100.00", 1, "regular", "UNKNOWN");
    }

    @Test
    void negativeAndSmallTotalsAreClampedAfterCoupon() {
        assertTotal("0.00", "5.00", 1, "regular", "SAVE10");
        assertTotal("0.00", "-5.00", 1, "regular", null);
        assertThat(calculator.calculateOrderTotal(List.of(), "regular", null))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void highValueDiscountUsesStrictThresholdAfterOtherDiscounts() {
        assertTotal("1000.00", "1000.00", 1, "regular", null);
        assertTotal("980.98", "1001.00", 1, "regular", null);
        assertTotal("1014.30", "100.00", 11, "regular", "SAVE10");
    }

    @Test
    void addsLinesAndRoundsHalfUpAtTheEnd() {
        BigDecimal result = calculator.calculateOrderTotal(List.of(
                new PricingCalculator.LineItem(new BigDecimal("1.005"), 1),
                new PricingCalculator.LineItem(new BigDecimal("2.00"), 1)), "regular", null);
        assertThat(result).isEqualByComparingTo("3.01");
    }

    private void assertTotal(String expected, String price, int quantity, String customerType, String couponCode) {
        BigDecimal total = calculator.calculateOrderTotal(
                List.of(new PricingCalculator.LineItem(new BigDecimal(price), quantity)), customerType, couponCode);
        assertThat(total).isEqualByComparingTo(expected);
    }
}
