package com.shopping.app;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import org.json.JSONObject;
import java.util.*;

public class MainActivity extends Activity {
    private Store store;
    private LinearLayout root, body, nav;
    private int page=0;
    private String query="", priceFilter="";
    private int shopFilter=-1;
    private String draftUrl="";
    private int draftPlatform=0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); store=new Store(this);
        if(state!=null) { page=state.getInt("page"); query=state.getString("query",""); priceFilter=state.getString("filter",""); shopFilter=state.getInt("shopFilter",-1); }
        render();
        if(state==null && getIntent().getBooleanExtra("quote",false)) { query=getIntent().getStringExtra("query");if(query==null)query="";draftUrl=getIntent().getStringExtra("url");draftPlatform=getIntent().getIntExtra("platformIndex",0);addPrice(null); }
        if(state==null && Intent.ACTION_SEND.equals(getIntent().getAction())) {
            String shared=getIntent().getStringExtra(Intent.EXTRA_TEXT);
            if(shared!=null) { java.util.regex.Matcher m=java.util.regex.Pattern.compile("https://[^\\s<>\"，。]+",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(shared); if(m.find()) addShop("",m.group()); else { query=shared.length()>100?shared.substring(0,100):shared; render(); } }
        }
    }
    @Override protected void onSaveInstanceState(Bundle b) { super.onSaveInstanceState(b); b.putInt("page",page); b.putString("query",query); b.putString("filter",priceFilter); b.putInt("shopFilter",shopFilter); }
    @Override protected void onResume() { super.onResume(); if(store!=null)render(); }

    private void render() {
        root=Ui.col(this); root.setBackgroundColor(Ui.BG); setContentView(root);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        body=Ui.col(this); body.setPadding(d(22),d(22),d(22),d(20)); scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        switch(page) { case 1: shops(); break; case 2: prices(); break; case 3: about(); break; default: home(); }
        nav=Ui.row(this); nav.setPadding(d(10),d(10),d(10),d(10)); nav.setBackgroundColor(Color.WHITE);
        String[] labels={"⌕\n搜索","▤\n店铺","⇄\n比价","◉\n我的"};
        for(int i=0;i<labels.length;i++) { final int dest=i; TextView t=Ui.text(this,labels[i],14,page==i?Ui.PURPLE:Ui.MUTED,page==i); t.setGravity(Gravity.CENTER); t.setLineSpacing(d(5),1); t.setPadding(0,d(4),0,d(4)); t.setContentDescription(new String[]{"搜索","店铺","比价","我的"}[i]); t.setOnClickListener(v->{page=dest;render();}); Ui.weighted(nav,t); }
        root.addView(nav);
    }
    private int d(int n) { return Ui.dp(this,n); }
    private void title(String eyebrow,String heading,String subtitle) {
        Ui.block(body,Ui.text(this,eyebrow,11,Ui.PURPLE,true));
        body.addView(Ui.text(this,heading,29,Ui.INK,true)); Ui.gap(body,8);
        body.addView(Ui.text(this,subtitle,13,Ui.MUTED,false)); Ui.gap(body,24);
    }
    private void section(String title,String action,Runnable callback) {
        LinearLayout row=Ui.row(this); Ui.weighted(row,Ui.text(this,title,18,Ui.INK,true));
        if(action!=null) { TextView t=Ui.text(this,action,12,Ui.PURPLE,true); t.setPadding(d(12),d(10),0,d(10)); t.setOnClickListener(v->callback.run()); row.addView(t); }
        Ui.block(body,row);
    }
    private void home() {
        title("SHOPPING  /  四平台一起比","搜同款，比价格。","一次搜索，汇总商品、平台、店铺与价格");
        LinearLayout search=Ui.card(this);search.setPadding(d(12),d(10),d(10),d(10));LinearLayout row=Ui.row(this);
        EditText input=Ui.input(this,"商品名称、型号或规格");input.setText(query);input.setBackgroundColor(Color.TRANSPARENT);input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();}public void afterTextChanged(android.text.Editable e){}});
        Ui.weighted(row,input);row.addView(Ui.button(this,"搜索",true,()->chooseSearch(input.getText().toString())));search.addView(row);Ui.block(body,search);
        input.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_SEARCH){chooseSearch(input.getText().toString());return true;}return false;});
        LinearLayout explanation=Ui.card(this);explanation.setBackground(Ui.bg(Color.rgb(236,232,255),d(20)));
        explanation.addView(Ui.text(this,"商品、店铺和价格，放在一张列表",19,Ui.PURPLE,true));Ui.gap(explanation,10);
        explanation.addView(Ui.text(this,"搜索后并行打开四个平台的官方搜索页，读取页面已展示的商品。支持平台筛选、规格关键词匹配和价格升降序。",13,Ui.PURPLE,false));Ui.block(body,explanation);
        section("平台登录","管理登录",()->login(null));
        for(Platform p:Platform.values()){
            LinearLayout card=Ui.card(this);LinearLayout line=Ui.row(this);TextView mark=Ui.text(this,p.mark,20,Color.WHITE,true);mark.setGravity(Gravity.CENTER);mark.setBackground(Ui.bg(Color.parseColor(p.color),d(12)));line.addView(mark,new LinearLayout.LayoutParams(d(42),d(42)));
            LinearLayout label=Ui.col(this);label.setPadding(d(12),0,0,0);label.addView(Ui.text(this,p.label,16,Ui.INK,true));Ui.gap(label,5);label.addView(Ui.text(this,"官方网页登录 · 登录状态保留在本机",10,Ui.MUTED,false));Ui.weighted(line,label);line.addView(Ui.button(this,"打开登录",false,()->login(p)));card.addView(line);Ui.block(body,card);
        }
        Ui.block(body,Ui.text(this,"请在本应用内登录平台。需要验证码时由你完成；未展示或无法识别的数据不会生成报价。搜索页报价需核对规格、优惠及运费。",12,Ui.MUTED,false));
        List<JSONObject> history=store.all("history");if(!history.isEmpty()){
            section("最近搜索","清空",()->confirm("清空搜索历史？",()->{store.save("history",new ArrayList<>());render();}));
            for(JSONObject o:history){String word=o.optString("word");TextView h=Ui.text(this,"↺   "+word+"  ↗",14,Ui.INK,false);h.setPadding(d(2),d(13),d(2),d(13));h.setOnClickListener(v->{query=word;chooseSearch(word);});body.addView(h);}
        }
    }
    private void login(Platform p){Intent intent=new Intent(this,SearchActivity.class);if(p!=null)intent.putExtra("platform",p.name());startActivity(intent);}
    private void chooseSearch(String word) {
        String q=word.trim();if(q.isEmpty()){Ui.toast(this,"先输入想找的商品");return;}if(q.length()>150){Ui.toast(this,"关键词不能超过 150 个字符");return;}
        query=q;store.history(q);Intent intent=new Intent(this,SearchActivity.class);intent.putExtra("query",q);startActivity(intent);
    }
    private void search(Platform p,String word) {
        String q=word.trim(); if(q.isEmpty()) { open(p.home,p,"");return; }
        query=q; store.history(q); open(p.search(q),p,q);
    }
    private void open(String url,Platform p,String word) {
        Intent i=new Intent(this,BrowserActivity.class); i.putExtra("url",url); i.putExtra("platform",p.name()); i.putExtra("query",word); startActivity(i);
    }
    private void shops() {
        title("YOUR STORES  /  常逛常新","我的店铺集","把常逛的店，放在一起");
        Ui.block(body,Ui.button(this,"＋ 收藏店铺",true,()->addShop("","")));
        LinearLayout filters=Ui.row(this);
        String[] labels={"全部","淘宝","拼多多","京东","天猫"};
        for(int i=0;i<labels.length;i++) { final int n=i-1; TextView t=Ui.text(this,labels[i],12,shopFilter==n?Ui.PURPLE:Ui.MUTED,shopFilter==n); t.setGravity(Gravity.CENTER); t.setPadding(0,d(12),0,d(12)); t.setOnClickListener(v->{shopFilter=n;render();}); Ui.weighted(filters,t); } Ui.block(body,filters);
        List<JSONObject> list=store.all("shops"); int count=0;
        for(JSONObject o:list) {
            Platform p=parsePlatform(o); if(shopFilter>=0&&p.ordinal()!=shopFilter)continue; count++;
            LinearLayout card=Ui.card(this); card.addView(Ui.text(this,p.label+"  /  收藏店铺",11,Color.parseColor(p.color),true)); Ui.gap(card,10); card.addView(Ui.text(this,o.optString("name"),19,Ui.INK,true)); Ui.gap(card,6);
            TextView link=Ui.text(this,o.optString("url"),11,Ui.MUTED,false); link.setMaxLines(1); link.setEllipsize(android.text.TextUtils.TruncateAt.END); card.addView(link); Ui.gap(card,15);
            LinearLayout actions=Ui.row(this); Ui.weighted(actions,Ui.button(this,"进入店铺 ↗",true,()->open(o.optString("url"),p,""))); TextView more=Ui.button(this,"管理",false,()->new AlertDialog.Builder(this).setTitle(o.optString("name")).setItems(new String[]{"编辑店铺","移除收藏"},(a,n)->{if(n==0)shopDialog(o,o.optString("name"),o.optString("url"));else confirm("移除这家店铺？",()->{store.remove("shops",o.optString("id"));render();});}).show()); LinearLayout.LayoutParams ml=new LinearLayout.LayoutParams(-2,-2); ml.leftMargin=d(10); actions.addView(more,ml); card.addView(actions); Ui.block(body,card);
        }
        if(count==0)empty("▤","留一个位置给好店","支持四大平台店铺链接，也可从其他应用分享链接收藏。");
    }
    private Platform parsePlatform(JSONObject o) { try{return Platform.valueOf(o.optString("platform"));}catch(Exception e){return Platform.TAOBAO;} }
    private void addShop(String name,String url) { shopDialog(null,name,url); }
    private void shopDialog(JSONObject existing,String name,String url) {
        LinearLayout form=Ui.col(this); Ui.pad(form,22); EditText n=Ui.input(this,"店铺名称"); n.setText(name); Ui.block(form,n); EditText u=Ui.input(this,"粘贴店铺 HTTPS 链接"); u.setText(url); u.setInputType(17); Ui.block(form,u);
        Spinner platform=spinner(); Platform detected=Platform.detect(url); platform.setSelection(existing!=null?parsePlatform(existing).ordinal():detected!=null?detected.ordinal():0); Ui.block(form,platform);
        form.addView(Ui.text(this,"请自行确认是品牌官方店或可信商家。",12,Ui.MUTED,false));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?"收藏店铺":"编辑店铺").setView(form).setPositiveButton("保存",null).setNegativeButton("取消",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{
            String nn=n.getText().toString().trim(), uu=u.getText().toString().trim();
            if(nn.isEmpty()){n.setError("请输入店铺名称");return;}
            if(!Platform.isSafeUrl(uu)){u.setError("请输入有效的 HTTPS 链接");return;}
            Platform actual=Platform.detect(uu); if(actual==null){u.setError("请使用淘宝、拼多多、京东或天猫链接");return;}
            if(actual.ordinal()!=platform.getSelectedItemPosition()){u.setError("链接与选择的平台不一致");return;}
            List<JSONObject> shops=store.all("shops"); for(JSONObject old:shops)if(old.optString("url").equals(uu)&&(existing==null||!old.optString("id").equals(existing.optString("id")))){u.setError("该链接已收藏");return;}
            try { JSONObject item=new JSONObject(); item.put("id",existing==null?UUID.randomUUID().toString():existing.optString("id"));item.put("name",nn); item.put("url",uu); item.put("platform",Platform.values()[platform.getSelectedItemPosition()].name()); if(existing!=null)store.remove("shops",existing.optString("id"));store.add("shops",item);dialog.dismiss();page=1;render(); }catch(Exception e){Ui.toast(this,"保存失败，请重试");}
        })); dialog.show();
    }
    private Spinner spinner() { Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Arrays.stream(Platform.values()).map(p->p.label).toArray(String[]::new)));return s; }
    private void prices() {
        title("PRICE NOTEBOOK  /  花得明白","我的比价本","记录真实报价，找到适合自己的那一款");
        Ui.block(body,Ui.button(this,"＋ 记录商品报价",true,()->addPrice(null)));
        List<JSONObject> all=store.all("prices"); LinkedHashSet<String> groups=new LinkedHashSet<>();for(JSONObject o:all)groups.add(o.optString("group"));
        LinearLayout select=Ui.card(this); select.addView(Ui.text(this,"比较的商品 / 规格",11,Ui.MUTED,true)); Ui.gap(select,8);
        List<String> options=new ArrayList<>();options.add("全部报价（各组内按到手价排序）");options.addAll(groups); Spinner group=new Spinner(this);group.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options));
        int selected=priceFilter.isEmpty()?0:options.indexOf(priceFilter); group.setSelection(Math.max(0,selected));select.addView(group);Ui.block(body,select);
        group.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> a){} public void onItemSelected(AdapterView<?> a,View v,int pos,long id){String next=pos==0?"":options.get(pos);if(!next.equals(priceFilter)){priceFilter=next;render();}}});
        body.addView(Ui.text(this,"手动记录 · 非平台实时抓取 · 请核对规格与优惠门槛",11,Ui.MUTED,false));Ui.gap(body,18);
        List<JSONObject> list=new ArrayList<>();for(JSONObject o:all)if(priceFilter.isEmpty()||o.optString("group").equals(priceFilter))list.add(o);
        list.sort(Comparator.comparing((JSONObject o)->o.optString("group")).thenComparingLong(o->o.optLong("total")));
        if(list.isEmpty()){empty("⇄","第一笔好价，从这里开始","在平台查看价格后，记录相同商品、规格与到手价。系统会在同一分组内排序。");return;}
        String last="";
        for(JSONObject o:list){
            String g=o.optString("group"); boolean first=!g.equals(last);if(first){section(g,null,null);last=g;}
            Platform p=parsePlatform(o);LinearLayout c=Ui.card(this);LinearLayout top=Ui.row(this);Ui.weighted(top,Ui.text(this,p.label+" · "+o.optString("shop","未填写店铺"),12,Color.parseColor(p.color),true));if(first)top.addView(Ui.text(this,"本组最低",11,Ui.PURPLE,true));c.addView(top);Ui.gap(c,12);
            c.addView(Ui.text(this,"¥ "+Prices.format(o.optLong("total")),29,Ui.INK,true));Ui.gap(c,8);
            c.addView(Ui.text(this,"商品 ¥"+Prices.format(o.optLong("price"))+" + 运费 ¥"+Prices.format(o.optLong("shipping"))+" − 优惠 ¥"+Prices.format(o.optLong("discount")),11,Ui.MUTED,false));Ui.gap(c,8);
            c.addView(Ui.text(this,"记录于 "+new java.text.SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(new Date(o.optLong("at"))),11,Ui.MUTED,false));Ui.gap(c,14);
            LinearLayout actions=Ui.row(this);if(!o.optString("url").isEmpty())Ui.weighted(actions,Ui.button(this,"查看商品 ↗",true,()->open(o.optString("url"),p,"")));TextView manage=Ui.button(this,"编辑",false,()->addPrice(o));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=d(8);actions.addView(manage,lp);TextView del=Ui.button(this,"删除",false,()->confirm("删除这条报价？",()->{store.remove("prices",o.optString("id"));render();}));LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(-2,-2);dl.leftMargin=d(8);actions.addView(del,dl);c.addView(actions);Ui.block(body,c);
        }
    }
    private void addPrice(JSONObject existing) {
        LinearLayout form=Ui.col(this);Ui.pad(form,22);ScrollView scroll=new ScrollView(this);scroll.addView(form);
        EditText group=field(form,"商品名称 + 相同规格（作为比价分组）",existing==null?query:existing.optString("group"));
        form.addView(Ui.text(this,"例如：iPhone 16 / 128GB / 黑色 / 全新",11,Ui.MUTED,false));Ui.gap(form,12);
        Spinner p=spinner();p.setSelection(existing!=null?parsePlatform(existing).ordinal():draftPlatform);Ui.block(form,p);
        EditText shop=field(form,"店铺名称（可选）",existing==null?"":existing.optString("shop"));
        EditText price=money(form,"商品价格（元）",existing,"price",false), shipping=money(form,"运费（元）",existing,"shipping",true), discount=money(form,"优惠金额（元）",existing,"discount",true);
        EditText url=field(form,"商品 HTTPS 链接（可选）",existing==null?draftUrl:existing.optString("url"));url.setInputType(17);draftUrl="";draftPlatform=0;
        form.addView(Ui.text(this,"到手价 = 商品价格 + 运费 − 优惠。请按实际可用优惠填写。",12,Ui.MUTED,false));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?"记录报价":"编辑报价").setView(scroll).setPositiveButton("保存报价",null).setNegativeButton("取消",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{
            if(group.getText().toString().trim().isEmpty()){group.setError("请填写商品名称与规格");return;}
            String link=url.getText().toString().trim();if(!link.isEmpty()&&!Platform.isSafeUrl(link)){url.setError("请输入有效的 HTTPS 链接");return;}
            try {
                long a=Prices.cents(price.getText().toString()),b=Prices.cents(shipping.getText().toString()),c=Prices.cents(discount.getText().toString());long total=Prices.total(a,b,c);
                JSONObject item=new JSONObject();item.put("id",existing==null?UUID.randomUUID().toString():existing.optString("id"));item.put("group",group.getText().toString().trim());item.put("platform",Platform.values()[p.getSelectedItemPosition()].name());item.put("shop",shop.getText().toString().trim().isEmpty()?"未填写店铺":shop.getText().toString().trim());item.put("price",a);item.put("shipping",b);item.put("discount",c);item.put("total",total);item.put("url",link);item.put("at",System.currentTimeMillis());
                if(existing!=null)store.remove("prices",existing.optString("id"));store.add("prices",item);dialog.dismiss();page=2;priceFilter=item.optString("group");render();
            }catch(Exception e){Ui.toast(this,e instanceof IllegalArgumentException&&!(e instanceof NumberFormatException)?e.getMessage():"请填写有效金额，最多两位小数");}
        }));dialog.show();
    }
    private EditText field(LinearLayout parent,String hint,String value) { EditText e=Ui.input(this,hint);e.setText(value);Ui.block(parent,e);return e; }
    private EditText money(LinearLayout parent,String hint,JSONObject item,String key,boolean zero) {
        parent.addView(Ui.text(this,hint,12,Ui.MUTED,false));Ui.gap(parent,6);EditText e=field(parent,"0.00",item==null?(zero?"0":""):Prices.format(item.optLong(key)));e.setInputType(8194);return e;
    }
    private void empty(String icon,String title,String detail) { LinearLayout c=Ui.card(this);Ui.gap(c,20);TextView mark=Ui.text(this,icon,42,Ui.PURPLE,false);mark.setGravity(Gravity.CENTER);c.addView(mark);Ui.gap(c,18);TextView t=Ui.text(this,title,18,Ui.INK,true);t.setGravity(Gravity.CENTER);c.addView(t);Ui.gap(c,10);TextView desc=Ui.text(this,detail,13,Ui.MUTED,false);desc.setGravity(Gravity.CENTER);desc.setLineSpacing(d(4),1);c.addView(desc);Ui.gap(c,20);Ui.block(body,c); }
    private void about() {
        title("SHOPPING APP  /  简单一点","买得更从容","你的跨平台购物小助手  ·  v1.1.0");
        LinearLayout stats=Ui.card(this);stats.addView(Ui.text(this,store.all("shops").size()+" 家收藏店铺   ·   "+store.all("prices").size()+" 条报价",18,Ui.INK,true));Ui.block(body,stats);
        info("搜索与价格来源","1.1 聚合搜索会打开四个平台的官方搜索页，读取页面已展示的商品标题、价格、店铺和图片，并在同一列表中筛选和排序。请在本应用内的官方网页完成登录，外部平台 APP 的登录状态通常不会共享。\n\n没有接入平台授权价格 API。页面布局不兼容、验证码、网络限制或登录要求会导致部分平台没有结果；不会补造缺失报价。店铺未展示时明确标记未知。\n\n搜索匹配只能筛选同款候选，不能保证 SKU 一致。排序使用页面展示价，起价、券后条件、运费、地区和最终结算可能不同。旧版手动比价本保留在“比价”页，与网页采集结果分别显示。");
        Ui.block(body,Ui.button(this,"管理四个平台登录",false,()->login(null)));
        info("隐私与本地数据","店铺、报价和搜索历史保存在本机。没有自建服务器或统计 SDK。平台网页可能使用其自身 Cookie；账号、支付和订单操作由平台处理。清除应用数据或卸载会移除本地记录。");
        Ui.block(body,Ui.button(this,"导出我的收藏与报价（JSON）",false,this::exportData));
        Ui.block(body,Ui.button(this,"导入 JSON 备份",false,this::importData));
        Ui.block(body,Ui.button(this,"清除平台网页登录状态",false,()->confirm("将退出内置网页中的平台登录状态，是否继续？",()->{android.webkit.CookieManager.getInstance().removeAllCookies(null);android.webkit.CookieManager.getInstance().flush();android.webkit.WebStorage.getInstance().deleteAllData();Ui.toast(this,"已清除网页 Cookie 与存储");})));
        Ui.block(body,Ui.button(this,"清空收藏、报价与搜索记录",false,()->confirm("此操作不可恢复。建议先导出备份。确定清空？",()->{store.save("shops",new ArrayList<>());store.save("prices",new ArrayList<>());store.save("history",new ArrayList<>());priceFilter="";render();})));
        TextView footer=Ui.text(this,"独立购物工具 · 与所列平台无隶属关系",11,Ui.MUTED,false);footer.setGravity(Gravity.CENTER);body.addView(footer);
    }
    private void info(String title,String content) { LinearLayout card=Ui.card(this);card.addView(Ui.text(this,title,16,Ui.INK,true));Ui.gap(card,12);TextView text=Ui.text(this,content,13,Ui.MUTED,false);text.setLineSpacing(d(5),1);card.addView(text);Ui.block(body,card); }
    private void confirm(String message,Runnable action) {new AlertDialog.Builder(this).setMessage(message).setPositiveButton("确定",(a,n)->action.run()).setNegativeButton("取消",null).show();}
    private void exportData(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"shopping-backup-"+new java.text.SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(new Date())+".json");try{startActivityForResult(i,10);}catch(Exception e){Ui.toast(this,"未找到文件管理器");}}
    private void importData(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(i,11);}catch(Exception e){Ui.toast(this,"未找到文件管理器");}}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;
        try{
            if(req==10){JSONObject backup=new JSONObject();backup.put("version",1);for(String key:new String[]{"shops","prices","history"}){org.json.JSONArray a=new org.json.JSONArray();for(JSONObject o:store.all(key))a.put(o);backup.put(key,a);}try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(backup.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}Ui.toast(this,"备份已导出");}
            if(req==11){byte[] bytes;try(java.io.InputStream in=getContentResolver().openInputStream(data.getData());java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream()){byte[] chunk=new byte[8192];int n;while((n=in.read(chunk))!=-1){buffer.write(chunk,0,n);if(buffer.size()>2*1024*1024)throw new Exception();}bytes=buffer.toByteArray();}JSONObject backup=new JSONObject(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));if(backup.getInt("version")!=1)throw new Exception();Map<String,List<JSONObject>> parsed=new HashMap<>();
                for(String key:new String[]{"shops","prices","history"}){org.json.JSONArray a=backup.getJSONArray(key);if(a.length()>5000)throw new Exception();List<JSONObject> list=new ArrayList<>();for(int j=0;j<a.length();j++){JSONObject o=a.getJSONObject(j);validateBackup(key,o);list.add(o);}parsed.put(key,list);}
                confirm("导入将替换本机收藏、报价与历史记录，是否继续？",()->{for(String key:parsed.keySet())store.save(key,parsed.get(key));priceFilter="";render();Ui.toast(this,"备份已导入");});}
        }catch(Exception e){Ui.toast(this,req==10?"导出失败，请检查文件位置":"无效的 Shopping APP 备份（最大 2MB）");}
    }
    private void validateBackup(String key,JSONObject o)throws Exception{
        if(key.equals("history")){if(o.getString("word").trim().isEmpty()||o.getString("word").length()>1000)throw new Exception();return;}
        if(o.getString("id").trim().isEmpty())throw new Exception();Platform.valueOf(o.getString("platform"));String url=o.getString("url");
        if(key.equals("shops")){if(o.getString("name").trim().isEmpty()||!Platform.isSafeUrl(url)||Platform.detect(url)!=Platform.valueOf(o.getString("platform")))throw new Exception();}
        else{if(o.getString("group").trim().isEmpty()||(!url.isEmpty()&&!Platform.isSafeUrl(url)))throw new Exception();for(String k:new String[]{"price","shipping","discount"})Prices.cents(Prices.format(o.getLong(k)));if(Prices.total(o.getLong("price"),o.getLong("shipping"),o.getLong("discount"))!=o.getLong("total"))throw new Exception();if(o.getLong("at")<0)throw new Exception();}
    }
}
