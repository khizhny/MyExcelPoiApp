package com.khizhny.tracker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import static com.khizhny.tracker.MainActivity.TAG;

import java.util.List;

public class LayersActivity extends AppCompatActivity implements View.OnClickListener {
    private List<MyLayer> layers;
    private boolean changedByUser;
    private LayerListAdapter layerListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_layers);
    }

    @Override
    protected void onStart() {
        super.onStart();
        DB db = DB.getInstance(this);
        db.open();
        layers = db.getLayers(null, false, true);
        db.close();
        layerListAdapter = new LayerListAdapter(this, layers);

        ListView lv = (ListView) findViewById(R.id.layers_list_view);
        if (lv != null) {
            lv.setAdapter(layerListAdapter);
            lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        }

    }

		@Override
		public void onClick(View v) {
				DB db=DB.getInstance(getApplicationContext());
				db.open();
				float[] randomHsv= {(float) (360*Math.random()),1,1};
				MyLayer newLayer = new MyLayer(0, "My layer", 0, Color.HSVToColor(randomHsv), true, getApplicationContext());
				db.saveLayer(newLayer);
				layers.add(newLayer);
				layerListAdapter.notifyDataSetChanged();
				db.close();
		}

		private class LayerListAdapter extends ArrayAdapter<MyLayer> {

        public LayerListAdapter(Context context, List<MyLayer> layers) {
            super(context, R.layout.activity_list_layers_row, layers);
        }

        @Override
        public View getView(final int position, View rowView , ViewGroup parent) {
            if (rowView == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = vi.inflate(R.layout.activity_list_layers_row, parent, false);
            }

            TextView layerNameView = (TextView) rowView.findViewById(R.id.layer_name);
            layerNameView.setText(layers.get(position).getName());
            layerNameView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), EditLayerActivity.class);
                    intent.putExtra("layer_id",layers.get(position).getId());
                    startActivity(intent);
                }
            });

            Drawable iconDrawable = layers.get(position).getLayerIconDrawable();
            if (iconDrawable != null) {
                iconDrawable.setBounds(0, 0,iconDrawable.getMinimumWidth(), iconDrawable.getMinimumHeight());
            }
            layerNameView.setCompoundDrawables(iconDrawable,null,null,null);

            CheckBox cb = (CheckBox) rowView.findViewById(R.id.layer_visible);
            changedByUser=false;
            if (layers.get(position).visible){
                cb.setChecked(true);
            }else{
                cb.setChecked(false);
            }
            changedByUser=true;
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (changedByUser) {
                        Log.d(TAG, "CheckBox View Changed");
                        layers.get(position).visible = isChecked;
                        DB db = DB.getInstance(getApplicationContext());
                        db.open();
                        db.editLayerInfo(layers.get(position));
                        db.close();
                        notifyDataSetChanged();
                    }
                }
            });
            return rowView;
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        MainActivity.isRefreshNeeded=true;
    }
}
