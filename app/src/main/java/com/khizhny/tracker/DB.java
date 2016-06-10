package com.khizhny.tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import static com.khizhny.tracker.MainActivity.TAG;

import java.util.ArrayList;

public class DB extends SQLiteOpenHelper {

    private static String DB_NAME="database";
    private static int DB_VERSION=1;
    private static final int MAX_POINTS_LIMIT=100000;
    private static DB instance;
    private static SQLiteDatabase database;
    private static Context ctx;

    private DB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static DB getInstance(Context context) {
        if (instance == null) {
            instance = new DB(context);
            ctx=context;
        }
        return instance;
    }

    public void open (){
        database=getWritableDatabase();
    }

    public void addOrEditPoint(MyLayer layer, MyPoint point){
        ContentValues cv = new ContentValues();
        cv.put("lat",point.getPosition().latitude);
        cv.put("lon",point.getPosition().longitude);
        cv.put("layer_id",layer.getId());
        cv.put("label",point.getLabel());
        cv.put("description",point.getComment());
        if (point.getId()==0){
            //adding new point
            database.insert("points", null, cv);
        } else {
            //editing point
            database.update("points", cv, "_id=?",new String[]{point.getId()+""});
        }
    }

    /**
     * Fills Database with some random Layers with markers
     */
    public void fillTestValues() {
        Log.d(TAG,"Start filling DB with test data...");
        for (int j=0; j<3; j++) {
            float[] randomHsv= {(float) (360*Math.random()),1,1};
            MyLayer layer = new MyLayer(0, "layer_"+j, j, Color.HSVToColor(randomHsv), true, ctx);
            double lat,lon;
            for (int i = 1; i < 100; i++) {
                lat = 46 + 6 * Math.random();
                lon = 27 + 13 * Math.random();
                layer.points.add(new MyPoint(new LatLng(lat, lon), "Sample point.", "Layer #"+j+" Point #" + i, layer,0));
            }
            saveLayer(layer);
        }
        Log.d(TAG,"Finished filling DB with test data.");
    }

    public void editLayerInfo(MyLayer layer){
        ContentValues cv = new ContentValues();
        // saving layer to db
        cv.put("_id",layer.getId());
        cv.put("name",layer.getName());
        cv.put("icon_id",layer.getIconId());
        cv.put("color",layer.getColor());
        if (layer.visible) {
            cv.put("visible", 1);
        }else{
            cv.put("visible", 0);
        }
        database.beginTransaction();
        try {
            database.update("layers",cv, "_id="+layer.getId(),null);
            database.setTransactionSuccessful();
        }catch (Exception e) {
            Log.e(TAG,"Error editing layer " + layer.getName()+ ".");
        } finally {
            database.endTransaction();
        }
    }
    /**
     * Saves layer to Db as a new record layer. (including all points in it.)
     * @param layer Layer object to be saved to DB.
     * @return Layer database id or 0 is fails.
     */
    public int saveLayer(MyLayer layer){
        ContentValues cv = new ContentValues();
        // saving layer to db
        cv.put("name",layer.getName());
        cv.put("icon_id",layer.getIconId());
        cv.put("color",layer.getColor());
        if (layer.visible) {
            cv.put("visible",1);
        }else {
            cv.put("visible",0);
        }

        database.beginTransaction();
        try {
            long dbRowId=database.insert("layers", null, cv);
            if (dbRowId==-1) {
                Log.d(TAG,"Error saving layer " + layer.getName()+ ".");
                return 0;
            } else {
                Cursor c = database.rawQuery("SELECT _id FROM layers WHERE rowid="+dbRowId, null);
                c.moveToFirst();
                int layerId=c.getInt(0);
                c.close();
                layer.setId(layerId);
                // saving all points on the layer to db
                for (MyPoint point: layer.points) {
                    addOrEditPoint(layer,point);
                }
                database.setTransactionSuccessful();
                return layerId;
            }
        } catch (Exception e) {
            Log.e(TAG,"Error saving layer " + layer.getName()+ ".");
            return 0;
        } finally {
            database.endTransaction();
        }
    }

    /**
     * Deletes the layer
     * @param layerId Id of layer
     * @return True if success. Otherwise false.
     */
    public boolean deleteLayer(int layerId){
        database.beginTransaction();
        try {
            database.delete("points","layer_id=?",new String[]{layerId+""});
            database.delete("layers","_id=?",new String[]{layerId+""});
            database.setTransactionSuccessful();
        } catch (Exception e) {
            return false;
        } finally {
            database.endTransaction();

        }
        return true;
    }

    public boolean deletePoint(int pointId){
        database.beginTransaction();
        try {
            database.delete("points","_id=?",new String[]{pointId+""});
            database.setTransactionSuccessful();
        } catch (Exception e) {
            return false;
        } finally {
            database.endTransaction();
        }
        return true;
    }

