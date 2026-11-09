package com.shopping.app;

import android.graphics.*;
import android.os.*;
import android.widget.ImageView;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

final class ThumbnailLoader {
    private final ExecutorService pool=Executors.newFixedThreadPool(2);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Map<String,Bitmap> cache=Collections.synchronizedMap(new LinkedHashMap<String,Bitmap>(32,0.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<String,Bitmap> entry){return size()>32;}});
    private volatile boolean closed=false;
    void load(String url,ImageView target){
        target.setTag(url);if(url.isEmpty()||!Platform.isSafeUrl(url))return;Bitmap hit=cache.get(url);if(hit!=null){target.setImageBitmap(hit);return;}
        pool.execute(()->{HttpURLConnection connection=null;try{
            connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(7000);connection.setReadTimeout(7000);connection.setInstanceFollowRedirects(false);if(connection.getResponseCode()!=200)return;
            byte[] data;try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>1024*1024)return;}data=out.toByteArray();}
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,options);options.inSampleSize=1;while(options.outWidth/options.inSampleSize>256||options.outHeight/options.inSampleSize>256)options.inSampleSize*=2;options.inJustDecodeBounds=false;Bitmap bitmap=BitmapFactory.decodeByteArray(data,0,data.length,options);if(bitmap!=null&&!closed){cache.put(url,bitmap);handler.post(()->{if(!closed&&url.equals(target.getTag()))target.setImageBitmap(bitmap);});}
        }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}});
    }
    void close(){closed=true;pool.shutdownNow();handler.removeCallbacksAndMessages(null);cache.clear();}
}
