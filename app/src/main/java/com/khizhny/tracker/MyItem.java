package com.khizhny.tracker;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class MyItem implements ClusterItem {
    private final LatLng position;
    private final String comment;
    private final String label;
    private final MyLayer layer;
    private final int layerId;
    private int id;
    private static DecimalFormat df = new DecimalFormat("#.######");

    /**
     * Constructs new point with linking to layer object
     * @param point - LatLng coordinates
     * @param comment - comment
     * @param label - label
     * @param layer -layer object
     * @param pointId - point db id. 0 for unsaved point.
     */
    public MyItem(LatLng point, String comment, String label, MyLayer layer, int pointId) {
        this.position = point;
        this.comment=comment;
        this.label=label;
        this.layer = layer;
        if (layer!=null) {
            this.layerId = layer.getId();
        } else{
            this.layerId =0;
        }
        this.id=pointId;
    }

    /**
     * Constructs new point without linking to layer object
     * @param point - LatLng coordinates
     * @param comment - comment
     * @param label - label
     * @param layerId -layer db id
     * @param pointId - point db id. 0 for unsaved point.
     */
    public MyItem(LatLng point, String comment, String label, int layerId, int pointId) {
        this.position = point;
        this.comment=comment;
        this.label=label;
        this.layer = null;
        this.layerId=layerId;
        this.id=pointId;
    }

    public LatLng getPosition() {
        return position;
    }

    public String getComment() {
        return comment;
    }

    public String getLabel() {
        return label;
    }

    public String getLongitude(boolean inGmsFormat) {
        if (inGmsFormat) {
            return convertToGms(position.longitude);
        }else {
            df.setRoundingMode(RoundingMode.CEILING);
            return df.format(position.longitude);
        }
    }

    private String convertToGms(double d){
        double temp = d;
        MathContext getInt=  new MathContext(0,RoundingMode.FLOOR);
        BigDecimal gg = (new BigDecimal(temp,getInt)).setScale(0, RoundingMode.FLOOR);
        temp = (temp - gg.doubleValue()) * 60d;
        BigDecimal mm = new BigDecimal(temp,getInt).setScale(0, RoundingMode.FLOOR);
        temp = (temp - mm.doubleValue()) * 60d;
        BigDecimal ss = new BigDecimal(temp,getInt).setScale(1, RoundingMode.HALF_UP);
        return gg.toString()+((char)0x02DA)+" "+mm.toString()+"' "+ss.toString()+((char)0x0022);
    }

    public String getLatitude(boolean inGmsFormat) {
        if (inGmsFormat) {
            return convertToGms(position.latitude);
        }else {
            df.setRoundingMode(RoundingMode.CEILING);
            return df.format(position.latitude);
        }
    }

    public BitmapDescriptor getIcon() {
        return layer.getLayerIcon();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLayerId() {
        return layerId;
    }
}