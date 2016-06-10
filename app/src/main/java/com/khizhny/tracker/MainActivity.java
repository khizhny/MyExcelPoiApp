package com.khizhny.tracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback,
    ClusterManager.OnClusterItemClickListener,
    GoogleMap.OnCameraChangeListener,
    ClusterManager.OnClusterClickListener {
    public static String TAG = "MyPOI";
    private static final String FILE_NAME = "MyPoints.xls";
    private static final String EXPORT_FOLDER = "MyPOI";
    private GoogleMap map;
    private ClusterManager<MyPoint> clusterManager;
    private ArrayList<MyLayer> layers;
    private int mapMode=1;
    private MyClusterRenderer myClusterRenderer;
    private LatLngBounds loadedBounds,cameraBounds;
    private float prevZoomLevel;
    public static boolean isRefreshNeeded=false; // flag for manual map refresh


    private class MyClusterRenderer extends DefaultClusterRenderer<MyPoint> {

        private IconGenerator mIconGenerator;

        public MyClusterRenderer() {
            super(getApplicationContext(), map, clusterManager);
            mIconGenerator=new IconGenerator(getApplicationContext());
        }

        @Override
        protected void onBeforeClusterItemRendered(MyPoint myItem, MarkerOptions markerOptions) {
            // Draw a single marker.
            //BitmapDescriptor ico;
            //ico = BitmapDescriptorFactory.fromI
            //ico=BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);

            markerOptions.icon(myItem.getIcon());
            markerOptions.title(myItem.getLabel());
        }

        @Override
        protected void onBeforeClusterRendered(Cluster<MyPoint> cluster, MarkerOptions markerOptions) {
            /*super.onBeforeClusterRendered(cluster, markerOptions);
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.locations));
            markerOptions.title(""+cluster.getItems().size());*/
            mIconGenerator.setBackground(getResources().getDrawable(R.drawable.locations));
            mIconGenerator.setTextAppearance(R.style.iconGenText);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(mIconGenerator.makeIcon(cluster.getSize()+"")));
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        // reloading data if camera moved out of loaded area
        cameraBounds = map.getProjection().getVisibleRegion().latLngBounds;
        if (Math.abs(prevZoomLevel-map.getCameraPosition().zoom) >= 2 || isRefreshNeeded){
            // if zoom changed a lot reloading data to free up memory from invisible markers.
            prevZoomLevel=map.getCameraPosition().zoom;
            loadedBounds=fetchData(cameraBounds);
            clusterManager.cluster();
            isRefreshNeeded=false;
        } else  {
            // if user just scrolls around check visible area and load data if needed
            if (!loadedBounds.contains(cameraBounds.southwest) || !loadedBounds.contains(cameraBounds.northeast)){
                loadedBounds=fetchData(cameraBounds);
            }
            clusterManager.cluster();
        }
        clusterManager.cluster();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRefreshNeeded){
            onCameraChange(map.getCameraPosition());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageButton mapModeButton = (ImageButton) findViewById(R.id.map_mode);
        mapModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapMode=mapMode+1;
                if (mapMode==5) mapMode=1;
                map.setMapType(mapMode);
            }
        });
        (findViewById(R.id.map_layers)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LayersActivity.class);
                startActivity(intent);
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapFragment.setHasOptionsMenu(true);
        layers = new ArrayList<>();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(50, 34), 8));
        clusterManager = new ClusterManager<>(this, map);

        myClusterRenderer=new MyClusterRenderer();
        //map.setOnCameraChangeListener(clusterManager);
        map.setOnCameraChangeListener(this);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setOnMarkerClickListener(clusterManager);
        map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        clusterManager.setRenderer(myClusterRenderer);
        clusterManager.setOnClusterItemClickListener(this);
        clusterManager.setOnClusterClickListener(this);
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                Intent intent = new Intent(getApplicationContext(),EditPointActivity.class);
                intent.putExtra("lat",latLng.latitude);
                intent.putExtra("lon",latLng.longitude);
                intent.putExtra("point_id",0);
                startActivity(intent);
            }
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        } else{
            map.setMyLocationEnabled(true);
        }
    }

    /**
     * Loads markers on the map around the visible area.
     * @param bounds - bounds of the visible area.
     * @return - bounds of the area for which data was loaded. (it 300% bigger than visible area)
     */
    private LatLngBounds fetchData(LatLngBounds bounds) {
        // reading points from db
        LatLngBounds fetchedBounds=extendBounds(bounds);
        Log.d(TAG, "Fetch data started for bounds="+fetchedBounds);
        DB db = DB.getInstance(this);
        db.open();
        layers = db.getLayers(fetchedBounds,true,true);
        db.close();
        map.clear();
        clusterManager.clearItems();
        for (MyLayer layer : layers) {
            if (layer.visible) {
                for (MyPoint point : layer.points) {
                    clusterManager.addItem(point);
                }
            }
        }
        return fetchedBounds;
    }

    @Override
    public boolean onClusterItemClick(ClusterItem clusterItem) {
        /*new AlertDialog.Builder(this)
                .setTitle(((MyPoint)clusterItem).getLabel())
                .setMessage(((MyPoint)clusterItem).getComment())
                .show();*/
        Intent intent = new Intent(getApplicationContext(),EditPointActivity.class);
        intent.putExtra("point_id",((MyPoint) clusterItem).getId());
        startActivity(intent);
        return true;
    }

    @Override
    public boolean onClusterClick(Cluster cluster) {
        String msg = "";
        String separator = "";
        Iterator itr=cluster.getItems().iterator();
        int i=0;
        while (itr.hasNext()) {
            if (i==8) {
                msg=msg+"...";
                break;
            }
            msg=msg+separator+((MyPoint) itr.next()).getLabel();
            separator = ", ";
            i=i+1;
        }
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_export:
                exportToExcel();
                break;
            case R.id.menu_item_import:
                importFromExcel();
                break;
            case R.id.menu_item_quit:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

  /*  private String getExportFolder() {
        Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory() + "/");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(selectedUri, "resource/folder");

        if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
            startActivity(intent);
            return "OK";
        } else {
            Toast.makeText(this, "No compatible file browser :(", Toast.LENGTH_SHORT).show();
            return "NOK";
        }

    }*/

    private void exportToExcel() {        //Saving file in external storage
        File sdCard = Environment.getExternalStorageDirectory();
        File directory = new File(sdCard.getAbsolutePath() + "/" + EXPORT_FOLDER);

        //create directory if not exist
        if (!directory.isDirectory()) {
            directory.mkdirs();
        }

        //file path
        File file = new File(directory, FILE_NAME);

        WorkbookSettings wbSettings = new WorkbookSettings();
        wbSettings.setLocale(new Locale("en", "EN"));
        WritableWorkbook workbook;

        try {
            workbook = Workbook.createWorkbook(file, wbSettings);
            //Excel sheet name. 0 represents first sheet
            WritableSheet pointsSheet = workbook.createSheet("MyPoints", 0);
            WritableSheet layerSheet = workbook.createSheet("MyLayers", 1);
            try {
                int i = 0;
                int j = 0;
                pointsSheet.addCell(new Label(0, i, "Num"));
                pointsSheet.addCell(new Label(1, i, "Label"));
                pointsSheet.addCell(new Label(2, i, "Longitude"));
                pointsSheet.addCell(new Label(3, i, "Latitude"));
                pointsSheet.addCell(new Label(4, i, "Description"));
                pointsSheet.addCell(new Label(5, i, "Layer Id"));
                // layer ws headers
                layerSheet.addCell(new Label(0, j, "id"));
                layerSheet.addCell(new Label(1, j, "Name"));
                layerSheet.addCell(new Label(2, j, "IconId"));
                layerSheet.addCell(new Label(3, j, "Color"));
                DB db = DB.getInstance(getApplicationContext());
                db.open();
                ArrayList<MyLayer> allLayers=db.getLayers(null, true,false);
                db.close();

                for (MyLayer l : allLayers) {
                    j = j + 1;
                    layerSheet.addCell(new Label(0, j, l.getId() + ""));
                    layerSheet.addCell(new Label(1, j, l.getName()));
                    layerSheet.addCell(new Label(2, j, l.getIconId() + ""));
                    layerSheet.addCell(new Label(3, j, l.getColor() + ""));
                    for (MyPoint p : l.points) {
                        i = i + 1;
                        pointsSheet.addCell(new Label(0, i, "" + i));
                        pointsSheet.addCell(new Label(1, i, p.getLabel()));
                        pointsSheet.addCell(new Label(2, i, p.getLongitude(false)));
                        pointsSheet.addCell(new Label(3, i, p.getLatitude(false)));
                        pointsSheet.addCell(new Label(4, i, p.getComment()));
                        pointsSheet.addCell(new Label(5, i, l.getId() + ""));
                    }
                }
            } catch (RowsExceededException e) {
                Toast.makeText(this, "Too many rows to save.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } catch (WriteException e) {
                Toast.makeText(this, "Can't write to " + directory + "/" + FILE_NAME, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
            workbook.write();
            try {
                workbook.close();
                Toast.makeText(this, "File saved to " + directory + "/" + FILE_NAME, Toast.LENGTH_SHORT).show();
            } catch (WriteException e) {
                Toast.makeText(this, "Can't saved to " + directory + "/" + FILE_NAME, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Can't create workbook " + directory + "/" + FILE_NAME, Toast.LENGTH_SHORT).show();
        }
    }

    private void importFromExcel() {        //Saving file in external storage
        File sdCard = Environment.getExternalStorageDirectory();
        File directory = new File(sdCard.getAbsolutePath() + "/" + EXPORT_FOLDER);
        //create directory if not exist
        if (!directory.isDirectory()) {
            directory.mkdirs();
        }

        //file path
        File file = new File(directory, FILE_NAME);
        Workbook workbook;
        try {
            workbook = Workbook.getWorkbook(file);
            Sheet pointsSheet = workbook.getSheet(0); // "MyPoints"
            Sheet layerSheet = workbook.getSheet(1);  // "MyLayers"
            try {
                int total_layer = layerSheet.getRows();
                for (int j = 1; j < total_layer; j++) {
                    int layerId = Integer.parseInt(layerSheet.getCell(0, j).getContents());
                    String layerName = layerSheet.getCell(1, j).getContents();
                    int layerIconId = Integer.parseInt(layerSheet.getCell(2, j).getContents());
                    int layerColor = Integer.parseInt(layerSheet.getCell(3, j).getContents());
                    if (layerId != 0) {
                        MyLayer new_layer = new MyLayer(layerId, layerName, layerIconId, layerColor, true, getApplicationContext());
                        int total_points = pointsSheet.getRows();
                        for (int i = 1; i < total_points; i++) {
                            int pointLayerId = Integer.parseInt(pointsSheet.getCell(5, i).getContents());
                            if (pointLayerId == layerId) {
                                String label = pointsSheet.getCell(1, i).getContents();
                                double lon = ((NumberCell) pointsSheet.getCell(2, i)).getValue();
                                double lat = ((NumberCell) pointsSheet.getCell(3, i)).getValue();
                                String comment = pointsSheet.getCell(4, i).getContents();
                                new_layer.points.add(new MyPoint(new LatLng(lat, lon), comment, label,new_layer,0));
                            }
                        }
                        DB db = DB.getInstance(this);
                        db.open();
                        db.saveLayer(new_layer);
                        db.close();
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
            try {
                workbook.close();
                Toast.makeText(this, "Import finished.", Toast.LENGTH_SHORT).show();
                isRefreshNeeded=true;
                onCameraChange(map.getCameraPosition());
            } catch (Exception e) {
                Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Extends bounds to 300% in each direction.
     * @param bounds - bounds before
     * @return - bounds after.
     */
    private LatLngBounds extendBounds(LatLngBounds bounds){
        double maxLon=2*bounds.southwest.longitude-bounds.northeast.longitude;
        double minLon=2*bounds.northeast.longitude-bounds.southwest.longitude;
        double maxLat=2*bounds.northeast.latitude-bounds.southwest.latitude;
        double minLat=2*bounds.southwest.latitude-bounds.northeast.latitude;
        return (new LatLngBounds(new LatLng(minLat,maxLon),new LatLng(maxLat,minLon)));
    }
}
