package com.nestora.nestora_app.enums;

import java.math.BigDecimal;

public enum PropertyAccessPlan {
    MONTHLY_1(1, BigDecimal.valueOf(599)),
    MONTHLY_2(2, BigDecimal.valueOf(899)),
    MONTHLY_3(3, BigDecimal.valueOf(1299)),
    MONTHLY_4(4, BigDecimal.valueOf(1599)),
    MONTHLY_5(5, BigDecimal.valueOf(1899));

    private final int durationMonths;
    private final BigDecimal price;

    PropertyAccessPlan(int durationMonths, BigDecimal price) {
        this.durationMonths = durationMonths;
        this.price = price;
    }

    public int getDurationMonths() { return durationMonths; }
    public BigDecimal getPrice() { return price; }
}