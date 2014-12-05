package org.mapsforge.applications.android.samples.seamark;
/**
 * Copyright 2012 V.Klein
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
import android.util.Log;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.util.LatLongUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by vkandroidstudioadm on 27.02.14.
 */
public class SeamarkNode {
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

    public SeamarkNode(String nodeNumber){
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

    public void clear() {
        mTagDictionary.clear();
        mTagList.clear();
    }

    public String getShapeIdentifier(){
        if (mBaseShapeIdentifier == null) {
            mBaseShapeIdentifier = calculateShapeIdentifier(this);

        }
        return mBaseShapeIdentifier;
    }

    /**
     * Create aShapeIdentifier for some popular seamarks which have a shape (buoys, beacons ..)
     * if no shape is given , return null
     * @param pSeamarkNode
     * @return
     */

    private  String calculateShapeIdentifier (SeamarkNode pSeamarkNode) {
        String result = null;
        String aIdentifier = "";
        String aTypeIdentifier="";
        String aKeyValDivider = "=";
        String aPropDivider = "!!!";
        int countTags = pSeamarkNode.getTagListSize();
        if (countTags== 0){
            return result;
        }
        try {

            for (int index=0; index < countTags; index++){
                Tag aTag = pSeamarkNode.getTag(index);
                if (aTag.key.equals("seamark:type")){
                    aIdentifier =  "type" + aKeyValDivider + aTag.value;
                    mBaseTypeIdentifier = aIdentifier;
                    aTypeIdentifier = aTag.value;
                    break;
                }

            }
        } catch (NullPointerException e){
            Log.d(TAG,"no tags available " + e.toString());
        }
        if (mBaseTypeIdentifier == null) {
            // no type tag found
            return null;
        }
	  	/*
	  	 * if (aTypeIdentifier.equals("bridge")) {
	  	for (int index=0; index < countTags; index++){
			  Tag aTag = pSeamarkNode.getTag(index);
			  if (aTag.key.equals("seamark:"+aTypeIdentifier + ":category")){
				  //aIdentifier = aIdentifier + aPropDivider + aTag.key + aPropDivider + aTag.value;
				  aIdentifier = aIdentifier + aPropDivider + "category" + aKeyValDivider + aTag.value;
			  }
	  	    }
	  	}*/

	  	/*if (aTypeIdentifier.equals("notice")) {
	  	for (int index=0; index < countTags; index++){
			  Tag aTag = pSeamarkNode.getTag(index);
			  if (aTag.key.equals("seamark:"+aTypeIdentifier + ":function")){
				  //aIdentifier = aIdentifier + aPropDivider + aTag.key + aPropDivider + aTag.value;
				  aIdentifier = aIdentifier + aPropDivider + "function" + aKeyValDivider + aTag.value;
			  }
	  	    }
	  	}


	  	else */
        if (aTypeIdentifier.equals("mooring")) {
            for (int index=0; index < countTags; index++){
                Tag aTag = pSeamarkNode.getTag(index);
                if (aTag.key.equals("seamark:"+aTypeIdentifier + ":category")){
                    //aIdentifier = aIdentifier + aPropDivider + aTag.key + aPropDivider + aTag.value;
                    aIdentifier = aIdentifier + aPropDivider + "category" + aKeyValDivider + aTag.value;
                }
            }
        } else {
            boolean aShapeWasFound = false;
            boolean aColourWasFound = false;
            for (int index=0; index < countTags; index++){
                Tag aTag = pSeamarkNode.getTag(index);
                if (aTag.key.equals("seamark:"+aTypeIdentifier + ":shape")){
                    //aIdentifier = aIdentifier + aPropDivider + aTag.key + aPropDivider + aTag.value;
                    aIdentifier = aIdentifier + aPropDivider + "shape" + aKeyValDivider + aTag.value;
                    aShapeWasFound = true;

                }
            }
            if (aShapeWasFound){
                for (int index=0; index < countTags; index++){
                    Tag aTag = pSeamarkNode.getTag(index);
                    if  (aTag.key.equals("seamark:"+aTypeIdentifier +":colour")) {
                        aIdentifier = aIdentifier + aPropDivider + "colour" + aKeyValDivider + aTag.value;
                        aColourWasFound= true;

                    }

                }
            }
            if (aColourWasFound){
                for (int index=0; index < countTags; index++){
                    Tag aTag = pSeamarkNode.getTag(index);
                    if (aTag.key.equals("seamark:"+aTypeIdentifier +":colour_pattern")) {
                        aIdentifier = aIdentifier + aPropDivider + "colour_pattern" + aKeyValDivider + aTag.value;

                    }

                }
            }

            boolean topmarkShapeFound= false;
            for (int index=0; index < countTags; index++){
                Tag aTag = pSeamarkNode.getTag(index);
                if  (aTag.key.equals("seamark:daymark:shape")) {
                    aIdentifier = aIdentifier + aPropDivider + "daymark_shape" + aKeyValDivider + aTag.value;
                    topmarkShapeFound= false;
                }

                if  (aTag.key.equals("seamark:topmark:shape")) {
                    aIdentifier = aIdentifier + aPropDivider + "topmark_shape" + aKeyValDivider + aTag.value;
                    topmarkShapeFound= false;
                }
            }
            if (topmarkShapeFound){
                for (int index=0; index < countTags; index++){
                    Tag aTag = pSeamarkNode.getTag(index);

                    if  (aTag.key.equals("seamark:daymark:colour")) {
                        aIdentifier = aIdentifier + aPropDivider + "daymark_colour" + aKeyValDivider + aTag.value;
                    }

                    if  (aTag.key.equals("seamark:topmark:colour")) {
                        aIdentifier = aIdentifier + aPropDivider + "topmark_colour" + aKeyValDivider + aTag.value;
                    }

                }
            }

            for (int index=0; index < countTags; index++){
                Tag aTag = pSeamarkNode.getTag(index);
                if (aTag.key.equals("seamark:light:colour")) {
                    aIdentifier = aIdentifier + aPropDivider + "light_color" + aKeyValDivider + aTag.value;
                }

            }
        } // else
        //Log.d(TAG,aIdentifier + " " + aIdentifier.length());
        if (!aIdentifier.equals(mBaseTypeIdentifier)){
            // we have more properties that the type
            //Log.d(TAG,aIdentifier + " " + aIdentifier.length());

            result = aIdentifier;
        }else {
            result = null;
        }
        return result;
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



}
