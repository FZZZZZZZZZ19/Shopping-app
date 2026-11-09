package com.shopping.app;

import android.app.Activity;
import android.os.*;
import android.webkit.*;
import android.widget.FrameLayout;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Reads only page-rendered product DOM in the user's own WebView session. */
final class PageCollector {
    interface Listener { void changed(); }
    static final class Source {
        final Platform platform;
        final WebView web;
        final Map<String,Offer> offers=new LinkedHashMap<>();
        String status="尚未搜索",detail="",error="",diagnostic="";
        long deadline;
        boolean collecting=false,reading=false;
        Source(Platform p,WebView w){platform=p;web=w;}
    }
    final Source[] sources=new Source[4];
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Listener listener;
    private String script,query="";
    private boolean destroyed=false, paused=false;
    private int generation=0, selected=-1;
    private final Runnable poll=new Runnable(){@Override public void run(){if(destroyed)return;if(!paused)for(Source s:sources)if(s.collecting||s.platform.ordinal()==selected)read(s);handler.postDelayed(this,3500);}};
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    PageCollector(Activity activity,FrameLayout host,Listener listener)throws IOException{
        this.listener=listener;
        try(InputStream in=activity.getAssets().open("collect-offers.js");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);script=new String(out.toByteArray(),StandardCharsets.UTF_8);}
        for(Platform p:Platform.values()){
            WebView web=new WebView(activity);web.setContentDescription(p.label+"官方网页");
            WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setJavaScriptCanOpenWindowsAutomatically(false);settings.setSupportMultipleWindows(false);settings.setSafeBrowsingEnabled(true);
            CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
            Source s=new Source(p,web);sources[p.ordinal()]=s;host.addView(web,new FrameLayout.LayoutParams(-1,-1));
            web.setWebViewClient(new WebViewClient(){
                @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){String url=r.getUrl().toString();if(Platform.isSafeUrl(url))return false;if(r.isForMainFrame())Ui.toast(activity,"请在当前官方网页完成操作；应用唤起链接可用外部打开");return true;}
                @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap icon){s.reading=false;s.error="";s.detail="正在加载官方页面";s.status="加载中";listener.changed();}
                @Override public void onPageFinished(WebView v,String url){if(destroyed)return;CookieManager.getInstance().flush();read(s);}
                @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()){s.error="网页加载失败";s.status="网络异常";s.detail="检查网络后重试";s.collecting=false;listener.changed();}}
                @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse response){if(r.isForMainFrame()&&response.getStatusCode()>=400){s.error="HTTP "+response.getStatusCode();s.status="访问受限";s.detail="平台返回 "+response.getStatusCode()+"，请打开官方页面查看";s.collecting=false;listener.changed();}}
            });
        }
        handler.postDelayed(poll,1500);
    }
    void search(String word){query=word.trim();generation++;for(Source s:sources){s.web.stopLoading();s.offers.clear();s.error="";s.reading=false;s.status="搜索中";s.detail="等待页面展示商品";s.collecting=true;s.deadline=SystemClock.elapsedRealtime()+45000;s.web.loadUrl(s.platform.search(query));}listener.changed();}
    void open(Platform p){selected=p.ordinal();Source s=sources[selected];s.web.bringToFront();if(s.web.getUrl()==null)s.web.loadUrl(query.isEmpty()?p.home:p.search(query));s.collecting=true;s.deadline=SystemClock.elapsedRealtime()+45000;}
    void closePage(){if(selected>=0)read(sources[selected]);selected=-1;}
    void retry(Platform p){Source s=sources[p.ordinal()];s.web.stopLoading();s.offers.clear();s.error="";s.status="搜索中";s.reading=false;s.collecting=true;s.deadline=SystemClock.elapsedRealtime()+45000;s.web.loadUrl(query.isEmpty()?p.home:p.search(query));listener.changed();}
    void collectNow(){for(Source s:sources){s.collecting=true;s.deadline=SystemClock.elapsedRealtime()+20000;read(s);}}
    private boolean trusted(Source s,String url){Platform actual=Platform.detect(url==null?"":url);return actual==s.platform||((s.platform==Platform.TAOBAO||s.platform==Platform.TMALL)&&(actual==Platform.TAOBAO||actual==Platform.TMALL));}
    private boolean searchDocument(String url){
        try{android.net.Uri uri=android.net.Uri.parse(url);String host=uri.getHost(),path=uri.getPath();if(host==null||path==null||!(host+path).toLowerCase(Locale.ROOT).contains("search")&& !host.startsWith("s."))return false;
            for(String parameter:new String[]{"q","keyword","search_key"}){String value=uri.getQueryParameter(parameter);if(value!=null&&!value.trim().equalsIgnoreCase(query))return false;}return true;
        }catch(Exception e){return false;}
    }
    private void read(Source s){
        if(destroyed||s.reading||!s.error.isEmpty())return;
        if(query.isEmpty()){s.status="网页登录";s.detail="完成登录后输入商品关键词；此处不显示推荐商品作为搜索结果";listener.changed();return;}
        String url=s.web.getUrl();if(url==null||!trusted(s,url)){if(url!=null){s.status="请完成登录";s.detail="登录后返回搜索页再采集";listener.changed();}return;}
        s.reading=true;int request=generation;String documentUrl=url;
        String code="(function(){try { return "+script.replace("__PLATFORM__",JSONObject.quote(s.platform.name()))+" } catch(e) { return JSON.stringify({items:[],error:String(e)}); }})()";
        s.web.evaluateJavascript(code,value->{
            if(destroyed||request!=generation)return;s.reading=false;if(!documentUrl.equals(s.web.getUrl()))return;
            try{
                Object decoded=new JSONTokener(value==null?"null":value).nextValue();if(!(decoded instanceof String))throw new JSONException("页面尚未就绪");JSONObject data=new JSONObject((String)decoded);
                s.diagnostic=data.optString("error","");
                List<Offer> found=searchDocument(documentUrl)?OfferParser.parse(data,s.platform,System.currentTimeMillis()):Collections.emptyList();for(Offer o:found)s.offers.put(o.key(),o);
                if(!found.isEmpty()){s.status="已读取 "+s.offers.size()+" 条";s.detail="只包含当前页面已加载的商品，可打开页面滚动后继续读取";}
                else if(data.optBoolean("verification")){s.offers.clear();s.status="需要验证";s.detail="请打开官方页面，由你完成验证码或安全验证";}
                else if(data.optBoolean("login")){s.offers.clear();s.status="需要登录";s.detail="请在本应用的官方网页中登录，再返回搜索页";}
                else if(data.optBoolean("empty")){s.status="平台无结果";s.detail="页面明确显示没有相关商品";}
                else if(!searchDocument(documentUrl)){s.status="请返回搜索页";s.detail="完成登录后，返回汇总并点击此平台的“重新搜索”，不会把首页推荐当作搜索结果";}
                else if(s.offers.isEmpty()){boolean ended=SystemClock.elapsedRealtime()>s.deadline;s.status=ended?"未读到商品":"等待商品加载";s.detail=ended?"页面布局可能不兼容，或还未进入搜索结果；打开官方页面检查后重试":"等待页面加载，登录或验证状态会单独提示";}
            }catch(Exception e){s.diagnostic=e.toString();if(SystemClock.elapsedRealtime()>s.deadline&&s.offers.isEmpty()){s.status="未读到商品";s.detail="页面未返回可识别的商品数据";}}
            if(SystemClock.elapsedRealtime()>s.deadline)s.collecting=false;listener.changed();
        });
    }
    List<Offer> all(){List<Offer> out=new ArrayList<>();for(Source s:sources)out.addAll(s.offers.values());return out;}
    void pause(){paused=true;for(Source s:sources)s.web.onPause();}
    void resume(){paused=false;for(Source s:sources)s.web.onResume();}
    void destroy(){destroyed=true;handler.removeCallbacksAndMessages(null);for(Source s:sources){s.web.stopLoading();s.web.destroy();}}
}
