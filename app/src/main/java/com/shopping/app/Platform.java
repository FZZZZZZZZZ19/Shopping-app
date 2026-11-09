package com.shopping.app;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.Locale;

public enum Platform {
    TAOBAO("淘宝", "淘", "#FF6B23", "https://main.m.taobao.com/", "https://s.m.taobao.com/search?q=", "taobao.com"),
    PDD("拼多多", "拼", "#EA4458", "https://mobile.yangkeduo.com/", "https://mobile.yangkeduo.com/search_result.html?search_key=", "yangkeduo.com"),
    JD("京东", "京", "#D93848", "https://m.jd.com/", "https://so.m.jd.com/ware/search.action?keyword=", "jd.com"),
    TMALL("天猫", "猫", "#202434", "https://www.tmall.com/", "https://list.tmall.com/search_product.htm?q=", "tmall.com");
    public final String label, mark, color, home, searchPrefix, domain;
    Platform(String label, String mark, String color, String home, String searchPrefix, String domain) {
        this.label=label; this.mark=mark; this.color=color; this.home=home; this.searchPrefix=searchPrefix; this.domain=domain;
    }
    public String search(String query) { try { return searchPrefix + URLEncoder.encode(query.trim(), "UTF-8"); } catch(java.io.UnsupportedEncodingException e) { throw new AssertionError(e); } }
    public static boolean isSafeUrl(String input) {
        try {
            URI uri=URI.create(input.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost()!=null && uri.getUserInfo()==null;
        } catch (Exception e) { return false; }
    }
    public static Platform detect(String url) {
        try {
            String host=URI.create(url).getHost().toLowerCase(Locale.ROOT);
            for (Platform p:values()) if(host.equals(p.domain)||host.endsWith("."+p.domain)) return p;
            if(host.equals("pinduoduo.com")||host.endsWith(".pinduoduo.com")) return PDD;
            if(host.equals("tb.cn")||host.endsWith(".tb.cn")) return TAOBAO;
            if(host.equals("jd.hk")||host.endsWith(".jd.hk")) return JD;
        } catch (Exception ignored) {}
        return null;
    }
}
