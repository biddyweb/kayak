package org.mapsforge.applications.android.samples.whitewater;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.util.LatLongUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by vkmapsforge05 on 04.12.2014.
 */
public class WhitewaterNode {

    private static final String TAG ="SeamarkNode";
    private String mNodeNumber;
    private int  mLatitudeE6 ;
    private int mLongitudeE6;
    private double mLatitude;
    private double mLongitude;
    private ArrayList<Tag> mTagList;
    private LinkedHashMap<String,String> mTagDictionary;
    private String mBaseShapeIdentifier = null;
    private String mBaseTypeIdentifier = "";


    private boolean mIsVisible = false;

    public WhitewaterNode (String nodeNumber){
        mNodeNumber = nodeNumber;
        mTagDictionary = new LinkedHashMap<String,String>();
        mTagList = new ArrayList<Tag>();

    }

    public String getId() {
        return mNodeNumber;
    }


    public void setVisibility(boolean visible){
        mIsVisible= visible;
    }
    public boolean getVisibility() {
        return mIsVisible;
    }


    public void setLatitudeE6( int lat) {
        mLatitudeE6 = lat;
        mLatitude = LatLongUtils.microdegreesToDegrees(lat);
    }

    public int getLatitudeE6 (){
        return mLatitudeE6;
    }

    public void setLongitudeE6 (int lon) {
        mLongitudeE6 = lon;
        mLongitude = LatLongUtils.microdegreesToDegrees(lon);
    }

    public int getLongitudeE6 (){
        return mLongitudeE6;
    }

    public LatLong getLatLong() {
        return new LatLong(mLatitude,mLongitude);
    }

    public void setLatLong ( LatLong latLong){
        mLongitude  = latLong.longitude;
        mLatitude = latLong.latitude;
        mLatitudeE6 = LatLongUtils.degreesToMicrodegrees(mLatitude);
        mLongitudeE6 = LatLongUtils.degreesToMicrodegrees(mLongitude);
    }

    public void addTag(Tag tag) {
        String key = tag.key;
        String value = tag.value;
        mTagDictionary.put(key, value);
        mTagList.add(tag);

    }

    public Tag getTag(int index) {
        Tag result = null;
        if (index > -1 && index < mTagList.size()) {
            result = mTagList.get(index);
        }
        return result;
    }

    public int getTagListSize(){
        return mTagList.size();
    }

    public String getValueToKey(String key){
        String result = null;
        if (mTagDictionary.containsKey(key)){
            result = mTagDictionary.get(key);
        }
        return result;
    }

    public LinkedHashMap<String,String> getTagDictionary() {
        return mTagDictionary;
    }

    public String tagsToString() {
        String result = "";
        StringBuffer buf = new StringBuffer();
        for (int index=0;index < mTagList.size(); index++ ){
            Tag aTag = (Tag) mTagList.get(index);
            buf.append(aTag.toString()) ;
            buf.append("\n");
        }
        result = buf.toString();
        return result;
    }

    public void clear() {
        mTagDictionary.clear();
        mTagList.clear();
    }

}
