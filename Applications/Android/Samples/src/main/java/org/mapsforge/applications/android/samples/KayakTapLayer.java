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
import org.mapsforge.applications.android.samples.whitewater.WhitewaterNode;
import org.mapsforge.applications.android.samples.whitewater.WhitewaterOSM;
import org.mapsforge.applications.android.samples.whitewater.WhitewaterWay;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.PointOfInterest;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.view.MapView;

import java.io.File;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Created by vkmapsforge05 on 04.12.2014.
 */
public class KayakTapLayer extends Layer {
    private static final String TAG = "KayakTapLayer";
    private static final boolean test = true;
    private final Context mContext;
    private File mMapFile;
    private MapView mMapView;
    private MapDatabase mMapDatabase =null;
    private XmlRenderTheme mXmlRenderTheme;
    private GraphicFactory mGraphicFactory ;
    private LinkedHashMap<LatLong,PointOfInterest> mPOIList= null;
    private WhitewaterOSM mWhitewaterOSM = null;
    private static Handler mReadPoisAndWaysHandler = new Handler();
    private ArrayList<WhitewaterNode> mWhitewaterNodeList;
    private ArrayList<WhitewaterWay> mWhitewaterWayList;
    private String mWhitewaterFilePath = null;
    private String mWhitewater_suffix = "_whitewater";

