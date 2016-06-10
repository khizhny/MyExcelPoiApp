package com.khizhny.tracker;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import static com.khizhny.tracker.MainActivity.TAG;
import java.util.ArrayList;

public class MyLayer {
    private int id;
    private String name;
    private int iconId;
    private int color;
    public ArrayList<MyPoint> points;
    public boolean visible;
    private BitmapDescriptor layerIcon;
    private Drawable drawableIcon;

    public static int[] markerIcons ={
            R.drawable.icon_00,R.drawable.icon_01,R.drawable.icon_02,R.drawable.icon_03,
            R.drawable.icon_04,R.drawable.icon_05,R.drawable.icon_06,R.drawable.icon_07,
            R.drawable.icon_08,R.drawable.icon_09,R.drawable.icon_10,R.drawable.icon_11,
            R.drawable.icon_12,R.drawable.icon_13,R.drawable.icon_14,R.drawable.icon_15,
            R.drawable.icon_16,R.drawable.icon_17,R.drawable.icon_18,R.drawable.icon_19,
            R.drawable.icon_20,R.drawable.icon_21,R.drawable.icon_22,R.drawable.icon_23,
            R.drawable.icon_24,R.drawable.icon_25,R.drawable.icon_26,R.drawable.icon_27,
            R.drawable.icon_28,R.drawable.icon_29,R.drawable.icon_30,R.drawable.icon_31,
            R.drawable.icon_32,R.drawable.icon_33,R.drawable.icon_34,R.drawable.icon_35,
            R.drawable.icon_36,R.drawable.icon_37,R.drawable.icon_38,R.drawable.icon_39,
    };

    public MyLayer(int id, String name, int iconId, int color, boolean visible, Context ctx) {
        this.id = id;
        this.name = name;
        this.points = new ArrayList<>();
        this.visible=visible;
        this.color=color;
        this.iconId = iconId;
        refreshDrawableIcons(ctx);
    }

    public String toString(){
        return this.name;
    };

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId, Context ctx) {
        if (this.iconId!=iconId) {
            this.iconId = iconId;
            refreshDrawableIcons(ctx);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color, Context ctx) {
        if (this.color!=color) {
            this.color=color;
            refreshDrawableIcons(ctx);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id= id;
    }

    public BitmapDescriptor getLayerIcon() {
        return layerIcon;
    }

    public Drawable getLayerIconDrawable() {
        return drawableIcon;
    }

    private void refreshDrawableIcons(Context ctx){
        // making an icon drawable
        Log.d(TAG,"Layer "+name+" icon is refreshing.");
        Drawable drawable=ctx.getResources().getDrawable(markerIcons[iconId]);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setAlpha(255);
        drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        drawable.setBounds(0,0,drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        this.layerIcon = BitmapDescriptorFactory.fromBitmap(bitmap);
        this.drawableIcon =  new BitmapDrawable(ctx.getResources(), bitmap);
    }
}


