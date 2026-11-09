package com.shopping.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.util.*;

public final class SearchActivity extends Activity implements PageCollector.Listener {
    private PageCollector collector;
    private FrameLayout layer,webHost;
    private LinearLayout summary,results,browserBar;
    private ScrollView scroll;
    private final TextView[] sourceLabels=new TextView[4];
    private TextView browserTitle,countLabel;
    private EditText input;
    private String query="",fingerprint="";
    private int sort=1,filter=0;
    private boolean strict=true;
    private Platform browserPlatform;
    private final ThumbnailLoader thumbnails=new ThumbnailLoader();

    @Override public void onCreate(Bundle state){
        super.onCreate(state);query=state==null?getIntent().getStringExtra("query"):state.getString("query","");if(query==null)query="";
        if(state!=null){sort=state.getInt("sort",1);filter=state.getInt("filter");strict=state.getBoolean("strict",true);}
        LinearLayout root=Ui.col(this);root.setBackgroundColor(Ui.BG);setContentView(root);
        LinearLayout heading=Ui.row(this);heading.setPadding(d(12),d(10),d(12),d(6));heading.addView(Ui.button(this,"‹ 返回",false,this::finish));
        TextView title=Ui.text(this,"四平台搜同款",19,Ui.INK,true);title.setGravity(Gravity.CENTER);Ui.weighted(heading,title);heading.addView(Ui.text(this,"1.1",12,Ui.PURPLE,true));root.addView(heading);
        LinearLayout search=Ui.row(this);search.setPadding(d(14),d(6),d(14),d(10));input=Ui.input(this,"商品型号、容量、颜色");input.setText(query);input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);Ui.weighted(search,input);search.addView(Ui.button(this,"搜索",true,()->startSearch(input.getText().toString())));root.addView(search);
        input.setOnEditorActionListener((v,a,e)->{if(a==EditorInfo.IME_ACTION_SEARCH){startSearch(input.getText().toString());return true;}return false;});
        LinearLayout chips=Ui.row(this);chips.setPadding(d(10),0,d(10),d(8));for(Platform p:Platform.values()){TextView t=Ui.text(this,p.label+"\n点击登录",11,Color.parseColor(p.color),true);t.setGravity(Gravity.CENTER);t.setPadding(d(2),d(10),d(2),d(10));t.setMaxLines(2);t.setOnClickListener(v->openPage(p));sourceLabels[p.ordinal()]=t;Ui.weighted(chips,t);}root.addView(chips);
        browserBar=Ui.row(this);browserBar.setPadding(d(10),d(5),d(10),d(5));browserBar.setVisibility(View.GONE);browserBar.addView(Ui.button(this,"返回汇总",true,this::closePage));browserTitle=Ui.text(this,"",11,Ui.INK,true);browserTitle.setGravity(Gravity.CENTER);Ui.weighted(browserBar,browserTitle);browserBar.addView(Ui.button(this,"采集此页",false,()->{collector.collectNow();Ui.toast(this,"正在读取页面已展示的商品");}));root.addView(browserBar);
        layer=new FrameLayout(this);root.addView(layer,new LinearLayout.LayoutParams(-1,0,1));webHost=new FrameLayout(this);layer.addView(webHost,new FrameLayout.LayoutParams(-1,-1));
        summary=Ui.col(this);summary.setBackgroundColor(Ui.BG);layer.addView(summary,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout controls=Ui.row(this);controls.setPadding(d(14),d(6),d(14),d(4));Spinner order=new Spinner(this);order.setContentDescription("价格排序");order.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"页面顺序","价格从低到高","价格从高到低"}));order.setSelection(sort);Ui.weighted(controls,order);
        Spinner platform=new Spinner(this);platform.setContentDescription("平台筛选");platform.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"全部平台","淘宝","拼多多","京东","天猫"}));platform.setSelection(filter);Ui.weighted(controls,platform);summary.addView(controls);
        order.setOnItemSelectedListener(selected(n->{sort=n;renderResults();}));platform.setOnItemSelectedListener(selected(n->{filter=n;renderResults();}));
        LinearLayout options=Ui.row(this);options.setPadding(d(10),0,d(14),d(2));CheckBox match=new CheckBox(this);match.setText("严格匹配关键词 / 规格");match.setTextSize(12);match.setChecked(strict);Ui.weighted(options,match);match.setOnCheckedChangeListener((b,value)->{strict=value;renderResults();});options.addView(Ui.button(this,"刷新读取",false,()->collector.collectNow()));summary.addView(options);
        countLabel=Ui.text(this,"",12,Ui.MUTED,false);countLabel.setPadding(d(18),d(8),d(18),d(8));summary.addView(countLabel);
        scroll=new ScrollView(this);results=Ui.col(this);results.setPadding(d(16),d(4),d(16),d(18));scroll.addView(results);summary.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        try{collector=new PageCollector(this,webHost,this);}catch(Exception e){Ui.toast(this,"网页采集组件初始化失败");finish();return;}
        summary.bringToFront();
        if(!query.isEmpty())collector.search(query);else renderResults();
        String requested=getIntent().getStringExtra("platform");if(requested!=null)try{openPage(Platform.valueOf(requested));}catch(Exception ignored){}
    }
    private interface Pick {void set(int value);}
    private AdapterView.OnItemSelectedListener selected(Pick pick){return new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int n,long id){pick.set(n);}};}
    private int d(int n){return Ui.dp(this,n);}
    private void startSearch(String word){
        String q=word.trim();if(q.isEmpty()){input.setError("请输入商品名称或具体型号");return;}if(q.length()>150){input.setError("关键词不能超过 150 个字符");return;}
        query=q;new Store(this).history(q);input.clearFocus();InputMethodManager keyboard=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);keyboard.hideSoftInputFromWindow(input.getWindowToken(),0);closePage();fingerprint="";scroll.scrollTo(0,0);collector.search(query);
    }
    private void openPage(Platform p){if(collector==null)return;browserPlatform=p;collector.open(p);summary.setVisibility(View.GONE);webHost.bringToFront();browserBar.setVisibility(View.VISIBLE);browserTitle.setText(getString(R.string.platform_webpage,p.label));}
    private void closePage(){if(collector==null)return;collector.closePage();browserPlatform=null;browserBar.setVisibility(View.GONE);summary.setVisibility(View.VISIBLE);summary.bringToFront();renderResults();}
    @Override public void changed(){if(collector==null)return;for(PageCollector.Source s:collector.sources){TextView t=sourceLabels[s.platform.ordinal()];t.setText(getString(R.string.platform_status,s.platform.label,s.status));t.setContentDescription(s.platform.label+"："+s.status+"，点击打开官方网页");}if(browserPlatform!=null){String url=collector.sources[browserPlatform.ordinal()].web.getUrl();String host=url==null?"":android.net.Uri.parse(url).getHost();browserTitle.setText(getString(R.string.platform_status,browserPlatform.label,host==null?"":host));}renderResults();}
    private void renderResults(){
        if(collector==null||results==null)return;
        List<Offer> all=collector.all();List<Offer> shown=Offer.select(all,query,filter==0?null:Platform.values()[filter-1],strict,sort);
        StringBuilder key=new StringBuilder(query).append(sort).append(filter).append(strict);for(PageCollector.Source s:collector.sources)key.append(s.status).append(s.detail);for(Offer o:shown)key.append(o.key()).append(o.cents).append(o.shop).append(o.title);
        if(key.toString().equals(fingerprint))return;fingerprint=key.toString();int oldScroll=scroll.getScrollY();results.removeAllViews();
        int active=0;for(PageCollector.Source s:collector.sources)if(!s.offers.isEmpty())active++;
        countLabel.setText(query.isEmpty()?"先登录平台，再输入关键词进行聚合搜索":"显示 "+shown.size()+" 条 / 已读取 "+all.size()+" 条 · "+active+"/4 个搜索页返回数据");
        if(!query.isEmpty()){
            LinearLayout note=Ui.card(this);note.setBackground(Ui.bg(Color.rgb(237,233,255),d(16)));TextView info=Ui.text(this,"同款候选请核对型号、容量和颜色。排序使用搜索页展示价；起价、券后条件、运费及最终结算价可能不同。",11,Ui.PURPLE,false);info.setLineSpacing(d(3),1);note.addView(info);Ui.block(results,note);
        }
        if(shown.isEmpty()){
            LinearLayout empty=Ui.card(this);empty.addView(Ui.text(this,query.isEmpty()?"先完成平台登录":all.isEmpty()?"等待真实商品结果":"没有符合当前筛选的商品",19,Ui.INK,true));Ui.gap(empty,10);empty.addView(Ui.text(this,query.isEmpty()?"点击上方平台，在本应用内的官方网页中登录。外部平台 APP 的登录状态通常不会自动共享。":all.isEmpty()?"各平台会分别加载搜索页。需要登录或验证时，点击对应平台处理，返回后自动汇总；未读到数据的平台不会生成报价。":"可切换平台或取消严格匹配查看更多候选；页面报价未必对应同一规格。",13,Ui.MUTED,false));Ui.block(results,empty);
        }
        for(Offer offer:shown)addCard(offer);
        for(PageCollector.Source s:collector.sources){
            LinearLayout status=Ui.card(this);LinearLayout line=Ui.row(this);Ui.weighted(line,Ui.text(this,s.platform.label+" · "+s.status,14,Ui.INK,true));line.addView(Ui.button(this,"打开网页",false,()->openPage(s.platform)));status.addView(line);Ui.gap(status,8);status.addView(Ui.text(this,s.detail.isEmpty()?"在官方网页登录，不读取密码，也不上传登录状态。":s.detail,12,Ui.MUTED,false));Ui.gap(status,8);TextView retry=Ui.text(this,"重新搜索此平台 ↗",12,Ui.PURPLE,true);retry.setPadding(0,d(8),0,d(8));retry.setOnClickListener(v->collector.retry(s.platform));status.addView(retry);Ui.block(results,status);
        }
        scroll.post(()->scroll.scrollTo(0,oldScroll));
    }
    private void addCard(Offer o){
        LinearLayout card=Ui.card(this);LinearLayout product=Ui.row(this);ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(Ui.bg(Color.rgb(242,240,252),d(12)));image.setContentDescription("商品图片");product.addView(image,new LinearLayout.LayoutParams(d(76),d(76)));thumbnails.load(o.image,image);
        LinearLayout text=Ui.col(this);text.setPadding(d(12),0,0,0);TextView badge=Ui.text(this,o.platform.label,11,Color.parseColor(o.platform.color),true);text.addView(badge);Ui.gap(text,6);TextView title=Ui.text(this,o.title,15,Ui.INK,true);title.setMaxLines(3);title.setEllipsize(android.text.TextUtils.TruncateAt.END);text.addView(title);Ui.weighted(product,text);card.addView(product);Ui.gap(card,13);
        card.addView(Ui.text(this,"店铺："+(o.shop.isEmpty()?"页面未展示店铺名":o.shop),12,Ui.MUTED,false));Ui.gap(card,10);LinearLayout price=Ui.row(this);Ui.weighted(price,Ui.text(this,"¥ "+Prices.format(o.cents),27,Ui.PURPLE,true));price.addView(Ui.button(this,o.url.isEmpty()?"查看来源页":"查看商品",false,()->{if(o.url.isEmpty())openPage(o.platform);else{Intent i=new Intent(this,BrowserActivity.class);i.putExtra("url",o.url);i.putExtra("platform",o.platform.name());i.putExtra("query",query);startActivity(i);}}));card.addView(price);Ui.gap(card,7);card.addView(Ui.text(this,o.priceLabel+" · "+new java.text.SimpleDateFormat("HH:mm",Locale.CHINA).format(new Date(o.observedAt))+" 读取",10,Ui.MUTED,false));Ui.block(results,card);
    }
    @Override public void onBackPressed(){if(browserPlatform!=null){PageCollector.Source s=collector.sources[browserPlatform.ordinal()];if(s.web.canGoBack())s.web.goBack();else closePage();}else super.onBackPressed();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("query",query);out.putInt("sort",sort);out.putInt("filter",filter);out.putBoolean("strict",strict);}
    @Override protected void onPause(){super.onPause();if(collector!=null)collector.pause();}
    @Override protected void onResume(){super.onResume();if(collector!=null)collector.resume();}
    @Override protected void onDestroy(){if(collector!=null){webHost.removeAllViews();collector.destroy();}thumbnails.close();super.onDestroy();}
}
