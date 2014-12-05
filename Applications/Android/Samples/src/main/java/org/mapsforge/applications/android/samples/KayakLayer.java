package org.mapsforge.applications.android.samples;

import java.util.ArrayList;
import java.util.Formatter;

import org.mapsforge.applications.android.samples.service.TrackDbAdapter;
import org.mapsforge.applications.android.samples.whitewater.KayakInfoItem;
import org.mapsforge.applications.android.samples.whitewater.WhitewaterNode;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.view.MapView;


import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Color;
import android.graphics.RectF;
import android.util.Log;

public class KayakLayer extends Layer {
	
	private static final GraphicFactory GRAPHIC_FACTORY = AndroidGraphicFactory.INSTANCE;
	private static final int UPDATE_DISTANCE = 0;
	private static final int UPDATE_INTERVAL = 1000;
	
	private static final String TAG = "KayakLayer";
	private static final String DEFAULT_TABLE_NAME ="default";
	public  static final String DEFAULT_KAYAK_TABLE_PREFIX ="ww_kayak";
	
	
	private boolean test = true;
	// we keep Kajak info  in the database
    private TrackDbAdapter mTrackDbAdapter = null;



	private static Paint getPaint(int color, int strokeWidth, Style style) {
		Paint paint = GRAPHIC_FACTORY.createPaint();
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth);
		paint.setStyle(style);
		return paint;
	}

    private android.graphics.Bitmap mMarkerInitialAndroidBitmap;
	private boolean myKajakLayerEnabled;
	private boolean snapToLocationEnabled;
	private KayakBasicActivity mContext;
    private MapView mMapView;
    private GraphicFactory mGraphicFactory ;

	private ArrayList<KayakInfoItem> mKayakInfoItemList = null;
	
	
	private String mShortCurrentInfoTableName = DEFAULT_TABLE_NAME;
	private String mCurrentInfoTableName = "";
	private boolean mMustShowCenter  = true;

    private String mBBoxkey="";
    private int mCountRedrawsWithSameBBox = 0;

    @Override
    public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
        if (test) Log.i(TAG, "Tap Coordinates " + tapLatLong.toString());
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
        ArrayList<KayakInfoItem> listOfNodesInBB = new ArrayList<KayakInfoItem>();
        if ( mKayakInfoItemList != null){
            for (int index = 0; index <  mKayakInfoItemList.size(); index++){
                KayakInfoItem aKayakInfoItem =  mKayakInfoItemList.get(index);
                LatLong aNodePoint = aKayakInfoItem.getLatLong();
                if (aBoundingBox.contains(aNodePoint)) {
                    listOfNodesInBB.add(aKayakInfoItem);
                }
            }
        }
        Log.i(TAG," items in bb tapped " + listOfNodesInBB.size());
        if (listOfNodesInBB.size() > 0) {
            showWhitewaterInfoItemListInfoDialog(listOfNodesInBB, aBoundingBox);
        }

        return true;
    }

    private void showWhitewaterInfoItemListInfoDialog(ArrayList<KayakInfoItem> pNodesList, BoundingBox pBoundingBox) {
        int count = pNodesList.size();
        double latSpan = pBoundingBox.getLatitudeSpan();
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        formatter.format("%d nodes in BoundingBox span %f", count, latSpan);
        if (test) Log.d(TAG, formatter.toString());
        if (count == 1) {
            int index = 0;
            KayakInfoItem aKayakInfoItem = pNodesList.get(index);
            boolean update = true;
            mContext.fixedWhitewaterDialog(aKayakInfoItem,update);
        } else {
            AlertDialog aDialog = new AlertDialog.Builder(this.mContext)
                    .setTitle("Whitewater " )
                    .setMessage(" there are "+ count + " whitewater info items selectet, please increase the zoom level")
                    .setPositiveButton("OK", null)
                    .create();
            aDialog.show();
        }
    }



	/**
	 *
	 * 
	 * @param context
	 *            a reference to the application context.

	 */
	public KayakLayer(KayakBasicActivity context,GraphicFactory pGraphicFactory, MapView pMapView) {
		super();
		this.mContext = context;
        this.mMapView = pMapView;
        this.mGraphicFactory = pGraphicFactory;
        this.mBBoxkey="";
        this.mCountRedrawsWithSameBBox = 0;
		this.mCurrentInfoTableName = getCurrentInfoTableName ();
		this.mKayakInfoItemList = new ArrayList<KayakInfoItem>();
		
		mTrackDbAdapter = new TrackDbAdapter(context);
	    mTrackDbAdapter.open();
	    
	    if (mTrackDbAdapter.existsWWTable(this.mCurrentInfoTableName)) {
	    	
	    	restoreWWInfoWithThread(this.mCurrentInfoTableName);
	    	
	    } else {
	    	
	    	Log.d(TAG," no Info found: " + this.mCurrentInfoTableName);
	    	createKayakInfoTable(this.mCurrentInfoTableName);
	    }
	}
	
	
	
	

	
  public String getCurrentInfoTableName () {
	  return  DEFAULT_KAYAK_TABLE_PREFIX + "_" + mShortCurrentInfoTableName;
  }
  
  public void createKayakInfoTable(String pTableIdentifier){
	  try {
		  mTrackDbAdapter.createFixedWhitewaterTable(pTableIdentifier);
	  } catch (SQLException e ) {
		  Log.i(TAG,"error in create KayakInfoTable " + pTableIdentifier + " " + e.toString());
	  }
  }

    public int getNextEntryNumberFromCurrentTable() {
        int result = -1;
        String aTableIdentifier = getCurrentInfoTableName ();
        Cursor aKayakTableCursor = null;
        try {
            aKayakTableCursor = mTrackDbAdapter.fetchFixedWhitewaterAllEntriesNumberAndName(aTableIdentifier);
        } catch (SQLException e ){
            Log.i(TAG, "no table" + aTableIdentifier);
        }

        if (aKayakTableCursor != null) {
            result = aKayakTableCursor.getCount()+1;
            aKayakTableCursor.close();
        }
        return result;
    }
  

  
  public void restoreWWInfoWithThread(String aTableName) {
	    Log.d(TAG, "Begin restore info with thread for " + aTableName); 
		final String theTableName = aTableName;
		new Thread(new Runnable() {
			public void run() {
				String aRowId;
                String aNumberStr;
                
                String aType;
                String aDescription;
				String aName;
				
				String aLATStr;
				String aLONStr;
				long aUTC;
				
				final Cursor cursor = mTrackDbAdapter.fetchFixedWhitewaterTable(theTableName);
				int count = 0;
				if (test && cursor != null) {
					count = cursor.getCount();
					Log.i(TAG,theTableName +  " number of infos to restore: " + count);
				}
				/* 
			    KEY_WW_NUMBER      = "ww_number"	;
			    KEY_WW_TYPE        = "ww_type";
			    KEY_WW_DESCRIPTION = "ww_description";
			    KEY_WW_NAME 	   = "ww_name";
			    KEY_UTC
			    KEY_LAT
			    KEY_LON
			   */  
				if ((cursor != null) && (cursor.getCount() > 0)) {
					cursor.moveToFirst();
					aRowId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
					aNumberStr= cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_NUMBER));
					aType = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_TYPE));
					aName = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_NAME));
					aDescription = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_DESCRIPTION));
					aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
					aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
					aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
                    int aNumber = Integer.parseInt(aNumberStr);
                    double lat = Double.parseDouble(aLATStr);
                    double lon = Double.parseDouble(aLONStr);
                    LatLong aLatLong = new LatLong(lat, lon);
                    KayakInfoItem aKayakInfoItem = new KayakInfoItem (aLatLong,aNumber,aType,aName, aDescription, aUTC);
                    mKayakInfoItemList.add(aKayakInfoItem);
					while (cursor.moveToNext()) {
						aRowId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
						aNumberStr= cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_NUMBER));
						aType = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_TYPE));
						aName = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_NAME));
						aDescription = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_DESCRIPTION));
						aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
						aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
						aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
						aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));

						aNumber = Integer.parseInt(aNumberStr);
						lat = Double.parseDouble(aLATStr);
						lon = Double.parseDouble(aLONStr);
						aLatLong = new LatLong(lat, lon);

                        if (test) {
                            Log.i(TAG, "restore next item id= "+ aRowId + " number " + aNumber + " type " + aType);
                        }
						aKayakInfoItem = new KayakInfoItem (aLatLong,aNumber,aType,aName, aDescription, aUTC);
						mKayakInfoItemList.add(aKayakInfoItem);

					} // while
				} // cursor!= null && cursor.getCount>0
				if (cursor != null)
					cursor.close();
				
				 Log.d(TAG, "End restore kayak info with thread for " + theTableName); 
			} // end of run
		}).start();
		Log.d(TAG, "Thread started to restore track with thread for " + theTableName); 
 } // restoreInfoWithTread


    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        //public void draw(BoundingBox arg0, byte arg1, Canvas arg2, Point arg3) {
        if (mMustShowCenter) {
            int aWidth = canvas.getWidth();
            int aHeight = canvas.getHeight();
            Point aP = new Point(aWidth / 2, aHeight / 2);
            // projection.toPoint(drawPosition, aPixelPoint, drawZoomLevel);

            Paint aPaint = getPaint(Color.RED, 5, Style.STROKE);
            canvas.drawCircle(aWidth / 2, aHeight / 2, 5, aPaint);

        }
        drawWhitewaterInfosOnLayer(boundingBox,zoomLevel, canvas, topLeftPoint);
        // we check if we have a new bounding box of the display
        String aBBoxkey = boundingBox.toString();
        if (!aBBoxkey.equals(mBBoxkey)) {
            mBBoxkey = aBBoxkey;
            mCountRedrawsWithSameBBox = 0;

        } else {
            mCountRedrawsWithSameBBox++;
            if (test) Log.i(TAG, "drawing with same BBBox " + mCountRedrawsWithSameBBox) ;
        }


        // TODO Auto-generated method stub

    }

    private void drawWhitewaterInfosOnLayer(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint){
        if (mKayakInfoItemList != null){
            int count = mKayakInfoItemList.size();
            if (test) {
                Log.i(TAG,"number if items in list " + count);
                for (int index=0;index < count;index++){
                    KayakInfoItem aKayakInfoItem = mKayakInfoItemList.get(index);
                    Log.i(TAG,"node "+index +" " + aKayakInfoItem.getType() + ", " + aKayakInfoItem.getName() +", " + aKayakInfoItem.getDescription());
                }
            }
            for (int index=0;index < count;index++){
                KayakInfoItem aKayakInfoItem = mKayakInfoItemList.get(index);
                drawWhitewaterItemOnlayer(aKayakInfoItem,boundingBox,zoomLevel, canvas, topLeftPoint);

            }
        }
    }

    private void drawWhitewaterItemOnlayer(KayakInfoItem pKayakInfoItem, BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint){
        int tileSize =this.displayModel.getTileSize();
        long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
        Paint aPaint = getPaint(Color.BLUE,5,Style.STROKE);

        double aLat = pKayakInfoItem.getLAT();
        double aLon = pKayakInfoItem.getLON();
        if (test) {
            Log.i(TAG,"paint circle with lon= " + aLon + " lat= " +aLat);
        }
        double x  = MercatorProjection.longitudeToPixelX(aLon,mapSize);
        double y = MercatorProjection.latitudeToPixelY(aLat,zoomLevel, tileSize);
        x = x - topLeftPoint.x;
        y = y - topLeftPoint.y;
        int aCircleX = (int)x;
        int aCircleY = (int)y;
        if (test) {
            Log.i(TAG,"paint circle with x= " + aCircleX + " y= " +aCircleY);
        }
        canvas.drawCircle(aCircleX,aCircleY,20,aPaint);

    }


    public void deleteCurrentWhitewaterTable(){
        mKayakInfoItemList.clear();
        mTrackDbAdapter.deleteFixedWhiteWaterTable(getCurrentInfoTableName());
    }

    public void putNewFixedWhitewaterPositionIntoDatabase(KayakInfoItem pKayakInfoItem){
        if (pKayakInfoItem!= null){
            // public long insertPointToFixedWhitewaterTable(String pIdentifier, String pNumberStr,String pTypeStr, String pName,
            //String pDescription, String pUTC, String pLATStr, String pLONStr)
            long aItemId =  mTrackDbAdapter.insertPointToFixedWhitewaterTable(getCurrentInfoTableName(),
                    Integer.toString(pKayakInfoItem.getNumber()),
                    pKayakInfoItem.getType(),
                    pKayakInfoItem.getName(),
                    pKayakInfoItem.getDescription(),
                    Double.toString(pKayakInfoItem.getUTC()),
                    Double.toString(pKayakInfoItem.getLAT()),
                    Double.toString(pKayakInfoItem.getLON()));
            pKayakInfoItem.setId(aItemId);
            mKayakInfoItemList.add(pKayakInfoItem);
        }
    }

    public void updateWhitewaterPositionIntoDatabase(KayakInfoItem pKayakInfoItem){
        if (pKayakInfoItem!= null){

            boolean result =  mTrackDbAdapter.updatePointInFixedWhitewaterTable(getCurrentInfoTableName(),
                    pKayakInfoItem.getId(),
                    Integer.toString(pKayakInfoItem.getNumber()),
                    pKayakInfoItem.getType(),
                    pKayakInfoItem.getName(),
                    pKayakInfoItem.getDescription(),
                    Double.toString(pKayakInfoItem.getUTC()),
                    Double.toString(pKayakInfoItem.getLAT()),
                    Double.toString(pKayakInfoItem.getLON()));
        }
    }
  

}
