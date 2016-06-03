package com.khizhny.tracker;

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
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class EditPointActivity extends AppCompatActivity implements View.OnClickListener{

    private ArrayList<MyLayer> layers;
    private Spinner layerSpinner;
    private MyItem point;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_point);
        TextView latView = (TextView) findViewById(R.id.new_point_lat);
        TextView lonView = (TextView) findViewById(R.id.new_point_lon);
        EditText descriptionView = (EditText) findViewById(R.id.new_point_description);
        EditText labelView = (EditText) findViewById(R.id.new_point_label);

        layerSpinner = (Spinner)  findViewById(R.id.new_point_layer);
        // reading all layers from db to fill spinner
        DB db = DB.getInstance(getApplicationContext());
        db.open();
        layers= db.getLayers(null,false,true);
        db.close();
        if (layers.size()>0) {
            ArrayAdapter spinnerArrayAdapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item, layers);
            layerSpinner.setAdapter(spinnerArrayAdapter);
            layerSpinner.setSelection(0);
        } else {
            Toast.makeText(this,"No layers found :(", Toast.LENGTH_SHORT).show();
            this.finish();
        }
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
            point = new MyItem(new LatLng(lat,lon),"","",null,0);
        }

        if (latView != null) {
            latView.setText(point.getLatitude());
        }
        if (lonView != null) {
            lonView.setText(point.getLongitude());
        }

        Button saveButton = (Button) findViewById(R.id.new_point_save);
        if (saveButton != null) {
            saveButton.setOnClickListener(this);
        }

        Button cancelButton = (Button) findViewById(R.id.new_point_cancel);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(this);
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
            case R.id.new_point_cancel:
                this.finish();
                break;
            case R.id.new_point_save:
                layer = (MyLayer) layerSpinner.getSelectedItem();
                comment = ((EditText)findViewById(R.id.new_point_description)).getText().toString();
                label = ((EditText)findViewById(R.id.new_point_label)).getText().toString();
                MyItem new_point = new MyItem(point.getPosition(),comment,label,layer,0);
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
        }
    }
}
