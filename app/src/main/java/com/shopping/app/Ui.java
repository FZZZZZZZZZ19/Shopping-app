package com.shopping.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

final class Ui {
    static final int INK=Color.rgb(35,38,56), MUTED=Color.rgb(123,126,147), PURPLE=Color.rgb(104,84,222), BG=Color.rgb(247,247,251);
    static int dp(Context c,float v) { return Math.round(v*c.getResources().getDisplayMetrics().density); }
    static GradientDrawable bg(int color,int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d; }
    static LinearLayout col(Context c) { LinearLayout l=new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
    static LinearLayout row(Context c) { LinearLayout l=new LinearLayout(c); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    static TextView text(Context c,String text,int size,int color,boolean bold) {
        TextView t=new TextView(c); t.setText(text); t.setTextSize(size); t.setTextColor(color); t.setIncludeFontPadding(false);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    static void gap(LinearLayout l,int height) { View v=new View(l.getContext()); l.addView(v,new LinearLayout.LayoutParams(1,dp(l.getContext(),height))); }
    static void pad(View v,int n) { int d=dp(v.getContext(),n); v.setPadding(d,d,d,d); }
    static TextView button(Context c,String title,boolean primary,Runnable action) {
        TextView v=text(c,title,14,primary?Color.WHITE:PURPLE,true); v.setGravity(Gravity.CENTER);
        v.setMinHeight(dp(c,46)); v.setPadding(dp(c,14),dp(c,12),dp(c,14),dp(c,12));
        v.setBackground(bg(primary?PURPLE:Color.rgb(240,237,255),dp(c,14))); v.setOnClickListener(w->action.run()); return v;
    }
    static LinearLayout card(Context c) { LinearLayout l=col(c); pad(l,18); l.setBackground(bg(Color.WHITE,dp(c,20))); return l; }
    static void block(LinearLayout parent,View child) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(parent.getContext(),14); parent.addView(child,p); }
    static EditText input(Context c,String hint) {
        EditText e=new EditText(c); e.setTextSize(15); e.setTextColor(INK); e.setHintTextColor(MUTED); e.setHint(hint); e.setSingleLine(true);
        e.setPadding(dp(c,14),dp(c,12),dp(c,14),dp(c,12)); e.setBackground(bg(BG,dp(c,12))); return e;
    }
    static void weighted(LinearLayout row,View v) { row.addView(v,new LinearLayout.LayoutParams(0,-2,1)); }
    static void toast(Context c,String s) { Toast.makeText(c,s,Toast.LENGTH_SHORT).show(); }
    static void external(Activity a,String url) {
        if(!Platform.isSafeUrl(url)) { toast(a,"只支持有效的 HTTPS 链接"); return; }
        try { a.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url))); } catch(Exception e) { toast(a,"未找到可打开链接的应用"); }
    }
}
