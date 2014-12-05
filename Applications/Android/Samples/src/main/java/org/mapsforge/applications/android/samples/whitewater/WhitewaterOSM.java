package org.mapsforge.applications.android.samples.whitewater;

import android.content.Context;
import android.util.Log;


import org.mapsforge.applications.android.samples.seamark.SeamarkNode;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by vkmapsforge05 on 04.12.2014.
 */
public class WhitewaterOSM {

    private static final String TAG = "WhitewaterOSM";
    private static final boolean test = true;
    private Context mContext;
    private File mWhitewaterFile = null;
    private ArrayList<String> mStringList = null; // not in use only for loadDataOld 12_11_30
    private boolean mFileReadHasFinished = false;
    private ArrayList<WhitewaterNode> mWhitewaterNodesList;
    private LinkedHashMap<String,WhitewaterNode> mWhitewaterDictionary;
    private LinkedHashMap<String,WhitewaterNode> mWayNodesDictionary;
    //private ArrayList<NavigationLine> mNavigationLinesList;
    private  ArrayList<WhitewaterWay> mWhitewaterWaysList;

    private BoundingBox mBoundingBox;
    private LinkedHashMap <Tile,ArrayList<WhitewaterNode>> mTileDictionary;
    private byte mZoomForLookUp = 11;
    private boolean mTileDictionaryIsReady ;

    public WhitewaterOSM ( Context context){
        mContext = context;
        mStringList = new ArrayList<String>(); // not in use only for  loadDataOld 12_11_30
        mFileReadHasFinished = false;
        mWhitewaterNodesList = new ArrayList<WhitewaterNode>();
        mWhitewaterDictionary = new LinkedHashMap<String,WhitewaterNode>();
        mWayNodesDictionary = new LinkedHashMap<String,WhitewaterNode>();
        //mNavigationLinesList = new ArrayList<NavigationLine>();
        mWhitewaterWaysList = new ArrayList<WhitewaterWay>();

        mBoundingBox = null;
        mTileDictionary = new LinkedHashMap<Tile,ArrayList<WhitewaterNode>>();
        mTileDictionaryIsReady = false;
        mZoomForLookUp = 11;
    }


    public void readWhitewaterFile (String aPath){
        if (test) {
            Log.d(TAG, "Scanning Whitewater file: " + aPath);
        }
        File aFile = new File (aPath);
        loadDataWithThread(aFile);

    }

    public boolean getWhitewaterFileReadComplete() {
        return mFileReadHasFinished;
    }

     /* public ArrayList<String> getWhitewatersAsStringList(){  / 12_11_30
    	 if (mFileReadHasFinished) return mStringList;
    	 return null;
      }*/

    public ArrayList<WhitewaterNode> getWhitewatersAsArrayList(){
        if (mFileReadHasFinished) return mWhitewaterNodesList;
        return null;
    }

    public LinkedHashMap<String,WhitewaterNode> getWhitewatersAsDictionary(){
        if (mFileReadHasFinished) return mWhitewaterDictionary;
        return null;
    }

     /* public ArrayList<NavigationLine> getNavigationLinesAsArrayList(){
          if (mFileReadHasFinished) return mNavigationLinesList ;
        return null;
     }*/

    public ArrayList<WhitewaterWay> getWhitewaterWaysAsArrayList() {
        if (mFileReadHasFinished) return mWhitewaterWaysList ;
        return null;
    }

    public BoundingBox getBoundingBox() {
        if(mBoundingBox != null) return mBoundingBox;
        return null;
    }

    private BoundingBox getBoundingBoxToTile (Tile tile){
        byte zoomLevel = tile.zoomLevel;
        double latMax = MercatorProjection.tileYToLatitude(tile.tileY, zoomLevel);
        double lonMin = MercatorProjection.tileXToLongitude(tile.tileX, zoomLevel);
        double latMin = MercatorProjection.tileYToLatitude(tile.tileY + 1, zoomLevel);
        double lonMax = MercatorProjection.tileXToLongitude(tile.tileX + 1, zoomLevel);
        BoundingBox tileBoundingBox = new BoundingBox(latMin,lonMin,latMax,lonMax);
        return tileBoundingBox;
    }

    public ArrayList<WhitewaterNode> getWhitewaterNodesToTile (Tile aTile){
        if (!mTileDictionaryIsReady) return null;
        if (aTile.zoomLevel == mZoomForLookUp) {
            if (mTileDictionary.containsKey(aTile) ) {
                return mTileDictionary.get(aTile);
            }
        } else {
            // we must calculate
            if (aTile.zoomLevel < mZoomForLookUp) {
                // zoom > , we must concat some lists
            } else {
                // we go up to find a patent tile
                Tile searchTile = aTile;
                ArrayList<WhitewaterNode> parentList = null;
                while (mTileDictionary.get(searchTile)== null && searchTile.zoomLevel >=mZoomForLookUp ){
                    // look at the parent tile
                    searchTile = searchTile.getParent();
                    parentList = mTileDictionary.get(searchTile);
                }
                if (parentList != null) {
                    BoundingBox bb = getBoundingBoxToTile(aTile);
                    ArrayList<WhitewaterNode> tileNodeList = new ArrayList<WhitewaterNode>();
                    for (int index = 0; index < parentList.size(); index ++){
                        WhitewaterNode aNode = parentList.get(index);
                        LatLong latLong = aNode.getLatLong();
                        if (bb.contains(aNode.getLatLong())){
                            tileNodeList.add(aNode);
                        }
                    }
                    return tileNodeList;
                }

            }
        }
        return null;
    }

