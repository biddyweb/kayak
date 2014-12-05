package org.mapsforge.applications.android.samples;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;

import org.mapsforge.applications.android.samples.seamark.SeamarkNode;
import org.mapsforge.applications.android.samples.seamark.SeamarkOSM;
import org.mapsforge.applications.android.samples.seamark.SeamarkWay;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.TilePosition;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.MapReadResult;
import org.mapsforge.map.reader.PointOfInterest;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.util.LayerUtil;
import org.mapsforge.map.view.MapView;

import java.io.File;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by vkmapsforge05 on 05.11.14.
 */
public class BuoyTapLayer extends Layer {
    private static final String TAG ="BuoyTapLayer";
    private static final boolean test = true;
    private final Context mContext;
    private SeamarkOSM mSeamarkOSM;
    private File mMapFile;
    private MapView mMapView;
    private ArrayList<SeamarkNode> mSeamarkNodeList;
    private ArrayList<SeamarkWay> mSeamarkWayList;
    private String mSeamarkFilePath = "";
    private XmlRenderTheme mXmlRenderTheme;
    private GraphicFactory mGraphicFactory ;

    private MapDatabase mMapDatabase =null;
    private LinkedHashMap<LatLong,PointOfInterest> mPOIList= null;
    private String mBBoxkey="";

    private static Handler mReadPoisAndWaysHandler = new Handler();

