package com.shopping.app;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** A single observed search-page offer. It is never a guaranteed checkout/SKU price. */
public final class Offer {
    public final Platform platform;
    public final String id,title,shop,url,image,priceLabel;
    public final long cents, observedAt;
    public Offer(Platform platform,String id,String title,String shop,String url,String image,long cents,long observedAt,String priceLabel) {
        if(platform==null || id==null || id.trim().isEmpty() || title==null || title.trim().length()<3 || cents<0 || cents>9999999999L || observedAt<0) throw new IllegalArgumentException("无效商品");
        this.platform=platform;this.id=id;this.title=title;this.shop=shop==null?"":shop;this.url=url==null?"":url;this.image=image==null?"":image;this.cents=cents;this.observedAt=observedAt;this.priceLabel=priceLabel==null?"页面展示价":priceLabel;
    }
    public String key(){return platform.name()+":"+id;}
    private static String normalize(String s){return Normalizer.normalize(s,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("([0-9]+)\\s*gb","$1g").replaceAll("([0-9]+)\\s*tb","$1t");}
    private static String compact(String s){return normalize(s).replaceAll("[^a-z0-9\\u3400-\\u9fff]","");}
    public static boolean matches(String query,String title) {
        String q=normalize(query).trim(), t=compact(title);
        if(q.isEmpty())return false;
        for(String accessory:new String[]{"手机壳","保护壳","保护套","钢化膜","贴膜","支架","镜头膜","转接头","空盒","包装盒","收纳包","耳机套"}){
            if(t.contains(accessory)&&!compact(q).contains(accessory))return false;
        }
        Matcher words=Pattern.compile("[a-z]+[0-9]*[a-z0-9]*|[0-9]+[a-z]*|[\\u3400-\\u9fff]+").matcher(q);
        boolean any=false;
        while(words.find()){
            String token=words.group();any=true;
            boolean present=t.contains(token);
            if(!present&&token.matches("[\\u3400-\\u9fff]+")){
                for(String brand:new String[]{"小米","红米","华为","荣耀","苹果","三星","联想","索尼","飞利浦","美的","海尔","格力","海信","罗技","雷蛇","任天堂","微软","惠普","佳能","尼康","松下","倍思","绿联","安克"}){
                    if(token.startsWith(brand)&&token.length()>brand.length()&&t.contains(brand)&&t.contains(token.substring(brand.length()))){present=true;break;}
                }
            }
            if(!present)return false;
            if(token.matches("[a-z]+[0-9]+")){
                if(!Pattern.compile(Pattern.quote(token)+"(?![0-9a-z])").matcher(normalize(title).replaceAll("([a-z])\\s+([0-9])","$1$2")).find())return false;
            }
            // A requested model number must not silently match another number (16 vs 160).
            if(token.matches("[0-9]+[a-z]*")) {
                if(!Pattern.compile("(?<![0-9])"+Pattern.quote(token)+"(?![0-9a-z])").matcher(normalize(title)).find())return false;
            }
        }
        for(String variant:new String[]{"promax","pro","max","ultra","plus","mini","lite","青春版"}){
            boolean variantInTitle=variant.equals("青春版")?t.contains(variant):Pattern.compile("(?<![a-z])"+variant+"(?![a-z])").matcher(normalize(title)).find();
            if(variantInTitle&&!compact(q).contains(variant))return false;
        }
        Set<String> requested=capacities(q), listed=capacities(normalize(title));
        if(!requested.isEmpty()&&!listed.isEmpty()){
            if(!listed.containsAll(requested))return false;
            Set<String> extra=new HashSet<>(listed);extra.removeAll(requested);
            for(String capacity:extra)if(!capacity.endsWith("g")||Integer.parseInt(capacity.substring(0,capacity.length()-1))>24)return false;
        }
        return any;
    }
    private static Set<String> capacities(String s){Set<String>a=new HashSet<>();Matcher m=Pattern.compile("(?<![0-9])([0-9]{2,4}|[1248])\\s*([gt])(?:b)?(?![a-z])").matcher(s);while(m.find())a.add(m.group(1)+m.group(2));return a;}
    public static List<Offer> select(Collection<Offer> input,String query,Platform only,boolean strict,int order){
        Map<String,Offer> unique=new LinkedHashMap<>();
        for(Offer o:input)if((only==null||o.platform==only)&&(!strict||matches(query,o.title))){Offer old=unique.get(o.key());if(old==null||o.observedAt>old.observedAt)unique.put(o.key(),o);}
        List<Offer> out=new ArrayList<>(unique.values());Comparator<Offer> price=Comparator.comparingLong(o->o.cents);if(order==2)price=price.reversed();
        if(order!=0)out.sort(price.thenComparing(o->o.platform.ordinal()).thenComparing(o->o.id));return out;
    }
}