    public KayakTapLayer(Context pContext, GraphicFactory pGraphicFactory, MapView pMapView,
                         File pMapfile, XmlRenderTheme pXMLRenderTheme,
                         MapDatabase pMapDatabase) {
        super();
        mContext = pContext;
        mMapView = pMapView;
        mMapFile = pMapfile;
        mXmlRenderTheme =pXMLRenderTheme;
        mGraphicFactory = pGraphicFactory;
        mMapDatabase = pMapDatabase;
        mWhitewaterOSM = new WhitewaterOSM(pContext);
        mPOIList = new LinkedHashMap<LatLong,PointOfInterest>();
        if (test) Log.i(TAG,"KayakTapLayer created");

    }
    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        if (test) Log.i(TAG, "draw BB begin ");
    }

    @Override
    public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
        return onTapWithWhitewaterFile(tapLatLong, layerXY, tapXY);
        //return onTapWithTiles(tapLatLong,layerXY,tapXY);
        //return super.onTap(tapLatLong, layerXY, tapXY);

    }

    private boolean onTapWithWhitewaterFile(LatLong tapLatLong, Point layerXY, Point tapXY) {
        if (test) Log.i(SamplesApplication.TAG, "Tap Coordinates " + tapLatLong.toString());
        double lon = tapLatLong.longitude;
        double lat = tapLatLong.latitude;
        byte zoomLevel = mMapView.getModel().mapViewPosition.getZoomLevel();
        // maybe there are some whitewater pois  in the boundingbox to test
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
        if (mWhitewaterOSM.getWhitewaterFileReadComplete()){
            if (mWhitewaterNodeList != null) {
                if (test) Log.i(SamplesApplication.TAG,"rad = " + rad );
                ArrayList<WhitewaterNode> listOfNodesInBB = new ArrayList<WhitewaterNode>();
                Log.i(TAG," nodes in list  " + mWhitewaterNodeList.size());
                for (int index = 0; index < mWhitewaterNodeList.size(); index++){
                    WhitewaterNode aWhitewaterNode = mWhitewaterNodeList.get(index);
                    LatLong aNodePoint = aWhitewaterNode.getLatLong();
                    if (aBoundingBox.contains(aNodePoint)) {
                        listOfNodesInBB.add(aWhitewaterNode);
                    }
                }

                Log.i(TAG," nodes in bb tapped " + listOfNodesInBB.size());
                if (listOfNodesInBB.size() > 0) {
                    showWhitewaterListInfoDialog(listOfNodesInBB, aBoundingBox);
                }
            } else {
                Log.i(TAG,"mNodeList " + mWhitewaterNodeList.toString());
            }
        }


        return true;
        //return super.onTap(tapLatLong, layerXY, tapXY);
    }

    private void showWhitewaterListInfoDialog(ArrayList<WhitewaterNode> pNodesList, BoundingBox pBoundingBox) {
        int count = pNodesList.size();
        double latSpan = pBoundingBox.getLatitudeSpan();
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        formatter.format("%d nodes in BoundingBox span %f", count, latSpan);
        if (test) Log.d(TAG, formatter.toString());
        if (count == 1) {
            int index = 0;
            WhitewaterNode aWhitewaterNode = pNodesList.get(index);
            showWhitewaterInfoDialog(aWhitewaterNode);
        } else {
            AlertDialog aDialog = new AlertDialog.Builder(this.mContext)
                    .setTitle("Whitewater " )
                    .setMessage(" there are "+ count + " whitewater nodes selectet, please increase the zoom level")
                    .setPositiveButton("OK", null)
                    .create();
            aDialog.show();
        }
    }

    private String getShapeNameToKey(String pKey){
        String result = null;
        if (pKey.equals("put_in")) {
            result = "kayak_put_in_green";
        }
        else if (pKey.equals("egress")){
            result = "kayak_out_green";
        }
        else if (pKey.equals("put_in;egress")) {
            result = "kayak_in_out_green";
        }
        else if (pKey.equals("rapid")) {
            result ="kayak_rapid_blue";
        }
        return result;
    }

    private void showWhitewaterInfoDialog(WhitewaterNode pWhitewaterNode){
        WhitewaterNode aWhitewaterNode = pWhitewaterNode;
        String shortShapeKey = aWhitewaterNode.getValueToKey("whitewater");
        String shortShapeIdStr = getShapeNameToKey(shortShapeKey);
        String renderDirectoryPath = mXmlRenderTheme.getRelativePathPrefix();
        if (renderDirectoryPath.endsWith("Rendertheme")) {
            renderDirectoryPath = renderDirectoryPath.replace("/Rendertheme","");
        }
        String shapePath = renderDirectoryPath + "/seamark_symbols/"+shortShapeIdStr+".png";
        String info = aWhitewaterNode.getId() + " " + aWhitewaterNode.tagsToString();
        if (test) Log.d(SamplesApplication.TAG,info);

        File aShapeFile = new File (shapePath);
        android.graphics.Bitmap aBitmap = null;
        if (aShapeFile.exists()){
            if (test) Log.d(TAG,"file exists : " + shapePath);
            aBitmap =  BitmapFactory.decodeFile(shapePath);
        } else {
            if (test) Log.d(SamplesApplication.TAG,"file not found : " + shapePath);
        }

        AlertDialog aDialog  = new AlertDialog.Builder(this.mContext)
                .setTitle("Whitewater Info Node " + aWhitewaterNode.getId())
                .setMessage(aWhitewaterNode.tagsToString())
                .setPositiveButton("OK",null)
                .create();
        if (aBitmap != null) {
            // the whiewater Symbol bitmap will be scaled,with factor 4
            if (test)Log.d(TAG,"bitmap exists : " + shapePath);
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


    private boolean onTapWithTiles(LatLong tapLatLong, Point layerXY, Point tapXY) {
        if (test) Log.i(SamplesApplication.TAG, "Tap Coordinates " + tapLatLong.toString());
        double lon = tapLatLong.longitude;
        double lat = tapLatLong.latitude;
        byte zoomLevel = mMapView.getModel().mapViewPosition.getZoomLevel();
        // maybe there are some whitewater pois in the boundingbox to test
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
            //showPoiListInfoDialog(listOfPoisInBB, aBoundingBox);
        }


        return true;
        //return super.onTap(tapLatLong, layerXY, tapXY);
    }

    public void updateWhitewaterFile() {
        if (mMapFile!= null) {
            File currentMapFile = mMapFile;
            if (currentMapFile != null) {
                String currentMapFilePath = currentMapFile.getAbsolutePath();
                setWhitewaterFilePathAndRead(currentMapFilePath);
                mReadPoisAndWaysHandler.postDelayed(readPoisAndWays, 1);
            }
        }
    }

    private Runnable readPoisAndWays = new Runnable() {
        public void run() {
            if (mWhitewaterOSM.getWhitewaterFileReadComplete()) {
                mWhitewaterNodeList = mWhitewaterOSM.getWhitewatersAsArrayList();
                mWhitewaterWayList = mWhitewaterOSM.getWhitewaterWaysAsArrayList();
            } else {
                mReadPoisAndWaysHandler.postDelayed(this,1000);
            }
        }
    };

    public void setWhitewaterFilePathAndRead(String aPath) {
        mWhitewaterOSM.clear();
        mWhitewaterFilePath = aPath;
        if (aPath.endsWith(".map")) {
            aPath = aPath.substring(0,aPath.length()-4);
            String aWhitewaterFilePath = aPath+ mWhitewater_suffix+".xml";
            Log.i(SamplesApplication.TAG,"try to open " + aWhitewaterFilePath);
            File xmlFile = new File (aWhitewaterFilePath);
            if (xmlFile.exists()){
                mWhitewaterFilePath = aWhitewaterFilePath;
                mWhitewaterOSM.readWhitewaterFile(mWhitewaterFilePath);
            }
            else {
                String aDatFilePath = aPath+ mWhitewater_suffix+".dat";
                Log.i(SamplesApplication.TAG,"try to open " + aDatFilePath);
                File aDatFile = new File(aDatFilePath);
                if (aDatFile.exists()){
                    mWhitewaterFilePath = aDatFilePath;
                    mWhitewaterOSM.readWhitewaterFile(mWhitewaterFilePath);
                }
                else {
                    String info = "no whitewater file " + aPath;
                    Log.i(SamplesApplication.TAG,info);
                }
            }
        }
        if (aPath.endsWith(".xml") || aPath.endsWith(".dat")){
            File aFile = new File(aPath);
            if (aFile.exists()){
                mWhitewaterFilePath = aPath;
                mWhitewaterOSM.readWhitewaterFile(aPath);
            }
            else {
                String info = "no whitewater file " + aPath;
                Log.i(SamplesApplication.TAG,info);
            }
        }
    }
}
