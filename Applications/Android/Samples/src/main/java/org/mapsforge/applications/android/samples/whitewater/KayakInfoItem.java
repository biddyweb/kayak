package org.mapsforge.applications.android.samples.whitewater;



import org.mapsforge.core.model.LatLong;

public class KayakInfoItem {
	private long mId;
	private int mNumber;
	private String mType;
	private String mName;
	private String mDescription;
	private double mUTC;
	private LatLong mLatLong;
	
	/*
    + KEY_WW_NUMBER  
    + KEY_WW_TYPE 
	+ KEY_WW_NAME 
	+ KEY_WW_DESCRIPTION 
	+ KEY_UTC 
	+ KEY_LAT    
    + KEY_LON 
*/
    /**
     * 
     * @param pPoint
     * @param pNumber
     * @param pTypeStr
     * @param pName
     * @param pDescription
     * @param pUTC
     */
	public KayakInfoItem(LatLong pPoint, int pNumber,String pTypeStr,
			              String pName ,  String pDescription , long pUTC) {

		mNumber = pNumber;
		mDescription = pDescription;
		mId = -1; // will be set from the fixed_buoy database to have a reference for updates
		mType = pTypeStr;
		mName = pName;
		mUTC = pUTC;
		mLatLong  = pPoint;
	}

	public long getId() {
		return mId;
	}
	public void setId(long pId){
		mId = pId;
	}
	
	public int getNumber() {
		return mNumber;
	}
	public String getDescription() {
		return mDescription;
	}
	
	public void setDescription(String pDescription){
		mDescription = pDescription;
	}

	public String getType() {
		return mType;
	}
	
	public void setType(String pTypeStr){
		mType = pTypeStr;
	}
	
	
	public String getName() {
		return mName;
	}

	public void setName(String pNameStr){
		mName = pNameStr;
	}
	
	public double getUTC() {
		return mUTC;
	}
	
	public void setUTC(long pUTC){
		mUTC = pUTC;
	}


	public double getLON() {
		return mLatLong.longitude;
	}
	
	public double getLAT() {
		return mLatLong.latitude;
	}
	
	public void setLatLong( LatLong pLatLong) {
		mLatLong = pLatLong;
	}

    public  LatLong getLatLong() {
        return new LatLong(mLatLong.latitude,mLatLong.longitude);
    }
	public void setLON(double pLON) {
		LatLong aNewLatLong = new LatLong (mLatLong.latitude,pLON);
		mLatLong= aNewLatLong;
	}
	
	public void setLAT(double pLAT) {
		LatLong aNewLatLong = new LatLong(pLAT,mLatLong.longitude);
		mLatLong= aNewLatLong;
	}
}
