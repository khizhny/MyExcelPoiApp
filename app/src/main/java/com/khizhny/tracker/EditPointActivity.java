package com.khizhny.tracker;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class EditPointActivity extends AppCompatActivity implements View.OnClickListener{

    private ArrayList<MyLayer> layers;
    private Spinner layerSpinner;
    private MyPoint point;
    private TextView latView;
    private TextView lonView;
    private Boolean useGmsFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_point);
        latView = (TextView) findViewById(R.id.new_point_lat);
        lonView = (TextView) findViewById(R.id.new_point_lon);
        EditText descriptionView = (EditText) findViewById(R.id.new_point_description);
        EditText labelView = (EditText) findViewById(R.id.new_point_label);
        SharedPreferences sp = getApplication().getSharedPreferences("settings",0);
        useGmsFormat=sp.getBoolean("useGmsFormat",true);

        layerSpinner = (Spinner)  findViewById(R.id.new_point_layer);
        // reading all layers from db to fill spinner
        DB db = DB.getInstance(getApplicationContext());
        db.open();
        layers= db.getLayers(null,false,true);
        if (layers.size()==0) {
            float[] randomHsv= {(float) (360*Math.random()),1,1};
            db.saveLayer(new MyLayer(0, "My layer", 0, Color.HSVToColor(randomHsv), true, this));
            layers= db.getLayers(null,false,true);
        }
        ArrayAdapter spinnerArrayAdapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item, layers);
        layerSpinner.setAdapter(spinnerArrayAdapter);
        layerSpinner.setSelection(0);

        db.close();
        // spinner change handler
        layerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ImageView icon = (ImageView) findViewById(R.id.new_point_icon);
                if (icon != null) {
                    icon.setImageDrawable(layers.get(position).getLayerIconDrawable());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });



        int point_id=getIntent().getIntExtra("point_id",0); // id of the point to edit
        if (point_id>0) { // if id exists reading full point info
            db = DB.getInstance(getApplicationContext());
            db.open();
            point = db.getPoint(point_id);
            MyLayer oldLayer = db.getLayer(point.getLayerId(),false);
            int ind=0;
            for (MyLayer l : layers) {
                if (l.getId()==oldLayer.getId()){
                    layerSpinner.setSelection(ind);
                }
                ind++;
            }
            db.close();
            if (descriptionView != null) {
                descriptionView.setText(point.getComment());
            }
            if (labelView != null) {
                labelView.setText(point.getLabel());
            }
        } else { // or creating a new point object
            Double lat = getIntent().getDoubleExtra("lat",0);
            Double lon = getIntent().getDoubleExtra("lon",0);
            point = new MyPoint(new LatLng(lat,lon),"","",null,0);
        }

        if (latView != null) {
            if(useGmsFormat) {
                latView.setText(point.getLatitude(true));
            } else {
                latView.setText(point.getLatitude(false));
            }
            latView.setOnClickListener(this);
        }
        if (lonView != null) {
            if(useGmsFormat) {
                lonView.setText(point.getLongitude(true));
            }else {
                lonView.setText(point.getLongitude(false));
            }
            lonView.setOnClickListener(this);
        }

        Button saveButton = (Button) findViewById(R.id.new_point_save);
        if (saveButton != null) {
            saveButton.setOnClickListener(this);
        }

        Button cancelButton = (Button) findViewById(R.id.new_point_cancel);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(this);
        }

        Button backCancelButton = (Button) findViewById(R.id.new_point_back_cancel);
        if (backCancelButton != null) {
            backCancelButton.setOnClickListener(this);
        }

        ImageButton deleteButton = (ImageButton) findViewById(R.id.new_point_delete);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(this);
        }


    }

    @Override
    public void onClick(View v) {
        DB db;
        MyLayer layer;
        String comment,label;
        switch (v.getId()){
            case R.id.new_point_back_cancel:
            case R.id.new_point_cancel:
                this.finish();
                break;
            case R.id.new_point_save:
                layer = (MyLayer) layerSpinner.getSelectedItem();
                comment = ((EditText)findViewById(R.id.new_point_description)).getText().toString();
                label = ((EditText)findViewById(R.id.new_point_label)).getText().toString();
                MyPoint new_point = new MyPoint(point.getPosition(),comment,label,layer,0);
                db = DB.getInstance(getApplicationContext());
                db.open();
                db.deletePoint(point.getId());
                db.addOrEditPoint(layer, new_point);
                db.close();
                MainActivity.isRefreshNeeded=true;
                this.finish();
                break;
            case R.id.new_point_delete:
                if (point.getId()!=0){  // existing point is deleting
                    db = DB.getInstance(getApplicationContext());
                    db.open();
                    db.deletePoint(point.getId());
                    db.close();
                }
                MainActivity.isRefreshNeeded=true;
                this.finish();
                break;
            case R.id.new_point_lat:
            case R.id.new_point_lon:
                useGmsFormat=!useGmsFormat;
                latView.setText(point.getLatitude(useGmsFormat));
                lonView.setText(point.getLongitude(useGmsFormat));
                SharedPreferences.Editor spe = getApplication().getSharedPreferences("settings",0).edit();
                spe.putBoolean("useGmsFormat",useGmsFormat);
                spe.commit();
                break;
        }
    }
}
