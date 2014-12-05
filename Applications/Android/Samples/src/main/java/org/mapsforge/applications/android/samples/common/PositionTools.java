package org.mapsforge.applications.android.samples.common;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class PositionTools {
	
	private static final String TAG = "PositionTools";
	private static final boolean test = false;
	 public static final FileFilter FILE_FILTER_EXTENSION_BACKUPSER = new FilterByFileExtension(".backupser");
	
	/**
	 * 
	 * @param aPos aPosition in degrees
	 * @return  a String formatted 10' 23,666 E/W
	 */
	public static String getLONString(double aPos) {
		  double grad = Math.floor(aPos);
		  double minutes = (aPos - grad) * 60;
		  StringBuffer sb = new StringBuffer();
		  sb.append(customFormat ("000" , grad));
		  sb.append("\' ");
		  sb.append(customFormat ("00.000",minutes));
		  //sb.append("\''");
		  // String aPosStr = format("%.0f", grad) + "� " +format("%2.3f",minutes);
		  if (aPos > 0) {
			  sb.append(" E");
		  } else {
			  sb.append(" W");
		  }
		  return sb.toString();
	  }
	
	/**
	 * 
	 * @param aPos aPosition in degrees
	 * @return a String formatted 54' 23,666 N/S
	 */
	public static String getLATString(double aPos) {
		  double grad = Math.floor(aPos);
		  double minutes = (aPos - grad) * 60;
		  StringBuffer sb = new StringBuffer();
		  sb.append(customFormat ("00" , grad));
		  sb.append("\' ");
		  sb.append(customFormat ("00.000",minutes));
		 // sb.append("\''");
		  // String aPosStr = format("%.0f", grad) + "� " +format("%2.3f",minutes);
		  if (aPos > 0) {
			  sb.append(" N");
		  } else {
			  sb.append(" S");
		  }
		  return sb.toString();
	  }
	
	/**
	 * 
	 * @param pattern  use a pattern like "000.00"
	 * @param value    the value to convert  45.34523
	 * @return  aString with the value formatted  045.34
	 */
	public static String customFormat(String pattern, double value ) {
	      DecimalFormat myFormatter = new DecimalFormat(pattern);
	      String output = myFormatter.format(value);
	      return output;
	  }
	
	
	public static String getTimeString(long aUTC) {
		Date aDate = new Date(aUTC);
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.setTime(aDate);
        StringBuffer aTime = new StringBuffer();
        aTime.append(customFormat("00",cal.get(Calendar.HOUR_OF_DAY)));
        aTime.append(":");
        aTime.append(customFormat("00",cal.get(Calendar.MINUTE)));
        aTime.append(":");
        aTime.append(customFormat("00",cal.get(Calendar.SECOND)));
        return aTime.toString();
	}
	

	


	
	// the following is needed to save the data to the sd-card
	public static String getCurrentDateTimeForFilename() {
		final Calendar c = Calendar.getInstance();
		int mYear = c.get(Calendar.YEAR);
		if (mYear > 2000)
			mYear = mYear - 2000;
		int mMonth = c.get(Calendar.MONTH);
		int mDay = c.get(Calendar.DAY_OF_MONTH);
		int mHour = c.get(Calendar.HOUR_OF_DAY);
		int mMinute = c.get(Calendar.MINUTE);
		int mSecond = c.get(Calendar.SECOND);
		StringBuffer buf = new StringBuffer();
		buf.append(customFormat("00", mYear));
		buf.append("_");
		buf.append(customFormat("00", mMonth + 1));
		buf.append("_");
		buf.append(customFormat("00", mDay));
		buf.append("_");
		buf.append(customFormat("00", mHour));
		buf.append("_");
		buf.append(customFormat("00", mMinute));
		buf.append("_");
		buf.append(customFormat("00", mSecond));
		String aDateStr = buf.toString();
		return aDateStr;
	}
	// all from MapScalebar --> redrawScaleBar()
	private static final double METER_FOOT_RATIO = 0.3048;
	private static final int[] SCALE_BAR_VALUES_IMPERIAL = { 26400000, 10560000, 5280000, 2640000, 1056000, 528000,
		264000, 105600, 52800, 26400, 10560, 5280, 2000, 1000, 500, 200, 100, 50, 20, 10, 5, 2, 1 };
    private static final int[] SCALE_BAR_VALUES_METRIC = { 10000000, 5000000, 2000000, 1000000, 500000, 200000, 100000,
		50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100, 50, 20, 10, 5, 2, 1 };
    private static final int BITMAP_WIDTH = 150;
    
    

	
	public static File createDirectory(String pathName) {
		File file = new File(pathName);
		if (!file.exists() && !file.mkdirs()) {
			throw new IllegalArgumentException("could not create directory: " + file);
		} else if (!file.isDirectory()) {
			throw new IllegalArgumentException("not a directory: " + file);
		} else if (!file.canRead()) {
			throw new IllegalArgumentException("cannot read directory: " + file);
		} else if (!file.canWrite()) {
			throw new IllegalArgumentException("cannot write directory: " + file);
		}
		return file;
	}


	
	public  static void copy( InputStream in , OutputStream out)throws IOException {
		byte[] buffer = new byte[0xFFFF];
		for (int len; (len = in.read(buffer)) !=-1;) {
			out.write(buffer,0,len);
		}
	}
	

	
	private static boolean copyFile (String src, String dest) {
		boolean result = false;
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(src);
			fos = new FileOutputStream(dest);
			copy(fis,fos);
			result = true;
		} catch (IOException e){
			Log.d(TAG,"cant create cache_ser_backup " + e.toString());
		} finally {
			if (fis != null)
				try { fis.close(); }catch (IOException e){}
			if (fos != null)
				try { fos.close(); } catch (IOException e) {}
		}
		return result;
	}
	
	public static boolean hasSecondaryStorage() {
		boolean myHasSecondaryStorage = Environment2.isSecondaryExternalStorageAvailable();
		return myHasSecondaryStorage;
	}
	
	public static File getExternalStorageDir() {
		// we use the Environment2.getCardDirectory since 12_11_19
		/*Device firstExternalStorageDevice = Environment2.getPrimaryExternalStorage();
        String firstExternalStorageDevicePath = firstExternalStorageDevice.getMountPoint();
        File aDir = firstExternalStorageDevice.getFile();
		String externalPathName = firstExternalStorageDevicePath;
		File externalStorageDir = aDir;
		boolean hasSecondaryStorage = Environment2.isSecondaryExternalStorageAvailable();
        if (hasSecondaryStorage){
        try {
        	aDir = Environment2.getSecondaryExternalStorageDirectory();
            String aPathname = aDir.getAbsolutePath();
            externalPathName = aPathname;
            externalStorageDir = aDir;
        } catch (Exception e){
        	Log.d(TAG,e.toString());
        }
        } else {
        	Log.d(TAG, "no Secondary storage on SD-Card: ");
        }*/
		File externalStorageDir = Environment2.getCardDirectory();
        return externalStorageDir;
	}
	
