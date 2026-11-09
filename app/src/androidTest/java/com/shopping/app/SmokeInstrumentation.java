package com.shopping.app;

import android.app.*;
import android.os.Bundle;
import android.os.SystemClock;
import android.content.Intent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.*;

/** A device smoke test without a third-party test runtime. Run on a clean emulator. */
public class SmokeInstrumentation extends Instrumentation {
    private int checks=0;
    private boolean aggregateOnly=false;
    @Override public void onCreate(Bundle args){super.onCreate(args);aggregateOnly=args!=null&&"true".equals(args.getString("aggregate_only"));start();}
    @Override public void onStart(){
        Bundle report=new Bundle();
        try {
            getUiAutomation();
            if(aggregateOnly){checks=AggregationDeviceChecks.run(this);report.putString("stream","PASS: 5 aggregate device checks (test fixtures only)\n");finish(Activity.RESULT_OK,report);return;}
            launch();expect("搜同款，比价格。");checks++;progress();
            nav("店铺");expect("我的店铺集");checks++;progress();
            click("＋ 收藏店铺");fill(0,"测试店铺");fill(1,"https://shop.m.taobao.com/shop/shop_index.htm?shop_id=123");click("保存");expect("测试店铺");checks++;progress();
            nav("比价");click("＋ 记录商品报价");fill(0,"测试耳机 / 黑色 / 全新");fill(1,"测试商家 A");fill(2,"100");fill(3,"6");fill(4,"10");click("保存报价");expect("¥ 96.00");expect("本组最低");checks++;progress();
            click("＋ 记录商品报价");fill(0,"测试耳机 / 黑色 / 全新");fill(1,"测试商家 B");fill(2,"88");fill(3,"0");fill(4,"0");click("保存报价");expect("¥ 88.00");expect("¥ 96.00");checks++;progress();
            List<AccessibilityNodeInfo> prices=findText("¥ ",false);if(prices.size()!=2||!prices.get(0).getText().toString().equals("¥ 88.00"))throw new AssertionError("Prices not ordered");checks++;progress();
            nav("我的");expect("1 家收藏店铺   ·   2 条报价");checks++;progress();
            launch();nav("店铺");expect("测试店铺");nav("比价");expect("¥ 88.00");checks++;progress();
            nav("我的");click("清空收藏、报价与搜索记录");click("确定");expect("0 家收藏店铺   ·   0 条报价");checks++;progress();
            nav("搜索");expect("搜同款，比价格。");
            checks+=AggregationDeviceChecks.run(this);
            report.putString("stream","PASS: "+checks+" device checks: 9 legacy data checks plus 5 aggregate DOM-to-UI checks (four platforms, shops, both sort orders, platform filter, login state). Aggregate HTML uses test fixtures only, not live accounts.\n");report.putInt("checks",checks);finish(Activity.RESULT_OK,report);
        }catch(Throwable e){report.putString("stream","FAIL after "+checks+" checks: "+e.toString()+"\n");finish(Activity.RESULT_CANCELED,report);}
    }
    private void progress(){Bundle b=new Bundle();b.putString("stream","Device check "+checks+" passed\n");sendStatus(0,b);}
    private void launch(){Intent i=new Intent();i.setClassName("com.shopping.app","com.shopping.app.MainActivity");i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivitySync(i);idle();}
    private void idle(){waitForIdleSync();SystemClock.sleep(300);}
    private List<AccessibilityNodeInfo> nodes(){AccessibilityNodeInfo r=getUiAutomation().getRootInActiveWindow();List<AccessibilityNodeInfo> a=new ArrayList<>();walk(r,a);return a;}
    private void walk(AccessibilityNodeInfo n,List<AccessibilityNodeInfo> a){if(n==null)return;a.add(n);for(int i=0;i<n.getChildCount();i++)walk(n.getChild(i),a);}
    private List<AccessibilityNodeInfo> findText(String text,boolean exact){List<AccessibilityNodeInfo> a=new ArrayList<>();for(AccessibilityNodeInfo n:nodes()){String t=n.getText()==null?"":n.getText().toString();if(exact?t.equals(text):t.contains(text))a.add(n);}return a;}
    private void expect(String text){for(int i=0;i<120;i++){if(!findText(text,true).isEmpty())return;SystemClock.sleep(250);}StringBuilder seen=new StringBuilder();for(AccessibilityNodeInfo n:nodes())if(n.getText()!=null)seen.append(n.getText()).append(" | ");throw new AssertionError("Missing: "+text+"; observed: "+seen);}
    private void click(String text){for(int i=0;i<100;i++){List<AccessibilityNodeInfo> matches=findText(text,true);if(!matches.isEmpty()){AccessibilityNodeInfo n=matches.get(0);while(n!=null&&!n.isClickable())n=n.getParent();if(n!=null){n.refresh();n.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId());if(n.performAction(AccessibilityNodeInfo.ACTION_CLICK)){idle();return;}}}SystemClock.sleep(300);}throw new AssertionError("Not clickable: "+text);}
    private void nav(String name){for(AccessibilityNodeInfo n:nodes())if(n.getContentDescription()!=null&&name.contentEquals(n.getContentDescription())){n.performAction(AccessibilityNodeInfo.ACTION_CLICK);idle();return;}throw new AssertionError("Missing nav "+name);}
    private void fill(int index,String text){List<AccessibilityNodeInfo>a=new ArrayList<>();for(AccessibilityNodeInfo n:nodes())if("android.widget.EditText".contentEquals(n.getClassName()))a.add(n);if(index>=a.size())throw new AssertionError("Missing input "+index+", have "+a.size());Bundle b=new Bundle();b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);if(!a.get(index).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b))throw new AssertionError("Cannot fill "+index);idle();}
}