    /**
     * Gets a list of points for the specified layer_id
     * @param layer - The layer
     * @param bounds - Geographical filter. If null - no filtering.
     * @return List of points
     */
    public ArrayList<MyPoint> getPoints(MyLayer layer, LatLngBounds bounds){
        Log.d(TAG,"Start reading points for bounds "+bounds);
        Cursor c = database.rawQuery("SELECT lat, lon, description, label, _id FROM points WHERE layer_id="+layer.getId()+" AND rowid<="+MAX_POINTS_LIMIT, null);
        // looping through all rows and adding to list
        ArrayList<MyPoint> points = new ArrayList<>();
        LatLng point;
        if (c.moveToFirst()) {
            do {
                point = new LatLng(c.getDouble(0), c.getDouble(1));
                if (bounds==null){
                    points.add(new MyPoint(point, c.getString(2), c.getString(3), layer,c.getInt(4)));
                } else {
                    if (bounds.contains(point)) {
                        points.add(new MyPoint(point, c.getString(2), c.getString(3),layer,c.getInt(4)));
                    }
                }
            } while (c.moveToNext());
        }
        c.close();
        Log.d(TAG,"Points read from db  "+  points.size()+" after bounds filtering "+points.size());
        return points;
    }

    /**
     * Gets a single point from DB for editing. No layer object. Just layer Id.
     * @param id Point ID.
     * @return MyPoint Object
     */
    public MyPoint getPoint(int id){
        Cursor c = database.rawQuery("SELECT lat, lon, description, label, layer_id FROM points WHERE _id="+id, null);
        MyPoint rez=null;
        if (c.moveToFirst()) {
           rez=new MyPoint(new LatLng(c.getDouble(0), c.getDouble(1)), c.getString(2), c.getString(3), c.getInt(4),id);
        c.close();
        }
        return rez;
    }

    /**
     * Get all of the layers stored in the database.
     * @param bounds bounds of  camera view.  Used for filtering.
     * @param includeMarkers If false only layer info will be loaded.
     * @param visibleMarkersOnly If true only markers on visible layers will be loaded.
     * @return List of MyLayer elements (including all points on that layers).
     */
    public ArrayList<MyLayer> getLayers(LatLngBounds bounds, boolean includeMarkers, boolean visibleMarkersOnly){
        Log.d(TAG,"Start reading layers...");
        Cursor c = database.rawQuery("SELECT _id, name, icon_id, color, visible FROM layers", null);
        ArrayList<MyLayer> layers = new ArrayList<>();
        if (c.moveToFirst()) {
            do {
                MyLayer layer = new MyLayer(c.getInt(0),c.getString(1),c.getInt(2),c.getInt(3),c.getInt(4)!=0,ctx);
                layers.add(layer);
                if (includeMarkers) {
                    if (!visibleMarkersOnly || layer.visible) {
                        layer.points = getPoints(layer, bounds);
                    }
                }
            } while (c.moveToNext());
        }
        c.close();
        return layers;
    }
    /**
     * Get a single layer stored in the database.
     * @param includeMarkers If false only layer info will be loaded.
     * @return Layer object or null
     */
    public MyLayer getLayer(int layerId, boolean includeMarkers){
        Cursor c = database.rawQuery("SELECT _id, name, icon_id, color, visible FROM layers WHERE _id="+layerId, null);
        MyLayer layer=null;
        if (c.moveToFirst()) {
            do {
                layer = new MyLayer(c.getInt(0),c.getString(1),c.getInt(2),c.getInt(3),c.getInt(4)!=0,ctx);
                if (includeMarkers) {
                    layer.points = getPoints(layer, null);
                }
            } while (c.moveToNext());
        }
        c.close();
        return layer;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database...");
        db.beginTransaction();
        try {
            // adding layers table
            db.execSQL("CREATE TABLE `layers` (\n" +
                    "\t`_id`\tINTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,\n" +
                    "\t`name`\tTEXT NOT NULL,\n" +
                    "\t`icon_id`\tINTEGER NOT NULL,\n" +
                    "\t`color`\tINTEGER,\n" +
                    "\t`visible`\tINTEGER\n" +
                    ");");

            // adding points table
            db.execSQL("CREATE TABLE `points` (\n" +
                    "\t`_id`\tINTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,\n" +
                    "\t`layer_id`\tINTEGER NOT NULL,\n" +
                    "\t`lon`\tNUMERIC NOT NULL,\n" +
                    "\t`lat`\tNUMERIC NOT NULL,\n" +
                    "\t`label`\tTEXT,\n" +
                    "\t`description`\tTEXT,\n" +
                    "\tFOREIGN KEY(`layer_id`) REFERENCES layers (_id)\n" +
                    ");");
            database=db;
            //fillTestValues();
            db.setTransactionSuccessful();
        } catch (Exception e) {
            //Error in between database transaction
        } finally {
            db.endTransaction();
            fillTestValues();
        }
        }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Database upgrade section
    }

}
