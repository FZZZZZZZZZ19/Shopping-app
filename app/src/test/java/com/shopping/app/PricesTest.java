package com.shopping.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class PricesTest {
    @Test public void centsAreExact(){assertEquals(10,Prices.cents("0.10"));assertEquals(123456,Prices.cents("1234.56"));assertEquals("1234.56",Prices.format(123456));}
    @Test public void checkoutIncludesShippingAndCoupon(){assertEquals(9650,Prices.total(10000,650,1000));assertEquals(0,Prices.total(100,0,100));}
    @Test(expected=IllegalArgumentException.class) public void rejectCouponLargerThanCheckout(){Prices.total(100,10,111);}
    @Test(expected=IllegalArgumentException.class) public void rejectNegative(){Prices.cents("-0.01");}
    @Test(expected=IllegalArgumentException.class) public void rejectFractionalCent(){Prices.cents("1.001");}
    @Test(expected=IllegalArgumentException.class) public void rejectOversizedAmount(){Prices.cents("100000000");}
}
