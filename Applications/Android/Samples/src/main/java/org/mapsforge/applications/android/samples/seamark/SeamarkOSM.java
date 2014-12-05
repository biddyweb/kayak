package org.mapsforge.applications.android.samples.seamark;

import android.content.Context;
import android.util.Log;

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
 * Created by vkandroidstudioadm on 27.02.14.
 */
public class SeamarkOSM {
    private static final String TAG = "SeamarkOSM";
    private static final boolean test = false;
    private Context mContext;
    private File mSeamarkFile = null;
    private ArrayList<String> mStringList = null; // not in use only for loadDataOld 12_11_30
    private boolean mFileReadHasFinished = false;
    private ArrayList<SeamarkNode> mSeamarksNodesList;
    private LinkedHashMap<String,SeamarkNode> mSeamarksDictionary;
    private LinkedHashMap<String,SeamarkNode> mWayNodesDictionary;
    //private ArrayList<NavigationLine> mNavigationLinesList;
    private  ArrayList<SeamarkWay> mSeamarkWaysList;

    private BoundingBox mBoundingBox;
    private LinkedHashMap <Tile,ArrayList<SeamarkNode>> mTileDictionary;
    private byte mZoomForLookUp = 11;
    private boolean mTileDictionaryIsReady ;

    public SeamarkOSM ( Context context){
        mContext = context;
        mStringList = new ArrayList<String>(); // not in use only for  loadDataOld 12_11_30
        mFileReadHasFinished = false;
        mSeamarksNodesList = new ArrayList<SeamarkNode>();
        mSeamarksDictionary = new LinkedHashMap<String,SeamarkNode>();
        mWayNodesDictionary = new LinkedHashMap<String,SeamarkNode>();
        //mNavigationLinesList = new ArrayList<NavigationLine>();
        mSeamarkWaysList = new ArrayList<SeamarkWay>();

        mBoundingBox = null;
        mTileDictionary = new LinkedHashMap<Tile,ArrayList<SeamarkNode>>();
        mTileDictionaryIsReady = false;
        mZoomForLookUp = 11;
    }


    public void readSeamarkFile (String aPath){
        if (test) {
            Log.d(TAG,"Scanning seamark file: " + aPath);
        }
        File aFile = new File (aPath);
        loadDataWithThread(aFile);

    }

    public boolean getSeamarkFileReadComplete() {
        return mFileReadHasFinished;
    }

     /* public ArrayList<String> getSeamarksAsStringList(){  / 12_11_30
    	 if (mFileReadHasFinished) return mStringList;
    	 return null;
      }*/

    public ArrayList<SeamarkNode> getSeamarksAsArrayList(){
        if (mFileReadHasFinished) return  mSeamarksNodesList ;
        return null;
    }

    public LinkedHashMap<String,SeamarkNode> getSeamarksAsDictionary(){
        if (mFileReadHasFinished) return  mSeamarksDictionary ;
        return null;
    }

     /* public ArrayList<NavigationLine> getNavigationLinesAsArrayList(){
          if (mFileReadHasFinished) return mNavigationLinesList ;
        return null;
     }*/

