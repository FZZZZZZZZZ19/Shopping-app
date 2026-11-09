package com.shopping.app;

import android.app.*;
import android.content.Intent;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

/** Test-only HTML is loaded in real WebViews. These are not live marketplace results. */
final class AggregationDeviceChecks {
    private static final String QUERY="FixturePhone 16 128GB 黑色";
    private static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private static void set(Object o,String name,Object value)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    static int run(Instrumentation test)throws Exception{
        if(!Offer.matches("FixturePhone 16 128GB 黑色","FixturePhone 16 8GB+128GB 黑色")||Offer.matches("FixturePhone16","FixturePhone16 手机壳"))throw new AssertionError("Android keyword matching regression");
        Intent i=new Intent();i.setClassName("com.shopping.app","com.shopping.app.SearchActivity");i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);SearchActivity activity=(SearchActivity)test.startActivitySync(i);
        PageCollector collector=(PageCollector)field(activity,"collector");
        test.runOnMainSync(()->{
            try{set(activity,"query",QUERY);set(collector,"query",QUERY);((EditText)field(activity,"input")).setText(QUERY);}catch(Exception e){throw new RuntimeException(e);}
            for(PageCollector.Source s:collector.sources){
                int index=s.platform.ordinal();String[] prices={"3899.00","3799.00","3999.00","4099.00"};String id="9100"+index;
                String url=s.platform==Platform.TAOBAO?"https://item.taobao.com/item.htm?id="+id:s.platform==Platform.TMALL?"https://detail.tmall.com/item.htm?id="+id:s.platform==Platform.PDD?"https://mobile.yangkeduo.com/goods.html?goods_id="+id:"https://item.m.jd.com/product/"+id+".html";
                String html="<html><head><title>TEST FIXTURE ONLY</title></head><body><div class='goods-item'><a href='"+url+"'><img alt='"+QUERY+"'/><h3 class='title'>"+QUERY+"</h3></a><div class='price'>¥"+prices[index]+"</div><div class='shop-name'>测试"+s.platform.label+"店铺</div></div></body></html>";
                android.webkit.WebViewClient original=s.web.getWebViewClient();
                s.web.setWebViewClient(new android.webkit.WebViewClient(){
                    @Override public android.webkit.WebResourceResponse shouldInterceptRequest(android.webkit.WebView view,android.webkit.WebResourceRequest request){
                        if(request.isForMainFrame()){String body=request.getUrl().getPath().contains("login.html")?"<html><body>请先登录<input type='password'/></body></html>":html;return new android.webkit.WebResourceResponse("text/html","UTF-8",new java.io.ByteArrayInputStream(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}return null;
                    }
                    @Override public void onPageStarted(android.webkit.WebView v,String url,android.graphics.Bitmap icon){original.onPageStarted(v,url,icon);}
                    @Override public void onPageFinished(android.webkit.WebView v,String url){original.onPageFinished(v,url);}
                    @Override public void onReceivedError(android.webkit.WebView v,android.webkit.WebResourceRequest r,android.webkit.WebResourceError e){original.onReceivedError(v,r,e);}
                    @Override public void onReceivedHttpError(android.webkit.WebView v,android.webkit.WebResourceRequest r,android.webkit.WebResourceResponse response){original.onReceivedHttpError(v,r,response);}
                });
                s.collecting=true;s.deadline=SystemClock.elapsedRealtime()+60000;s.web.loadUrl(s.platform.search(QUERY));
            }
        });
        try{waitFor(test,()->collector.all().size()==4,"four WebViews must yield four parsed offers");}catch(AssertionError error){StringBuilder details=new StringBuilder();test.runOnMainSync(()->{for(PageCollector.Source s:collector.sources)details.append(s.platform).append(':').append(s.status).append(" n=").append(s.offers.size()).append(" url=").append(s.web.getUrl()).append(" diagnostic=").append(s.diagnostic).append("; ");});throw new AssertionError(error.getMessage()+" "+details);}
        List<String> ascending=priceTexts(test,activity);if(!ascending.equals(Arrays.asList("¥ 3799.00","¥ 3899.00","¥ 3999.00","¥ 4099.00")))throw new AssertionError("aggregate ascending UI "+ascending);
        for(String p:new String[]{"测试淘宝店铺","测试拼多多店铺","测试京东店铺","测试天猫店铺"})if(!texts(test,activity).contains("店铺："+p))throw new AssertionError("missing actual shop label "+p);
        android.graphics.Bitmap screenshot=test.getUiAutomation().takeScreenshot();if(screenshot!=null)try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(test.getTargetContext().getFilesDir(),"aggregate-fixture.png"))){screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
        test.runOnMainSync(()->spinner(activity,"价格排序").setSelection(2));test.waitForIdleSync();SystemClock.sleep(500);
        if(!priceTexts(test,activity).get(0).equals("¥ 4099.00"))throw new AssertionError("descending selection did not reorder prices");
        test.runOnMainSync(()->spinner(activity,"平台筛选").setSelection(3));test.waitForIdleSync();SystemClock.sleep(500);
        if(!priceTexts(test,activity).equals(Collections.singletonList("¥ 3999.00")))throw new AssertionError("JD filter wrong");
        test.runOnMainSync(()->collector.sources[Platform.PDD.ordinal()].web.loadUrl("https://mobile.yangkeduo.com/login.html"));
        waitFor(test,()->collector.sources[Platform.PDD.ordinal()].status.equals("需要登录"),"login must be reported");
        if(!collector.sources[Platform.PDD.ordinal()].offers.isEmpty())throw new AssertionError("stale offers remained after login expiry");
        test.runOnMainSync(activity::finish);return 5;
    }
    private interface Condition{boolean ok();}
    private static void waitFor(Instrumentation test,Condition condition,String message)throws Exception{for(int i=0;i<200;i++){final boolean[] ok={false};test.runOnMainSync(()->ok[0]=condition.ok());if(ok[0]){test.waitForIdleSync();return;}SystemClock.sleep(300);}throw new AssertionError(message);}
    private static List<View> views(Activity a){List<View> out=new ArrayList<>();walk(a.getWindow().getDecorView(),out);return out;}
    private static void walk(View view,List<View> out){out.add(view);if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)walk(group.getChildAt(i),out);}}
    private static Spinner spinner(Activity a,String label){for(View v:views(a))if(v instanceof Spinner&&label.equals(v.getContentDescription()))return (Spinner)v;throw new AssertionError(label);}
    private static List<String> texts(Instrumentation test,Activity a){List<String> out=new ArrayList<>();test.runOnMainSync(()->{for(View v:views(a))if(v instanceof TextView)out.add(((TextView)v).getText().toString());});return out;}
    private static List<String> priceTexts(Instrumentation test,Activity a){List<String> out=new ArrayList<>();for(String text:texts(test,a))if(text.startsWith("¥ "))out.add(text);return out;}
}