    public BuoyTapLayer(Context pContext, GraphicFactory pGraphicFactory, MapView pMapView, File pMapfile, XmlRenderTheme pXMLRenderTheme,
                        SeamarkOSM pSeamarkOSM, MapDatabase pMapDatabase) {
        super();
        mContext = pContext;
        mMapView = pMapView;
        mSeamarkOSM = pSeamarkOSM;
        mMapFile = pMapfile;
        mXmlRenderTheme =pXMLRenderTheme;
        mGraphicFactory = pGraphicFactory;
        mMapDatabase = pMapDatabase;
        mPOIList = new LinkedHashMap<LatLong,PointOfInterest>();
        mBBoxkey="";
    }
    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        if (test) Log.i(TAG, "draw BB begin ");
        String aBBoxkey = boundingBox.toString();
        if (!aBBoxkey.equals(mBBoxkey)) {
            mBBoxkey = aBBoxkey;
            if (test) Log.i(TAG, "draw with new  " + boundingBox.toString());
            List<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, zoomLevel, topLeftPoint,
                    this.displayModel.getTileSize());
            for (int i = tilePositions.size() - 1; i >= 0; --i) {
                TilePosition aTilePosition = tilePositions.get(i);
                Tile aTile = aTilePosition.tile;
                readPoisToTile(aTile);
            }
        } else {
            if (test) Log.i(TAG,"redrawing with same BBox");
        }
        if (test)Log.i(TAG,"found POIS " +  mPOIList.size() );
        if (test)Log.i(TAG,"draw end " );

    }

    private void readPoisToTile (Tile aTile){
        try {
            if (mMapDatabase != null) {
                MapReadResult aMapReadResult = mMapDatabase.readMapData(aTile);
                List<PointOfInterest> aPOIList = aMapReadResult.pointOfInterests;
                int countNewPois = 0;
                for (int index = 0; index < aPOIList.size(); index++) {
                    PointOfInterest aPoi = aPOIList.get(index);
                    LatLong aPoiKey = aPoi.position;
                    if (!mPOIList.containsKey(aPoiKey)) {
                        countNewPois++;
                        mPOIList.put(aPoiKey, aPoi);
                    }
                }
                // Log.i(TAG,"new pois in tap Layer " + countNewPois);
            }

        } catch (Exception e) {
           Log.i(TAG,"error in readPoisToTile "+ e.toString()) ;
        }

    }

    @Override
    public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
        //return onTapWithSeamarksFile(tapLatLong, layerXY, tapXY);
        return onTapWithTiles(tapLatLong,layerXY,tapXY);
        //return super.onTap(tapLatLong, layerXY, tapXY);

    }

    private boolean onTapWithTiles(LatLong tapLatLong, Point layerXY, Point tapXY) {
        if (test) Log.i(SamplesApplication.TAG, "Tap Coordinates " + tapLatLong.toString());
        double lon = tapLatLong.longitude;
        double lat = tapLatLong.latitude;
        byte zoomLevel = mMapView.getModel().mapViewPosition.getZoomLevel();
        // maybe there are some seamarks in the boundingbox to test
        // we handle only the first one
        double rad = 0.005d;
        if (zoomLevel > 14) {
            rad = 0.0005d;
        }
        if (zoomLevel > 15) {
            rad = 0.0001d;
        }
        double minLon = lon - rad;
        double maxLon = lon + rad;
        double minLat = lat -rad;
        double maxLat= lat + rad;


        BoundingBox aBoundingBox = new BoundingBox(minLat, minLon,maxLat,maxLon);
        ArrayList<PointOfInterest> listOfPoisInBB = new ArrayList<PointOfInterest>();
        int count = mPOIList.size();
        Set<LatLong> aSet = mPOIList.keySet();

        for (Iterator<LatLong> it = aSet.iterator();it.hasNext();) {
            LatLong aLatLong = it.next();
            if (aBoundingBox.contains(aLatLong)){
               PointOfInterest aPoi = mPOIList.get(aLatLong);
               listOfPoisInBB.add(aPoi);
            }
        }
        Log.i(TAG,"Number of POIS in BB " +listOfPoisInBB.size() );
        if (listOfPoisInBB.size() > 0 ){
            showPoiListInfoDialog(listOfPoisInBB, aBoundingBox);
        }


        return true;
        //return super.onTap(tapLatLong, layerXY, tapXY);
    }

    private void showPoiListInfoDialog(ArrayList<PointOfInterest> pNodesList, BoundingBox pBoundingBox) {
        int count = pNodesList.size();
        double latSpan = pBoundingBox.getLatitudeSpan();
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        formatter.format("%d pois in BoundingBox span %f", count, latSpan);
        if (test) Log.d(SamplesApplication.TAG, formatter.toString());
        if (count == 1) {
            int index = 0;
           PointOfInterest poi = pNodesList.get(index);
            showPoiInfoDialog(poi);
        } else {
            AlertDialog aDialog = new AlertDialog.Builder(this.mContext)
                    .setTitle("POIS " )
                    .setMessage(" there are "+ count + " Pois selectet, increase the zoom level")
                    .setPositiveButton("OK", null)
                    .create();
            aDialog.show();
        }
    }

    private String getInfoFromPoi(PointOfInterest poi){
        StringBuffer buf =new StringBuffer();
        buf.append(poi.position.toString());
        buf.append("\n");
        int count = poi.tags.size();
        for (int index = 0; index < count; index++){
            Tag aTag = poi.tags.get(index);
            buf.append(aTag.toString());
            buf.append(("\n"));
        }
        return buf.toString();
    }

    private SeamarkNode makeSeamarkNodeFromPoi(PointOfInterest poi){
        SeamarkNode aSeamarkNode = new SeamarkNode("dummy");
        int count = poi.tags.size();
        for (int index = 0; index < count; index++){
            Tag aTag = poi.tags.get(index);
            aSeamarkNode.addTag(aTag);
        }
        return aSeamarkNode;
    }

    private void showPoiInfoDialog(PointOfInterest poi){
        String aMsg = getInfoFromPoi(poi);
        String renderDirectory = mXmlRenderTheme.getRelativePathPrefix();
        SeamarkNode aSeamarkNode = makeSeamarkNodeFromPoi(poi);
        String shortShapeIdStr = aSeamarkNode.getValueToKey("seamark:short_shape_id");
        String shapePath = renderDirectory + "/seamark_symbols/"+shortShapeIdStr+".png";
        String aSeamarkType = aSeamarkNode.getValueToKey("seamark:type");
        String aName = aSeamarkNode.getValueToKey("name");
        if (test) Log.d(SamplesApplication.TAG,shapePath);
        File aShapeFile = new File (shapePath);
        android.graphics.Bitmap aBitmap = null;
        if (aShapeFile.exists()){
            if (test) Log.d(SamplesApplication.TAG,"file exists : " + shapePath);
            aBitmap =  BitmapFactory.decodeFile(shapePath);
        } else {
            if (test) Log.d(SamplesApplication.TAG,"file not found : " + shapePath);
        }
        AlertDialog aDialog  = new AlertDialog.Builder(this.mContext)
                .setTitle(aName )
                .setMessage(aMsg)
                .setPositiveButton("OK",null)
                .create();
        if (aBitmap != null) {
            // the Seamark Symbol bitmap will be scaled,with factor 4
            if (test)Log.d(SamplesApplication.TAG,"bitmap exists : " + shapePath);
            int height = aBitmap.getHeight() * 4;
            int width = aBitmap.getWidth() *4;
            android.graphics.Bitmap.Config aBitmapConfig =  android.graphics.Bitmap.Config.ARGB_8888;
            android.graphics.Bitmap aBiggerBitmap  = android.graphics.Bitmap.createBitmap (width, height,aBitmapConfig);
            android.graphics.Canvas aCanvas = new android.graphics.Canvas(aBiggerBitmap);
            RectF aRectF = new RectF(1,1,width-1,height-1);
            if (test)Log.d(SamplesApplication.TAG,"rHeight " + aRectF.height() + " rWidth  "+ aRectF.width());
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            aCanvas.drawRect(aRectF,paint);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(2);
            paint.setStyle(Paint.Style.STROKE);
            aCanvas.drawBitmap(aBitmap, null, aRectF, paint);
            //aCanvas.drawCircle(width/2, height/2,30,paint);
            Drawable aDrawable = new BitmapDrawable(aBiggerBitmap);
            aDialog.setIcon(aDrawable);
        } else {
            Log.d(SamplesApplication.TAG,"no bitmap exists : " + shapePath);
        }
        aDialog.show();
    }

    private boolean onTapWithSeamarksFile(LatLong tapLatLong, Point layerXY, Point tapXY) {
        if (test) Log.i(SamplesApplication.TAG, "Tap Coordinates " + tapLatLong.toString());
        double lon = tapLatLong.longitude;
        double lat = tapLatLong.latitude;
        byte zoomLevel = mMapView.getModel().mapViewPosition.getZoomLevel();
        // maybe there are some seamarks in the boundingbox to test
        // we handle only the first one
        double rad = 0.005d;
        if (zoomLevel > 14) {
            rad = 0.0005d;
        }
        if (zoomLevel > 15) {
            rad = 0.0001d;
        }
        double minLon = lon - rad;
        double maxLon = lon + rad;
        double minLat = lat -rad;
        double maxLat= lat + rad;


        BoundingBox aBoundingBox = new BoundingBox(minLat, minLon,maxLat,maxLon);
        if (mSeamarkOSM.getSeamarkFileReadComplete()){
            if (mSeamarkNodeList != null) {
                if (test) Log.i(SamplesApplication.TAG,"rad = " + rad );
                ArrayList<SeamarkNode> listOfNodesInBB = new ArrayList<SeamarkNode>();
                Log.i(TAG," nodes in list  " + mSeamarkNodeList.size());
                for (int index = 0; index < mSeamarkNodeList.size(); index++){
                    SeamarkNode aSeamarkNode = mSeamarkNodeList.get(index);
                    LatLong aNodePoint = aSeamarkNode.getLatLong();
                    if (aBoundingBox.contains(aNodePoint)) {
                        listOfNodesInBB.add(aSeamarkNode);
                    }
                }

                Log.i(TAG," nodes in bb tapped " + listOfNodesInBB.size());
                if (listOfNodesInBB.size() > 0) {
                    showSeamarkListInfoDialog(listOfNodesInBB,aBoundingBox);
                }
            } else {
                Log.i(TAG,"mNodeList " + mSeamarkNodeList.toString());
            }
        }

        return true;
        //return super.onTap(tapLatLong, layerXY, tapXY);
    }







    private void showSeamarkListInfoDialog(ArrayList<SeamarkNode> pNodesList, BoundingBox pBoundingBox) {
        int count = pNodesList.size();
        double latSpan = pBoundingBox.getLatitudeSpan();
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        formatter.format("%d nodes in BoundingBox span %f", count, latSpan);
        if (test) Log.d(SamplesApplication.TAG, formatter.toString());
        if (count == 1) {
            int index = 0;
            SeamarkNode aSeamarkNode = pNodesList.get(index);
            showSeamarkInfoDialog(aSeamarkNode);
        } else {
            AlertDialog aDialog = new AlertDialog.Builder(this.mContext)
                    .setTitle("Seamrks " )
                    .setMessage(" there are "+ count + " seamarks selectet, increase the zoom level")
                    .setPositiveButton("OK", null)
                    .create();
            aDialog.show();
        }
    }

    private void showSeamarkInfoDialog(SeamarkNode pSeamarkNode){
        SeamarkNode aSeamarkNode = pSeamarkNode;
        String info = aSeamarkNode.getId() + " " + aSeamarkNode.tagsToString();
        if (test) Log.d(SamplesApplication.TAG,info);

        String renderDirectory = mXmlRenderTheme.getRelativePathPrefix();
        String shortShapeIdStr = aSeamarkNode.getValueToKey("seamark:short_shape_id");
        String shapePath = renderDirectory + "/seamark_symbols/"+shortShapeIdStr+".png";
        if (test) Log.d(SamplesApplication.TAG,shapePath);
        File aShapeFile = new File (shapePath);
        android.graphics.Bitmap aBitmap = null;
        if (aShapeFile.exists()){
         if (test) Log.d(SamplesApplication.TAG,"file exists : " + shapePath);
          aBitmap =  BitmapFactory.decodeFile(shapePath);
        } else {
            if (test) Log.d(SamplesApplication.TAG,"file not found : " + shapePath);
        }
        AlertDialog aDialog  = new AlertDialog.Builder(this.mContext)
                .setTitle("Seamark Info Node " + aSeamarkNode.getId())
                .setMessage(aSeamarkNode.tagsToString())
                .setPositiveButton("OK",null)
                .create();
        if (aBitmap != null) {
            // the Seamark Symbol bitmap will be scaled,with factor 4
            if (test)Log.d(SamplesApplication.TAG,"bitmap exists : " + shapePath);
            int height = aBitmap.getHeight() * 4;
            int width = aBitmap.getWidth() *4;
            android.graphics.Bitmap.Config aBitmapConfig =  android.graphics.Bitmap.Config.ARGB_8888;
            android.graphics.Bitmap aBiggerBitmap  = android.graphics.Bitmap.createBitmap (width, height,aBitmapConfig);
            android.graphics.Canvas aCanvas = new android.graphics.Canvas(aBiggerBitmap);
            RectF aRectF = new RectF(1,1,width-1,height-1);
            if (test)Log.d(SamplesApplication.TAG,"rHeight " + aRectF.height() + " rWidth  "+ aRectF.width());
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            aCanvas.drawRect(aRectF,paint);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(2);
            paint.setStyle(Paint.Style.STROKE);
            aCanvas.drawBitmap(aBitmap, null, aRectF, paint);
            //aCanvas.drawCircle(width/2, height/2,30,paint);
            Drawable aDrawable = new BitmapDrawable(aBiggerBitmap);
            aDialog.setIcon(aDrawable);
        } else {
            Log.d(SamplesApplication.TAG,"no bitmap exists : " + shapePath);
        }
        aDialog.show();
    }

    public void updateSeamarkFile() {
        if (mMapFile!= null) {
            File currentMapFile = mMapFile;
            if (currentMapFile != null) {
                String currentMapFilePath = currentMapFile.getAbsolutePath();
                setSeamarkFilePathAndRead(currentMapFilePath);
                mReadPoisAndWaysHandler.postDelayed(readPoisAndWays, 1);
            }
        }
    }

    private Runnable readPoisAndWays = new Runnable() {
        public void run() {
            if (mSeamarkOSM.getSeamarkFileReadComplete()) {
                mSeamarkNodeList = mSeamarkOSM.getSeamarksAsArrayList();
                mSeamarkWayList = mSeamarkOSM.getSeamarkWaysAsArrayList();
            } else {
                mReadPoisAndWaysHandler.postDelayed(this,1000);
            }
        }
    };

    public void setSeamarkFilePathAndRead(String aPath) {
       mSeamarkOSM.clear();
       mSeamarkFilePath = aPath;
       if (aPath.endsWith(".map")) {
           aPath = aPath.substring(0,aPath.length()-4);
           String aSeamarkFilePath = aPath+ "_seamarks"+".xml";
           Log.i(SamplesApplication.TAG,"try to open " + aSeamarkFilePath);
           File xmlFile = new File (aSeamarkFilePath);
           if (xmlFile.exists()){
              mSeamarkFilePath = aSeamarkFilePath;
              mSeamarkOSM.readSeamarkFile(mSeamarkFilePath);
           }
           else {
               String aDatFilePath = aPath+ "_seamarks"+".dat";
               Log.i(SamplesApplication.TAG,"try to open " + aDatFilePath);
               File aDatFile = new File(aDatFilePath);
               if (aDatFile.exists()){
                   mSeamarkFilePath = aDatFilePath;
                   mSeamarkOSM.readSeamarkFile(mSeamarkFilePath);
               }
               else {
                   String info = "no seamark file " + aPath;
                   Log.i(SamplesApplication.TAG,info);
               }
           }
       }
       if (aPath.endsWith(".xml") || aPath.endsWith(".dat")){
           File aFile = new File(aPath);
           if (aFile.exists()){
               mSeamarkFilePath = aPath;
               mSeamarkOSM.readSeamarkFile(aPath);
           }
           else {
               String info = "no seamark file " + aPath;
               Log.i(SamplesApplication.TAG,info);
           }
       }
    }
}
