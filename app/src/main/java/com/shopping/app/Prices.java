package com.shopping.app;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Prices {
    private Prices() {}
    public static long cents(String text) {
        BigDecimal value=new BigDecimal(text.trim());
        if(value.signum()<0 || value.compareTo(new BigDecimal("99999999.99"))>0 || value.stripTrailingZeros().scale()>2) throw new IllegalArgumentException("请输入有效金额，最多两位小数");
        return value.movePointRight(2).longValueExact();
    }
    public static long total(long price, long shipping, long discount) {
        if(price<0 || shipping<0 || discount<0 || discount>price+shipping) throw new IllegalArgumentException("优惠不能超过商品价格与运费之和");
        return Math.addExact(price,shipping)-discount;
    }
    public static String format(long cents) { return BigDecimal.valueOf(cents,2).setScale(2, RoundingMode.UNNECESSARY).toPlainString(); }
}
