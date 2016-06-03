package com.khizhny.tracker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

import static com.khizhny.tracker.MyLayer.markerIcons;

public class EditLayerActivity extends AppCompatActivity  {
    private MyLayer layer;
    private int newIconId=0;
    private ImageView selectedIconView;

    private class CustomAdapter extends BaseAdapter {

        private LayoutInflater inflater;

        public CustomAdapter(Context context) {
            this.inflater = ( LayoutInflater )context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(final int position, final View convertView, ViewGroup parent) {
            View rowView;
            rowView = inflater.inflate(R.layout.activity_edit_later_row, null);
            ImageView icon;
            icon=(ImageView) rowView.findViewById(R.id.icon_image);
            icon.setImageResource(markerIcons[position]);
            icon.setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY);
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    newIconId=position;
                    layer.setIconId(newIconId,getApplicationContext());
                    selectedIconView.setImageDrawable(layer.getLayerIconDrawable());
                }
            });
            return rowView;
        }

        @Override
        public int getCount() {
            return markerIcons.length;
        }

        @Override
        public Object getItem(int position) {
            return markerIcons[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_layer);
        int layer_id = getIntent().getExtras().getInt("layer_id");
        DB db = DB.getInstance(this);
        db.open();
        layer=db.getLayer(layer_id,false);
        db.close();

        GridView gvMain = (GridView) findViewById(R.id.gridView);
        CustomAdapter adapter = new CustomAdapter(this);
        if (gvMain != null) {
            gvMain.setAdapter(adapter);
            gvMain.setNumColumns(7);
        }


        selectedIconView = (ImageView)  findViewById(R.id.selected_layer_icon);
        newIconId = layer.getIconId();
        selectedIconView.setImageResource(markerIcons[newIconId]);
        SeekBar colorSeekBar = (SeekBar) findViewById(R.id.icon_color_seek_bar);
        float[] layerHsv={0,1,1};
        Color.colorToHSV(layer.getColor(),layerHsv);
        colorSeekBar.setProgress((int)layerHsv[0]);

        colorSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private float[] hsv={120,1,1};
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                hsv[0]=progress;
                selectedIconView.setColorFilter(Color.HSVToColor(hsv));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                layer.setColor(Color.HSVToColor(hsv),getApplicationContext());
            }
        });

        ImageButton deleteButtonView = (ImageButton) findViewById(R.id.delete_layer_button);
        if (deleteButtonView != null) {
            deleteButtonView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DB db = DB.getInstance(getApplicationContext());
                    db.open();
                    db.deleteLayer(layer.getId());
                    db.close();
                    EditLayerActivity.this.finish();
                }
            });
        }

        ImageButton saveLayerButtonView = (ImageButton) findViewById(R.id.save_layer_button);
        if (saveLayerButtonView != null) {
            saveLayerButtonView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newName= ((EditText) findViewById(R.id.edit_layer_name)).getText().toString();
                    layer.setName(newName);
                    layer.setIconId(newIconId,getApplicationContext());
                    DB db = DB.getInstance(getApplicationContext());
                    db.open();
                    db.editLayerInfo(layer);
                    db.close();
                    EditLayerActivity.this.finish();
                }
            });
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        ((EditText) findViewById(R.id.edit_layer_name)).setText(layer.getName());
    }

}