    public byte getZoomLevelForLookUp() {
        return mZoomForLookUp;
    }


    public void loadDataWithThread(File aFile) {

        final String aFilename = aFile.getAbsolutePath();
        Log.d(TAG, "Reading Whitewatersfile: " + aFilename);
        new Thread(new Runnable() {

            public void run() {

                try {

                    XmlPullParserFactory parserCreator;

                    parserCreator = XmlPullParserFactory.newInstance();

                    XmlPullParser parser = parserCreator.newPullParser();
                    FileReader myReader = new FileReader(aFilename);
                    parser.setInput(myReader);
                    // parser.setInput(text.openStream(), null);

                    int parserEvent = parser.getEventType();
                    long nodeCount = 0;
                    long WhitewaterNodeCount = 0;
                    long wayCount = 0;
                    long relationCount = 0;
                    long WhitewaterWayCount = 0;
                    long refToNodesCount = 0;
                    String aId = "";
                    String aWayId ="";
                    String aNodeStr="";

                    WhitewaterNode currentWhitewaterNode = null;
                    boolean WhitewaterNodeFound = false;
                    //NavigationLine currentNavLine = null;
                    //boolean navLineFound= false;
                    WhitewaterWay currentWhitewaterWay = null;
                    boolean WhitewaterWayFound = false;

                    // Parse the XML returned from the file
                    while (parserEvent != XmlPullParser.END_DOCUMENT) {
                        switch (parserEvent) {
                            case XmlPullParser.START_TAG:
                                String tag = parser.getName();
                                if (tag.compareTo("bounds")== 0){
                                    String minlatStr = parser.getAttributeValue(null,"minlat");
                                    String minlonStr = parser.getAttributeValue(null, "minlon");
                                    String maxlatStr = parser.getAttributeValue(null, "maxlat");
                                    String maxlonStr = parser.getAttributeValue(null, "maxlon");
                                    double minlat = (Double.parseDouble(minlatStr));
                                    double minlon = (Double.parseDouble(minlonStr));
                                    double maxlat = (Double.parseDouble(maxlatStr));
                                    double maxlon = (Double.parseDouble(maxlonStr));
                                    if (mBoundingBox == null ) mBoundingBox = new BoundingBox(minlat,minlon,maxlat,maxlon);
                                }
                                if (tag.compareTo("node") == 0) {
                                    // make a new node
                                    String aNodeNumberStr = parser.getAttributeValue(null,"id");
                                    aId = aNodeNumberStr;

                                    String aLatStr = parser.getAttributeValue(null,"lat");
                                    Double aLat = Double.parseDouble(aLatStr);
                                    int aLatE6 = (int)(aLat * 1E6);

                                    String aLonStr = parser.getAttributeValue(null,"lon");
                                    Double aLon = Double.parseDouble(aLonStr);
                                    int aLonE6 = (int)(aLon * 1E6);

                                    currentWhitewaterNode = new WhitewaterNode(aNodeNumberStr);
                                    currentWhitewaterNode.setLatitudeE6(aLatE6);
                                    currentWhitewaterNode.setLongitudeE6(aLonE6);
                                    mWayNodesDictionary.put(aId, currentWhitewaterNode);

                                }
                                if (tag.compareTo("tag") == 0) {
                                    int countAttr = parser.getAttributeCount();
                                    // should be <tag k="Whitewater:...." v="xyz" >
                                    for (int indexAttr = 0;2*indexAttr < countAttr; indexAttr++) {
                                        String aKeyAttributeName = parser.getAttributeName(indexAttr);
                                        String aKeyAttributeValue  = parser.getAttributeValue(indexAttr);

                                        String aValueAttributeName = parser.getAttributeName(indexAttr +1);
                                        String aValueAttributeValue  = parser.getAttributeValue(indexAttr +1);

                                        if (aKeyAttributeValue.contains("whitewater")){  // maybe we have a node or a way
                                            Tag aTag = new Tag(aKeyAttributeValue,aValueAttributeValue);
                                            if (currentWhitewaterNode != null ) { // we have a valid node
                                                currentWhitewaterNode.addTag(aTag);
                                                WhitewaterNodeFound = true;
                                            }
                                            if (currentWhitewaterWay != null) { // we have a valid Whitewater way
                                                currentWhitewaterWay.addTag(aTag);
                                            }
                                        }

                                    }

                                }
                                if(tag.compareTo("way")== 0){
                                    aWayId = parser.getAttributeValue(null,"id");
                                    // navLineFound = true;
                                    //currentNavLine = new NavigationLine(aWayId);
                                    WhitewaterWayFound = true;
                                    currentWhitewaterWay = new WhitewaterWay(aWayId);
                                    if (test) Log.d(TAG,"new way found " + aWayId);
                                }

                                if(tag.compareTo("nd")== 0){
                                    refToNodesCount++;
                                    String refToNode = parser.getAttributeValue(null,"ref");
                                    //System.out.println("ref to node " + refToNode);
                                    //if (currentNavLine != null){
                                    if (currentWhitewaterWay != null){
                                        WhitewaterNode aNode =  mWayNodesDictionary.get(refToNode);
                                        if (aNode != null){
                                            //currentNavLine.addNode(aNode);
                                            currentWhitewaterWay.addNode(aNode);
                                            // if(test) Log.d(TAG,"node " +  refToNode + " added to line " + currentNavLine.getId());
                                            if(test) Log.d(TAG,"node " +  refToNode + " added to way " + currentWhitewaterWay.getId());
                                        } else {
                                            if(test) Log.d(TAG,"node to ref " + refToNode + " not found");

                                        }
                                    }

                                }


                                break;
                            case XmlPullParser.END_TAG: {
                                tag = parser.getName();

                                if (tag.compareTo("node") == 0) {
                                    if (WhitewaterNodeFound) {
                                        WhitewaterNodeCount++;

                                        if ((WhitewaterNodeCount % 10) == 0 ){
                                            if(test)Log.d(TAG,"WhitewaterNodes " + WhitewaterNodeCount);
                                        }
                                        if (!mWhitewaterDictionary.containsKey(aId)){
                                            //Log.d(TAG,"add Node " + aId);
                                            mWhitewaterNodesList.add(currentWhitewaterNode);
                                            mWhitewaterDictionary.put(aId, currentWhitewaterNode);
                                        }
                                        //WhitewaterNodeFound = false;
                                        //currentWhitewaterNode = null;  // for the next step we begin with aWhitewaterNode = null
                                    }

                                    //Log.d(TAG,"processed " + aId);
                                    WhitewaterNodeFound = false;
                                    //mWayNodesDictionary.remove(currentWhitewaterNode);
                                    currentWhitewaterNode = null;  // for the next step we begin with aWhitewaterNode = null

                                    nodeCount++;
                                    if ((nodeCount % 100) == 0 ){
                                        if(test) Log.d(TAG,"Nodes " + nodeCount);
                                        if (test) Log.d(TAG,"WaynodeDictionary size " + mWayNodesDictionary.size());
                                    }

                                }
                                if (tag.compareTo("way")==0) {
                                    WhitewaterWayCount++;
                                    if ((WhitewaterWayCount % 10) == 0 ){
                                        if(test) Log.d(TAG,"Whitewater ways " + WhitewaterWayCount);
                                    }
                                    //if (currentNavLine != null){
                                    //	  mNavigationLinesList.add(currentNavLine);
                                    // }
                                    if (currentWhitewaterWay != null){
                                        mWhitewaterWaysList.add(currentWhitewaterWay);
                                    }
                                    //navLineFound = false;
                                    // currentNavLine = null;
                                    WhitewaterWayFound= false;
                                    currentWhitewaterWay = null;
                                }

                                if (tag.compareTo("nd")==0) {
                                }

                            }
                            break;
                        }

                        parserEvent = parser.next();
                    }

                    mFileReadHasFinished = true;
                    Log.d(TAG,"finished reading Whitewatersfile: " + aFilename);
                    //Log.d(TAG,"found " + wayCount + " navigation_lines ");
                    Log.d(TAG,"found " + nodeCount + "  nodes ");
                    Log.d(TAG,"found " + WhitewaterNodeCount + " Whitewater nodes ");
                    Log.d(TAG,"found " + WhitewaterWayCount + " Whitewater ways ");
                    Log.d(TAG,"found " + refToNodesCount + " Whitewater refToNodes ");
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found");
                } catch (Exception e) {
                    Log.i(TAG, "Failed in parsing XML", e);
                    //showToastOnUiThread(aUIMsg);

                }


            }
        }).start();

    }

    public synchronized void clear() {
        mFileReadHasFinished = false;
        Log.d(TAG,"Clear Semarks");
        mStringList.clear();
        mWhitewaterDictionary.clear();
        int count = mWhitewaterNodesList.size();
        for (int index =0;index < count;index++) {
            WhitewaterNode aWhitewaterNode = mWhitewaterNodesList.get(index);
            aWhitewaterNode.clear();
        }
        mWhitewaterNodesList.clear();
    }

}
