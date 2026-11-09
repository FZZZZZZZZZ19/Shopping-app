package com.shopping.app;
import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

public class OfferTest {
    private Offer offer(Platform p,String id,long cents,long at){return new Offer(p,id,"CedarPhone 16 128GB 黑色","测试店铺","","",cents,at,"页面价");}
    @Test public void sortsFourPlatformsNumericallyAndReverses(){List<Offer> a=Arrays.asList(offer(Platform.JD,"1",10000,1),offer(Platform.PDD,"2",950,1),offer(Platform.TAOBAO,"3",2000,1),offer(Platform.TMALL,"4",11000,1));List<Offer> low=Offer.select(a,"",null,false,1),high=Offer.select(a,"",null,false,2);assertEquals(950,low.get(0).cents);assertEquals(11000,high.get(0).cents);assertEquals(4,low.size());}
    @Test public void duplicateKeepsLatestObservedPriceNotArtificialMinimum(){List<Offer>a=Arrays.asList(offer(Platform.JD,"1",100,1),offer(Platform.JD,"1",200,2));List<Offer> r=Offer.select(a,"",null,false,1);assertEquals(1,r.size());assertEquals(200,r.get(0).cents);}
    @Test public void filterPreservesPlatform(){List<Offer>a=Arrays.asList(offer(Platform.JD,"1",1,1),offer(Platform.PDD,"2",2,1));assertEquals(Platform.JD,Offer.select(a,"",Platform.JD,false,1).get(0).platform);assertEquals(1,Offer.select(a,"",Platform.JD,false,1).size());}
    @Test public void rejectsSimilarDifferentModelsAndCapacity(){assertTrue(Offer.matches("CedarPhone 16 128GB 黑色","CedarPhone 16 全新 128G 黑色"));assertFalse(Offer.matches("CedarPhone 16 128GB","CedarPhone 16 Pro 128GB"));assertFalse(Offer.matches("CedarPhone 16 128GB","CedarPhone 16 256GB"));assertFalse(Offer.matches("CedarPhone16","CedarPhone160"));assertFalse(Offer.matches("CedarPhone 16 128GB","CedarPhone 16 128GB 256GB 多规格"));}
    @Test public void malformedPricesAndLinksCannotBecomeRealQuotes()throws Exception{JSONObject data=new JSONObject("{items:[{id:'1',title:'正常商品名称',price:'99.90',shop:'',url:'javascript:alert(1)'},{id:'2',title:'价格未知商品',price:'面议'},{id:'3',title:'超额优惠商品',price:'-1'},{id:'4',title:'多价串商品',price:'99 199'}]}");List<Offer> out=OfferParser.parse(data,Platform.JD,1);assertEquals(1,out.size());assertEquals(9990,out.get(0).cents);assertEquals("",out.get(0).url);assertEquals("",out.get(0).shop);}
    @Test public void taobaoSearchCanContainTmallOffer()throws Exception{JSONObject data=new JSONObject("{items:[{id:'1',title:'天猫商品测试',price:'100.00',url:'https://detail.tmall.com/item.htm?id=12345',starting:true}]}");Offer o=OfferParser.parse(data,Platform.TAOBAO,1).get(0);assertEquals(Platform.TMALL,o.platform);assertTrue(o.priceLabel.contains("起价"));}
    @Test public void chineseBrandAndProductCanBeSeparatedInTitle(){assertTrue(Offer.matches("小米耳机","小米 全新真无线 蓝牙耳机"));assertFalse(Offer.matches("小米耳机","华为真无线蓝牙耳机"));}
    @Test public void ramIsNotConfusedWithAnotherStorageVariant(){assertTrue(Offer.matches("CedarPhone 16 128GB","CedarPhone 16 8GB+128GB 黑色"));assertFalse(Offer.matches("CedarPhone 16 128GB","CedarPhone 16e 128GB 黑色"));assertFalse(Offer.matches("CedarPhone16","CedarPhone16e 黑色"));assertTrue(Offer.matches("测试耳机","测试耳机 Original Product"));}
    @Test public void cheapAccessoriesAreNotTheRequestedPhone(){assertFalse(Offer.matches("CedarPhone16","适用 CedarPhone16 手机壳"));assertFalse(Offer.matches("CedarPhone16","CedarPhone16 钢化膜"));assertTrue(Offer.matches("CedarPhone16 手机壳","适用 CedarPhone16 手机壳"));}
}
