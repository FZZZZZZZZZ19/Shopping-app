package com.shopping.app;

import org.json.*;
import java.util.*;

public final class OfferParser {
    public static List<Offer> parse(JSONObject data,Platform source,long now){
        List<Offer> list=new ArrayList<>();JSONArray items=data.optJSONArray("items");if(items==null)return list;
        for(int i=0;i<Math.min(items.length(),100);i++)try{
            JSONObject item=items.getJSONObject(i);String title=item.getString("title").trim();if(title.length()>500)continue;
            Platform platform=source;String link=item.optString("url","");Platform detected=Platform.detect(link);
            if((source==Platform.TAOBAO||source==Platform.TMALL)&&(detected==Platform.TMALL||detected==Platform.TAOBAO))platform=detected;
            if(!link.isEmpty()&&(!Platform.isSafeUrl(link)||Platform.detect(link)!=platform))link="";
            String shop=item.optString("shop","").trim();if(shop.length()>150)shop=shop.substring(0,150);
            String id=item.getString("id");if(id.length()>500)continue;
            String raw=item.getString("price");if(!raw.matches("[0-9]+(?:\\.[0-9]{1,2})?"))continue;
            long cents=Prices.cents(raw);String label=item.optBoolean("starting",false)?"页面起价 · 规格待核对":"页面展示价 · 规格待核对";
            String image=item.optString("image","");if(!Platform.isSafeUrl(image))image="";
            list.add(new Offer(platform,id,title,shop,link,image,cents,now,label));
        }catch(Exception ignored){}return list;
    }
    private OfferParser(){}
}
