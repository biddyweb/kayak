package org.mapsforge.applications.android.samples.whitewater;


import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by vkmapsforge05 on 04.12.2014.
 */
public class WhitewaterWay {
    private String mId;
    private ArrayList<WhitewaterNode> mNodesList;
    private ArrayList<Tag> mTagList;
    private LinkedHashMap<String,String> mTagDictionary;

    public WhitewaterWay(String id){
        mId = id;
        mNodesList = new ArrayList<WhitewaterNode>();
        mTagDictionary = new LinkedHashMap<String,String>();
        mTagList = new ArrayList<Tag>();
    }

    public String getId() {
        return mId;
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
    public ArrayList<WhitewaterNode> getNodeList() {
        return mNodesList;
    }

    public void addNode(WhitewaterNode aNode){
        mNodesList.add(aNode);
    }

    public boolean belongsToBoundingBox (BoundingBox boundingBox) {
        boolean result = false;
        int countNodes = mNodesList.size();
        for (int index = 0; index < countNodes; index++){
            WhitewaterNode aNode = mNodesList.get(index);
            LatLong geoPoint = new LatLong(aNode.getLatitudeE6()/1E6,aNode.getLongitudeE6()/1E6);
            if (boundingBox.contains(geoPoint)){
                result = true;
                break;
            }
        }
        return result;
    }
}
