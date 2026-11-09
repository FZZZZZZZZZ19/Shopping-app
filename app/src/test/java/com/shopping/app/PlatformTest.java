package com.shopping.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlatformTest {
    @Test public void queryEncoding(){for(Platform p:Platform.values()){String url=p.search("耳机 & 128GB");assertTrue(url.contains("%26"));assertTrue(url.contains("%E8%80%B3"));assertTrue(Platform.isSafeUrl(url));}}
    @Test public void allPlatformsDetected(){for(Platform p:Platform.values())assertEquals(p,Platform.detect(p.home));assertEquals(Platform.PDD,Platform.detect("https://www.pinduoduo.com/"));assertEquals(Platform.TAOBAO,Platform.detect("https://m.tb.cn/xyz"));}
    @Test public void rejectLookalikeDomains(){assertNull(Platform.detect("https://taobao.com.evil.example/"));assertNull(Platform.detect("https://eviltaobao.com/"));assertNull(Platform.detect("not a URL"));}
    @Test public void safeNavigation(){assertFalse(Platform.isSafeUrl("javascript:alert(1)"));assertFalse(Platform.isSafeUrl("file:///etc/passwd"));assertFalse(Platform.isSafeUrl("http://m.jd.com/"));assertFalse(Platform.isSafeUrl("https://user:pass@jd.com/"));assertFalse(Platform.isSafeUrl("https://"));}
}
