package com.shopping.app;

import android.app.*;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.view.*;
import android.webkit.*;
import android.widget.*;

public class BrowserActivity extends Activity {
    private WebView web;
    private LinearLayout root,tabs,error;
    private TextView host;
    private ProgressBar progress;
    private String query="";
    private Platform platform=Platform.TAOBAO;
    private Store store;

    // Official shopping pages require JavaScript. No native bridge, file access, or mixed content is exposed.
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);store=new Store(this);
        query=getIntent().getStringExtra("query");if(query==null)query="";
        try{platform=Platform.valueOf(saved!=null?saved.getString("platform"):getIntent().getStringExtra("platform"));}catch(Exception ignored){}
        root=Ui.col(this);root.setBackgroundColor(Ui.BG);setContentView(root);
        LinearLayout bar=Ui.row(this);Ui.pad(bar,10);TextView back=Ui.button(this,"‹ 返回",false,this::finish);bar.addView(back);
        host=Ui.text(this,"平台官方网页",12,Ui.INK,true);host.setGravity(Gravity.CENTER);host.setMaxLines(1);host.setEllipsize(android.text.TextUtils.TruncateAt.END);Ui.weighted(bar,host);bar.addView(Ui.button(this,"外部打开",false,()->Ui.external(this,currentUrl())));root.addView(bar);
        tabs=Ui.row(this);drawTabs();root.addView(tabs);
        TextView notice=Ui.text(this,"价格由平台展示 · 可记录报价到比价本 · 登录可能受平台限制",10,Ui.MUTED,false);notice.setPadding(Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,8));root.addView(notice);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);root.addView(progress,new LinearLayout.LayoutParams(-1,Ui.dp(this,3)));
        error=Ui.col(this);Ui.pad(error,20);error.setVisibility(View.GONE);root.addView(error);
        web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setJavaScriptCanOpenWindowsAutomatically(false);settings.setSupportMultipleWindows(false);settings.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView v,int n){progress.setProgress(n);progress.setVisibility(n==100?View.GONE:View.VISIBLE);}});
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
                String url=r.getUrl().toString();if(Platform.isSafeUrl(url))return false;
                if(r.isForMainFrame())Ui.toast(BrowserActivity.this,"平台尝试唤起应用，请点击“外部打开”继续");return true;
            }
            @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap icon){host.setText(android.net.Uri.parse(url).getHost());error.setVisibility(View.GONE);}
            @Override public void onPageFinished(WebView v,String url){CookieManager.getInstance().flush();}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())showError("网页暂时无法加载。请检查网络，重试或在外部浏览器打开。");}
            @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse response){if(r.isForMainFrame()&&response.getStatusCode()>=400)showError("平台返回 "+response.getStatusCode()+"。可尝试登录、重试，或在外部浏览器打开。");}
        });
        LinearLayout tools=Ui.row(this);Ui.pad(tools,10);Ui.weighted(tools,Ui.button(this,"＋ 记录报价",true,this::record));TextView save=Ui.button(this,"收藏店铺",false,this::saveShop);LinearLayout.LayoutParams margin=new LinearLayout.LayoutParams(-2,-2);margin.leftMargin=Ui.dp(this,8);tools.addView(save,margin);root.addView(tools);
        if(saved==null||web.restoreState(saved)==null){String url=getIntent().getStringExtra("url");if(Platform.isSafeUrl(url==null?"":url))web.loadUrl(url);else web.loadUrl(platform.home);}
    }
    private void drawTabs(){tabs.removeAllViews();for(Platform p:Platform.values()){TextView t=Ui.text(this,p.label,14,p==platform?Ui.PURPLE:Ui.MUTED,p==platform);t.setGravity(Gravity.CENTER);t.setPadding(0,Ui.dp(this,13),0,Ui.dp(this,13));t.setBackgroundColor(p==platform?Color.rgb(236,232,255):Ui.BG);t.setOnClickListener(v->{platform=p;drawTabs();web.loadUrl(query.isEmpty()?p.home:p.search(query));});Ui.weighted(tabs,t);}}
    private String currentUrl(){String url=web.getUrl();return Platform.isSafeUrl(url==null?"":url)?url:platform.home;}
    private void showError(String message){error.removeAllViews();error.addView(Ui.text(this,message,13,Ui.INK,false));Ui.gap(error,10);error.addView(Ui.button(this,"重新加载",false,()->web.reload()));error.setVisibility(View.VISIBLE);}
    private void record(){Intent intent=new Intent(this,MainActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);intent.putExtra("quote",true);intent.putExtra("query",query);intent.putExtra("url",currentUrl());intent.putExtra("platformIndex",platform.ordinal());startActivity(intent);finish();}
    private void saveShop(){
        String url=currentUrl();Platform detected=Platform.detect(url);if(detected==null){Ui.toast(this,"请在支持的平台店铺页收藏");return;}
        LinearLayout form=Ui.col(this);Ui.pad(form,20);EditText name=Ui.input(this,"为这家店铺起个名字");String title=web.getTitle();if(title!=null)name.setText(title);form.addView(name);Ui.gap(form,10);form.addView(Ui.text(this,"将收藏当前页面，请先确认已进入店铺主页。",12,Ui.MUTED,false));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("收藏当前店铺页").setView(form).setPositiveButton("收藏",null).setNegativeButton("取消",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{if(name.getText().toString().trim().isEmpty()){name.setError("请填写店铺名称");return;}for(org.json.JSONObject o:store.all("shops"))if(o.optString("url").equals(url)){Ui.toast(this,"该链接已收藏");dialog.dismiss();return;}
            try{org.json.JSONObject item=new org.json.JSONObject();item.put("id",java.util.UUID.randomUUID().toString());item.put("name",name.getText().toString().trim());item.put("url",url);item.put("platform",detected.name());store.add("shops",item);Ui.toast(this,"已加入店铺集");dialog.dismiss();}catch(Exception e){Ui.toast(this,"收藏失败");}
        }));dialog.show();
    }
    @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);web.saveState(out);out.putString("platform",platform.name());}
    @Override protected void onPause(){super.onPause();if(web!=null)web.onPause();}
    @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();}
    @Override protected void onDestroy(){if(web!=null){web.stopLoading();root.removeView(web);web.destroy();}super.onDestroy();}
}
