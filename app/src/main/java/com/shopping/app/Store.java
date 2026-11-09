package com.shopping.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

final class Store {
    private final SharedPreferences prefs;
    Store(Context c) { prefs=c.getSharedPreferences("shopping",Context.MODE_PRIVATE); }
    List<JSONObject> all(String key) {
        List<JSONObject> list=new ArrayList<>();
        try { JSONArray a=new JSONArray(prefs.getString(key,"[]")); for(int i=0;i<a.length();i++) list.add(a.getJSONObject(i)); } catch(Exception ignored) {}
        return list;
    }
    void save(String key,List<JSONObject> list) { JSONArray a=new JSONArray(); for(JSONObject o:list) a.put(o); prefs.edit().putString(key,a.toString()).apply(); }
    void add(String key,JSONObject item) {
        List<JSONObject> list=all(key); list.add(0,item); save(key,list);
    }
    void remove(String key,String id) { List<JSONObject> a=all(key); a.removeIf(o->o.optString("id").equals(id)); save(key,a); }
    void history(String word) {
        List<JSONObject> a=all("history"); a.removeIf(o->o.optString("word").equals(word));
        try { JSONObject o=new JSONObject(); o.put("word",word); a.add(0,o); if(a.size()>12) a=a.subList(0,12); save("history",a); } catch(Exception ignored) {}
    }
}