    public ArrayList<SeamarkWay> getSeamarkWaysAsArrayList() {
        if (mFileReadHasFinished) return mSeamarkWaysList ;
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

    public ArrayList<SeamarkNode> getSeamarkNodesToTile (Tile aTile){
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
                 ArrayList<SeamarkNode> parentList = null;
                 while (mTileDictionary.get(searchTile)== null && searchTile.zoomLevel >=mZoomForLookUp ){
                     // look at the parent tile
                     searchTile = searchTile.getParent();
                     parentList = mTileDictionary.get(searchTile);
                 }
                 if (parentList != null) {
                     BoundingBox bb = getBoundingBoxToTile(aTile);
                     ArrayList<SeamarkNode> tileNodeList = new ArrayList<SeamarkNode>();
                     for (int index = 0; index < parentList.size(); index ++){
                         SeamarkNode aNode = parentList.get(index);
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
        Log.d(TAG, "Reading seamarksfile: " + aFilename);
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
                    long seamarkNodeCount = 0;
                    long wayCount = 0;
                    long relationCount = 0;
                    long seamarkWayCount = 0;
                    long refToNodesCount = 0;
                    String aId = "";
                    String aWayId ="";
                    String aNodeStr="";

                    SeamarkNode currentSeamarkNode = null;
                    boolean seamarkNodeFound = false;
                    //NavigationLine currentNavLine = null;
                    //boolean navLineFound= false;
                    SeamarkWay currentSeamarkWay = null;
                    boolean seamarkWayFound = false;

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

                                    currentSeamarkNode = new SeamarkNode(aNodeNumberStr);
                                    currentSeamarkNode.setLatitudeE6(aLatE6);
                                    currentSeamarkNode.setLongitudeE6(aLonE6);
                                    mWayNodesDictionary.put(aId, currentSeamarkNode);

                                }
                                if (tag.compareTo("tag") == 0) {
                                    int countAttr = parser.getAttributeCount();
                                    // should be <tag k="seamark:...." v="xyz" >
                                    for (int indexAttr = 0;2*indexAttr < countAttr; indexAttr++) {
                                        String aKeyAttributeName = parser.getAttributeName(indexAttr);
                                        String aKeyAttributeValue  = parser.getAttributeValue(indexAttr);

                                        String aValueAttributeName = parser.getAttributeName(indexAttr +1);
                                        String aValueAttributeValue  = parser.getAttributeValue(indexAttr +1);

                                        if (aKeyAttributeValue.contains("seamark")){  // maybe we have a node or a way
                                            Tag aTag = new Tag(aKeyAttributeValue,aValueAttributeValue);
                                            if (currentSeamarkNode != null ) { // we have a valid node
                                                currentSeamarkNode.addTag(aTag);
                                                seamarkNodeFound = true;
                                            }
                                            if (currentSeamarkWay != null) { // we have a valid seamark way
                                                currentSeamarkWay.addTag(aTag);
                                            }
                                        }
                                        if (aKeyAttributeValue.equals("light:description")){
                                            Tag aTag = new Tag(aKeyAttributeValue,aValueAttributeValue);
                                            if (currentSeamarkNode != null ) { // we must have a valid node
                                                currentSeamarkNode.addTag(aTag);
                                            }
                                        }
                                    }

                                }
                                if(tag.compareTo("way")== 0){
                                    aWayId = parser.getAttributeValue(null,"id");
                                    // navLineFound = true;
                                    //currentNavLine = new NavigationLine(aWayId);
                                    seamarkWayFound = true;
                                    currentSeamarkWay = new SeamarkWay(aWayId);
                                    if (test) Log.d(TAG,"new way found " + aWayId);
                                }

                                if(tag.compareTo("nd")== 0){
                                    refToNodesCount++;
                                    String refToNode = parser.getAttributeValue(null,"ref");
                                    //System.out.println("ref to node " + refToNode);
                                    //if (currentNavLine != null){
                                    if (currentSeamarkWay != null){
                                        SeamarkNode aNode =  mWayNodesDictionary.get(refToNode);
                                        if (aNode != null){
                                            //currentNavLine.addNode(aNode);
                                            currentSeamarkWay.addNode(aNode);
                                            // if(test) Log.d(TAG,"node " +  refToNode + " added to line " + currentNavLine.getId());
                                            if(test) Log.d(TAG,"node " +  refToNode + " added to way " + currentSeamarkWay.getId());
                                        } else {
                                            if(test) Log.d(TAG,"node to ref " + refToNode + " not found");

                                        }
                                    }

                                }


                                break;
                            case XmlPullParser.END_TAG: {
                                tag = parser.getName();

                                if (tag.compareTo("node") == 0) {
                                    if (seamarkNodeFound) {
                                        seamarkNodeCount++;

                                        if ((seamarkNodeCount % 10) == 0 ){
                                            if(test)Log.d(TAG,"SeamarkNodes " + seamarkNodeCount);
                                        }
                                        if (!mSeamarksDictionary.containsKey(aId)){
                                            //Log.d(TAG,"add Node " + aId);
                                            mSeamarksNodesList.add(currentSeamarkNode);
                                            mSeamarksDictionary.put(aId, currentSeamarkNode);
                                        }
                                        //seamarkNodeFound = false;
                                        //currentSeamarkNode = null;  // for the next step we begin with aSeamarkNode = null
                                    }

                                    //Log.d(TAG,"processed " + aId);
                                    seamarkNodeFound = false;
                                    //mWayNodesDictionary.remove(currentSeamarkNode);
                                    currentSeamarkNode = null;  // for the next step we begin with aSeamarkNode = null

                                    nodeCount++;
                                    if ((nodeCount % 100) == 0 ){
                                        if(test) Log.d(TAG,"Nodes " + nodeCount);
                                        if (test) Log.d(TAG,"WaynodeDictionary size " + mWayNodesDictionary.size());
                                    }

                                }
                                if (tag.compareTo("way")==0) {
                                    seamarkWayCount++;
                                    if ((seamarkWayCount % 10) == 0 ){
                                        if(test) Log.d(TAG,"seamark ways " + seamarkWayCount);
                                    }
                                    //if (currentNavLine != null){
                                    //	  mNavigationLinesList.add(currentNavLine);
                                    // }
                                    if (currentSeamarkWay != null){
                                        mSeamarkWaysList.add(currentSeamarkWay);
                                    }
                                    //navLineFound = false;
                                    // currentNavLine = null;
                                    seamarkWayFound= false;
                                    currentSeamarkWay = null;
                                }

                                if (tag.compareTo("nd")==0) {
                                }

                            }
                            break;
                        }

                        parserEvent = parser.next();
                    }

                    mFileReadHasFinished = true;
                    Log.d(TAG,"finished reading seamarksfile: " + aFilename);
                    //Log.d(TAG,"found " + wayCount + " navigation_lines ");
                    Log.d(TAG,"found " + nodeCount + "  nodes ");
                    Log.d(TAG,"found " + seamarkNodeCount + " seamark nodes ");
                    Log.d(TAG,"found " + seamarkWayCount + " seamark ways ");
                    Log.d(TAG,"found " + refToNodesCount + " seamark refToNodes ");
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found");
                } catch (Exception e) {
                    Log.i(TAG, "Failed in parsing XML", e);
                    //showToastOnUiThread(aUIMsg);

                }


            }
        }).start();

    }

    public void spreadSeamarksOnTiles(int tileSize) {
        if (mFileReadHasFinished && mBoundingBox != null){
            byte zoomLevel = mZoomForLookUp;  // fixed to 11
            int countNodes =  mSeamarksNodesList.size();
            Log.i(TAG,"nr of seamarks " + countNodes);
            for (int index = 0; index < countNodes; index ++ ){
                SeamarkNode aSeamarkNode = mSeamarksNodesList.get(index);
                int tileX = MercatorProjection.longitudeToTileX((aSeamarkNode.getLongitudeE6() / 1E6d), zoomLevel);
                int tileY = MercatorProjection.latitudeToTileY((aSeamarkNode.getLatitudeE6() / 1E6d), zoomLevel);
                Tile aTile = new Tile(tileX,tileY,zoomLevel,tileSize);
                if (mTileDictionary.containsKey(aTile)) {
                    ArrayList<SeamarkNode> aSeamarkNodeList = mTileDictionary.get(aTile);
                    aSeamarkNodeList.add(aSeamarkNode);
                } else {
                    ArrayList<SeamarkNode> aSeamarkNodeList = new ArrayList<SeamarkNode>();
                    aSeamarkNodeList.add(aSeamarkNode);
                    mTileDictionary.put(aTile,aSeamarkNodeList);
                }
            }

            int countTilesInDictionary = mTileDictionary.size();
            Log.i(TAG,"Number of Tiles: " + countTilesInDictionary);
            int sum = 0;
            for ( Tile aTile : mTileDictionary.keySet()){
                if (test)Log.i(TAG, "Tile tileX=" + aTile.tileX + " tileY=" + aTile.tileY + " zoom=" + aTile.zoomLevel);
                ArrayList<SeamarkNode> aSeamarkNodeList = mTileDictionary.get(aTile);
                if (test) Log.i(TAG,"nr of seamarks in this tile " + aSeamarkNodeList.size());
                if (test)Log.i(TAG,"");
                sum = sum + aSeamarkNodeList.size();
            }
            Log.i(TAG,"Sum for all tiles " + sum);
            mTileDictionaryIsReady = true;
        }
    }


    public synchronized void clear() {
        mFileReadHasFinished = false;
        Log.d(TAG,"Clear Semarks");
        mStringList.clear();
        mSeamarksDictionary.clear();
        int count = mSeamarksNodesList.size();
        for (int index =0;index < count;index++) {
            SeamarkNode aSeamarkNode = mSeamarksNodesList.get(index);
            aSeamarkNode.clear();
        }
        mSeamarksNodesList.clear();
    }


}