public static String getExternalStorageDirPath() {
		
		/*Device firstExternalStorageDevice = Environment2.getPrimaryExternalStorage();
        String firstExternalStorageDevicePath = firstExternalStorageDevice.getMountPoint();
        File aDir = firstExternalStorageDevice.getFile();
		String externalPathName = firstExternalStorageDevicePath;
		File externalStorageDir = aDir;
		boolean hasSecondaryStorage = Environment2.isSecondaryExternalStorageAvailable();
        if (hasSecondaryStorage){
        try {
        	aDir = Environment2.getSecondaryExternalStorageDirectory();
            String aPathname = aDir.getAbsolutePath();
            externalPathName = aPathname;
            externalStorageDir = aDir;
        } catch (Exception e){
        	Log.d(TAG,e.toString());
        }
        } else {
        	Log.d(TAG, "no Secondary storage on SD-Card: ");
        }*/
	     File aDir =  getExternalStorageDir();
	     String externalPathName = aDir.getAbsolutePath();
         return externalPathName;
	}
/**
 * Create the necessary directories to follow the path to the external directory given in pDirPath
 * knows the path to the cardDirectory, the /mnt/sdcard or /mnt/sdcard/sdcard2 
 * @param pDirPath  the path describing the path without ref to the card dir
 */

public static void createExternalDirectoryIfNecessary(String pDirPath) {
	if (test)
		Log.v(TAG, "createAISDirectory");
	//String result = Environment.getExternalStorageState(); since 12_11_19 use Environment2
	// String result = Environment2.getCardState(); // we don't use Environment2 any more 2014_11_02
    String result = Environment.getExternalStorageState();
	if (result.equals(Environment.MEDIA_MOUNTED)) {

	//File path = getExternalStorageDir(); since 12_11_19
	//File path = Environment2.getCardDirectory(); // we don't use Environment2 any more 2014_11_02

	File path = Environment.getExternalStorageDirectory();
		File file = new File(path, pDirPath);
		try {
			String filePathStr = file.getAbsolutePath();
			if (file.mkdirs()) { // here we need android permission in the manifest, mkdirs with generating parents if necessary
				if (test)
					Log.v(TAG, "erzeuge Directory: " + filePathStr);
			} else {
				if (test)
					Log.v(TAG, "directory schon vorhanden " + filePathStr);
			}
		} catch (SecurityException se) {
			Log.d(TAG,se.toString());
			if (test)
				Log.v("TAG", "Security exception : Directory nicht erzeugt " + se);
		} catch (Exception e ) {
			Log.d(TAG,e.toString());
			
		} // try
	
	}
}
public static void old_createExternalDirectoryIfNecessary(String pDirPath) {
	if (test)
		Log.v(TAG, "createAISDirectory");
	//String result = Environment.getExternalStorageState(); since 12_11_19 use Environment2
	String result = Environment2.getCardState();
	if (result.equals(Environment.MEDIA_MOUNTED)) {

		//File path = getExternalStorageDir(); since 12_11_19 
		File path = Environment2.getCardDirectory();
		// if pDirName contains / we have to analyse the whole path
		String [] dirs = pDirPath.split("/");
		int dirCount = dirs.length;
		String newDirName = "/"; // we must begin with a /
	    //  now we know how many dirs we must create
		for (int dirIndex =0;dirIndex < dirCount; dirIndex++){
			StringBuffer buf = new StringBuffer();
			newDirName = newDirName + dirs[dirIndex]  +"/";
			buf.append(newDirName);
			String dirName = buf.toString();
			File file = new File(path, dirName);
			try {
				String filePathStr = file.getAbsolutePath();
				if (file.mkdir()) { // here we need android permission in the manifest
					if (test)
						Log.v(TAG, "erzeuge Directory: " + filePathStr);
				} else {
					if (test)
						Log.v(TAG, "directory schon vorhanden " + filePathStr);
				}
			} catch (SecurityException se) {
				Log.d(TAG,se.toString());
				if (test)
					Log.v("TAG", "Security exception : Directory nicht erzeugt " + se);
			} catch (Exception e ) {
				Log.d(TAG,e.toString());
				
			} // try
		} //for
	}
}
		
}
