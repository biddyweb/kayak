/*
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
package org.mapsforge.applications.android.samples.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.mapsforge.applications.android.samples.utils.DirectoryUtils;
import org.mapsforge.core.model.LatLong;




import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;



/**
 * @author vkADM
 *
 */
public class TrackDbAdapter {
	private static final int DATABASE_VERSION = 1;
	private static final String TAG = "TrackDbAdapter";
	
	public static final String KEY_ROWID = "_id";
	public static final String KEY_MMSI = "mmsi";
    public static final String KEY_NAME = "name";
    public static final String KEY_LAT  = "lat";
    public static final String KEY_LON  = "lon";
    public static final String KEY_COG  = "cog";
    public static final String KEY_SOG  = "sog";
    public static final String KEY_HDG  = "hdg";
    public static final String KEY_UTC = "utc";
 
 // used in fix   information  in whitewater table
    
   /* 
    KEY_WW_NUMBER      = "ww_number"	;
    KEY_WW_TYPE        = "ww_type";
    KEY_WW_DESCRIPTION = "ww_description";
    KEY_WW_NAME 	      = "ww_name";
   */  
    
   
    public static final String KEY_WW_NUMBER      = "ww_number"	;
    public static final String KEY_WW_TYPE        = "ww_type";
    public static final String KEY_WW_DESCRIPTION = "ww_description";
    public static final String KEY_WW_NAME 	      = "ww_name";
    
	/*
	        + KEY_WW_NUMBER  
	        + KEY_WW_TYPE 
			+ KEY_WW_NAME 
			+ KEY_WW_DESCRIPTION 
			+ KEY_UTC 
			+ KEY_LAT    
	        + KEY_LON 
	 */
	
	public  static final String FIXED_WHITEWATER_TABLE = "fixed_ww_";
	private static final String FIXED_WHITEWATER_CREATE = "create table ";
	private static final String FIXED_WHITEWATER_CREATE_PARAMS = " (_id integer primary key autoincrement, "
			+ KEY_WW_NUMBER + " text not null, " 
			+ KEY_WW_TYPE + " text not null, " 
			+ KEY_WW_NAME + " text not null, " 
			+ KEY_WW_DESCRIPTION + " text not null, " 
			+ KEY_UTC + " text not null, " 
			+ KEY_LAT + " text not null, "   
	        + KEY_LON + " text not null);";
    
    
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private boolean test = false;
    
    /**
	* table creation sql statement for shiptracks 
	*/
	    private static final String SHIPTRACK_TABLE = "shiptrack";

		private static final String SHIPTRACK_CREATE = "create table ";
		private static final String SHIPTRACK_CREATE_PARAMS = " (_id integer primary key autoincrement,"
				+ "mmsi text not null, lat text not null, lon text not null, utc integer not null);";
    
		private ArrayList<String> mTrackTableNames = new ArrayList<String>();
		// we use the name aisdata for the database, later we may add AIS-Targets support
		private static final String DATABASE_NAME = "aisdata";
		private final Context mCtx;

	    private static class DatabaseHelper extends SQLiteOpenHelper {

	        DatabaseHelper(Context context) {
	            super(context, DATABASE_NAME, null, DATABASE_VERSION);
	        }

	        @Override
	        public void onCreate(SQLiteDatabase db) {
                // actually we have no AIS-Targets
	            //db.execSQL(AIS_TARGETS_TABLE_CREATE);
	        }

	        @Override
	        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
	                    + newVersion + ", which will destroy all old data");
	            
	            ArrayList<String> aTableList = listTrackTables(db);
	            for (int index = 0; index < aTableList.size();index++){
	            	String aTableName = aTableList.get(index);
	            	db.execSQL("DROP TABLE IF EXISTS "+aTableName);
	            }
	            // actually we have no AIS-Targets
	            //db.execSQL("DROP TABLE IF EXISTS "+AIS_TARGETS_TABLE);
	            onCreate(db);
	        }
	        
	        private ArrayList<String> listTrackTables(SQLiteDatabase db) throws SQLException {

				try {
					// see How to get all table names in SQL database in Android Developers
					// http://groups.google.com/group/android-developers/browse_thread/thread/13cd2537a0adc9b9
					// www.sqlite.org/faq.html How do I list all tables/indices contained in an SQLite database
					// String aQuery = "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;";
					String SQL_GET_ALL_TABLES = "SELECT name FROM " + "sqlite_master WHERE type='table' ORDER BY name";
					ArrayList<String> aStringList = new ArrayList<String>();
					Cursor aCursor = db.rawQuery(SQL_GET_ALL_TABLES, null);
					// Cursor aCursor = this.mDb.query("sqlite_master", null, "type='table'", null, null, null, "name");

					String[] names = aCursor.getColumnNames();
					int count = names.length;

					int rowcount = aCursor.getCount();
					aCursor.moveToFirst();
					int index = aCursor.getColumnIndexOrThrow("name");
					String s1 = aCursor.getString(index);
					if (s1.contains ("shiptrack") ) aStringList.add(s1);
					while (aCursor.moveToNext()) {
						index = aCursor.getColumnIndexOrThrow("name");
						s1 = aCursor.getString(index);
						if (s1.contains ("shiptrack")) aStringList.add(s1);
					}

					aCursor.close();

					return aStringList;

				} catch (SQLException e) {
					Log.d(TAG, " Error getting getting tables from the database ");
					e.printStackTrace();

				}
				return null;
	        }
	    }  // DatabaseHelper Class
	    
	    /**
	     * Constructor - takes the context to allow the database to be
	     * opened/created
	     * 
	     * @param ctx the Context within which to work
	     */
	    public TrackDbAdapter(Context ctx) {
	        this.mCtx = ctx;
	    }

	    /**
	     * Open the TarckDb database. If it cannot be opened, try to create a new
	     * instance of the database. If it cannot be created, throw an exception to
	     * signal the failure
	     * 
	     * @return this (self reference, allowing this to be chained in an
	     *         initialization call)
	     * @throws android.database.SQLException if the database could be neither opened or created
	     */
	    public TrackDbAdapter open() throws SQLException {
	        mDbHelper = new DatabaseHelper(mCtx);
	        mDb = mDbHelper.getWritableDatabase();
	        mTrackTableNames = listAllShipTrackTablesInDatabase();
	        if (test) Log.d(TAG,"tables: " + mTrackTableNames.toString());
	        return this;
	    }

	    public void close() throws SQLException{
	        mDbHelper.close();
	    }
	    
	    public ArrayList<String> listAllShipTrackTablesInDatabase() {
	    	Cursor aCursor = null;
			try {
				// see How to get all table names in SQL database in Android Developers
				// http://groups.google.com/group/android-developers/browse_thread/thread/13cd2537a0adc9b9
				// www.sqlite.org/faq.html How do I list all tables/indices contained in an SQLite database
				// String aQuery = "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;";
				String SQL_GET_ALL_TABLES = "SELECT name FROM " + "sqlite_master WHERE type='table' ORDER BY name";
				ArrayList<String> aStringList = new ArrayList<String>();
				aCursor = this.mDb.rawQuery(SQL_GET_ALL_TABLES, null);
				// Cursor aCursor = this.mDb.query("sqlite_master", null, "type='table'", null, null, null, "name");

				String[] names = aCursor.getColumnNames();
				int count = names.length;

				int rowcount = aCursor.getCount();
				aCursor.moveToFirst();
				int index = aCursor.getColumnIndexOrThrow("name");
				String s1 = aCursor.getString(index);
				if (s1.contains ("shiptrack")) aStringList.add(s1);
				while (aCursor.moveToNext()) {
					index = aCursor.getColumnIndexOrThrow("name");
					s1 = aCursor.getString(index);
					if (s1.contains ("shiptrack")) aStringList.add(s1);
				}

				aCursor.close();

				return aStringList;

			} catch (SQLException e) {
				Log.d(TAG, " Error getting getting tables from the database ");
				e.printStackTrace();
				return null;

			} catch (Exception e) {
				Log.d(TAG," other error getting tables from database");
				return null;
			}
			finally {
				if (aCursor != null) aCursor.close(); 
			}
		
		}
	   
	    /** create a table for the track to the target with mmsi
	     * 
	     * @param aMMSI the mmsi of the target to which the track belongs
	     */
	    public void createShipTrackTable(String aMMSI)  {
	    	String tableName = SHIPTRACK_TABLE + aMMSI;
			try {
				mDb.execSQL(SHIPTRACK_CREATE + tableName + SHIPTRACK_CREATE_PARAMS);
				mTrackTableNames.add(tableName);
			} catch (SQLException e) {
				Log.d(TAG, " Error in creating ShipTrackTable to " + aMMSI);
				Log.d(TAG,"exception " + e.toString());
			}
		}
	    /**
	     * insert a track point to the table for this mmsi
	     * @param aMMSI  mmsi of target
	     * @param aLAT   LAT for trackpoint as string e.g. 52.3452
	     * @param aLON   LON for trackpoint as string eg. 6.3432
	     * @param aUTC   long denoting utc of inserting to db
	     */
	    
	    public void insertTrackPointToTable(String aMMSI, String aLAT, String aLON, long aUTC) {
	    	String tableName = SHIPTRACK_TABLE + aMMSI;
	    	 ContentValues initialValues = new ContentValues();
		     initialValues.put(KEY_MMSI, aMMSI);
		     initialValues.put(KEY_LAT, aLAT);
		     initialValues.put(KEY_LON, aLON);
		     initialValues.put(KEY_UTC, aUTC);
		     long id = -1;
		     try {
		       id = mDb.insert(tableName, null, initialValues);
		     }
		     catch (SQLException e){
		    	 String message = " Error while inserting Point in ShipTrackTable to " + aMMSI;
		    	 Log.d(TAG,message + e.toString());
		     }
		     catch (IllegalStateException e){
		    	 String message = " found a illegal state Exception";
		    	 Log.d(TAG, message + e.toString());
		    	 e.printStackTrace();
		     }
		     //Log.d(TAG,"TrackPoint Id " + id + " LAT " + aLAT + "  LON " + aLON);
	    }
	    
	    /**
	     *  delete a track table to target with mmsi
	     * @param aMMSI
	     * @throws android.database.SQLException
	     */
	    public void deleteShipTrackTable(String aMMSI) {
	    	String tableName = SHIPTRACK_TABLE + aMMSI;
	    	try {
	    		 mDb.execSQL("DROP TABLE IF EXISTS "+tableName);
	    		 mTrackTableNames.clear();
	    		 mTrackTableNames = listAllShipTrackTablesInDatabase();
	    	}
	    	catch(SQLException e) {
	    		Log.d(TAG," Error in deleting ShipTrackTable to " + aMMSI);
	    	}
	    }
	    
	    /**
	     * fetch track table to target with mmsi aMMSI :remember we get lat, lon in grad 53.244
	     * @param aMMSI
	     * @return a Cursor with KEY_ROWID, KEY_MMSI, KEY_LAT, KEY_LON, KEY_UTC
	     * @throws android.database.SQLException
	     */
	    public Cursor fetchShipTrackTable(String aMMSI) {
			try {
				Cursor aCursor = mDb.query(SHIPTRACK_TABLE + aMMSI, new String[] { KEY_ROWID, KEY_MMSI, KEY_LAT, KEY_LON,
						KEY_UTC }, null, null, null, null, null); // name,
																	// colums[],no
																	// selection,no
																	// selectionArgs.
																	// nogroupBy,
																	// no
																	// having,
																	// no
																	// orderBy
				return aCursor;
			} catch (SQLException e) {
				Log.d(TAG, " Error quering ShipTrackTable to " + aMMSI);
			}
			return null;
		}
	    
	    public boolean isTableToMMSIInShipTrackList(String aMMSI){
	    	String aTablename = SHIPTRACK_TABLE+aMMSI;
	    	ArrayList<String> aList = mTrackTableNames;
	    	for (int index = 0;index < aList.size();index ++){
	    		if (aTablename.equals(aList.get(index))) {
	    			return true;
	    		}
	    	}
	    	return false;
	    }
	    
	    /**
	     * get the table size of track to mmsi aMMSI
	     * @param aMMSI
	     * @return  the table size
	     * @throws android.database.SQLException
	     */
	    
	    public long getShipTrackTableSize(String aMMSI) {
			long result = -1;
			try {
				Cursor aCursor = mDb.query(SHIPTRACK_TABLE + aMMSI, new String[] { KEY_ROWID }, null, null, null, null,
						null);
				result = aCursor.getCount();
			} catch (SQLException e) {
				Log.d(TAG, " Error getting size of  ShipTrackTable to " + aMMSI);
				e.printStackTrace();
			}
			return result;
		}
	    
	    /**
		 * @param pattern
		 *            use a pattern like "000.00"
		 * @param value
		 *            the value to convert 45.34523
		 * @return aString with the value formatted 045.34
		 */
		public static String customFormat(String pattern, double value) {
			DecimalFormat myFormatter = new DecimalFormat(pattern);
			String output = myFormatter.format(value);
			return output;
		}  
	// the following is needed to save the data to the sd-card
		private String getCurrentDateTime() {
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
			if (test)
				Log.v(TAG, "Datum: " + aDateStr);
			return aDateStr;
		}
	    
	    public void WriteTrackDataToExternalStorage(String aMMSI) {
		    String aName = "";
		    /* we do not track AIS-targets now
		     * 
		     * String myMMSI = Long.toString(AISPlotterGlobals.myShipMMSI);
		    if (aMMSI.equals(myMMSI)){
		    	aName = "myShip";
		    } else {
		    	try {
				    Cursor cursor = this.fetchTargetFromMMSI(aMMSI);
					aName = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_NAME));
				    cursor.close();
		    	} 
		    	catch (Exception e) {
		    	  e.printStackTrace();
		    	}
		    }*/ 
		    
		    
		    final String theMMSI = aMMSI;
		    final String theName = aName; 
		    
			new Thread(new Runnable() { // as we want response to the user we must use a separate thread
						public void run() {
							// Create a path where we will place our data in the user's
							// public directory. Note that you should be careful about
							// what you place here, since the user often manages these files.
							// we write the data in a directory called AISPlotter/Trackdata
							String result = Environment.getExternalStorageState();
							if (result.equals(Environment.MEDIA_MOUNTED)) {
								String aDirectoryName = DirectoryUtils.getTrackDirectoryPath();
								DirectoryUtils.createExternalDirectoryIfNecessary(aDirectoryName);
								File dirPath = new File(aDirectoryName);
								StringBuffer buf = new StringBuffer();
								// if we found a sdcard2 directory we put it in front of the path
								
								buf.append(aDirectoryName);
								buf.append("/Track_");
								buf.append(theName);
								buf.append("_");
								buf.append(theMMSI);
								buf.append("_");
								buf.append(getCurrentDateTime());
								buf.append(".gpx");
								String fileName = buf.toString();
								File file = new File( fileName);
								String filePathStr = file.getAbsolutePath();
								try {
									if (file.createNewFile()) { // here we need android permission in the manifest
										if (test)
											Log.v(TAG, "create file: " + filePathStr);
									} else {
										if (test)
											Log.v(TAG, "file exists, overwrite " + filePathStr);
									}
									// the file exists or was opened for writing
									BufferedWriter fileBuf = new BufferedWriter(new FileWriter(file));
									// Write the gpx header
									// <?xml version="1.0" encoding="UTF-8"?>
									// write header
									String header = "<?xml version=" + '"' + "1.0" + '"' + " encoding=" + '"' + "UTF-8"
											+ '"' + "?>";
									fileBuf.write(header);
									// "creator="GPSTracker" version="1.1"
									String gpxStart = "\n <gpx creator=" + '"' + "AdvancedMapViewer" + '"' + " version="
											+ '"' + "0.3" + '"';
									// xsi:schemaLocation="http://www.topografix.com/GPX/1/1
									gpxStart = gpxStart + "\n xsi:schemaLocation=" + '"'
											+ "http://www.topografix.com/GPX/1/1 ";
									// http://www.topografix.com/GPX/1/1/gpx.xsd"
									gpxStart = gpxStart + "\n http://www.topografix.com/GPX/1/1/gpx.xsd" + '"';
									// xmlns="http://www.topografix.com/GPX/1/1"
									gpxStart = gpxStart + "\n xmlns=" + '"' + "http://www.topografix.com/GPX/1/1" + '"';
									// xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
									gpxStart = gpxStart + "\n xmlns:xsi=" + '"'
											+ "http://www.w3.org/2001/XMLSchema-instance" + '"' + ">";
									fileBuf.write(gpxStart);
									String metadataStart = "\n <metadata>";
									fileBuf.write(metadataStart);
									String metadataEnd = "\n </metadata>";
									fileBuf.write(metadataEnd);
									String trsegStart = "\n <trk>\n <trkseg>";
									fileBuf.write(trsegStart);

									Cursor cursor = fetchShipTrackTable(theMMSI);
									int count = cursor.getCount();
									int updateProgressbar = count / 100; // The progressbar is 0 to 100,
									// we calculate the counter if we have to update the progressbar
									int updateCounter = 0;
									if ((cursor != null) && (cursor.getCount() > 0)) {

										String aId;
										String aLATStr;
										String aLONStr;
										String aMMSI;
										long aUTC;
										// we use the simpleDataFormat to convert millis to gpx-readable form
										String format = "yyyy-MM-dd'T'HH:mm:ss";
										SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
										sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
										cursor.moveToFirst();
										aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
										aMMSI = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_MMSI));
										aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
										aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
										aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
										LatLong aTrackPoint = new LatLong(Double.parseDouble(aLATStr), Double
												.parseDouble(aLONStr));
										StringBuffer bufPoint = new StringBuffer();
										// <trkpt lon="6.96045754" lat="51.44282806"> <time>2011-07-16T13:24:55</time>
										// </trkpt>
										bufPoint.append("\n <trkpt lon=" + '"' + aTrackPoint.longitude + '"' + " lat="
												+ '"' + aTrackPoint.latitude + '"' + ">" + " <time>"
												+ sdf.format(new Date(aUTC)) + "</time> " + "</trkpt>");

										String aString = bufPoint.toString();
										fileBuf.write(aString);
										while (cursor.moveToNext()) {
											aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
											aMMSI = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_MMSI));
											aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
											aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
											aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
											if (test) {
												Log.i(TAG, "next route Point " + aId + " MMSI " + theMMSI + " LAT " + aLATStr
														+ " LON " + aLONStr + " UTC " + aUTC);
											}

											double lat = Double.parseDouble(aLATStr);
											double lon = Double.parseDouble(aLONStr);
											aTrackPoint = new LatLong(lat, lon);
											bufPoint = new StringBuffer();
											// <trkpt lon="6.96045754" lat="51.44282806"> </trkpt>
											bufPoint.append("\n <trkpt lon=" + '"' + aTrackPoint.longitude + '"'
													+ " lat=" + '"' + aTrackPoint.latitude + '"' + ">" + " <time>"
													+ sdf.format(new Date(aUTC)) + "</time> " + "</trkpt>");

											aString = bufPoint.toString();
											fileBuf.write(aString);
											
										}

									}
									cursor.close();
									String trsegEnd = "\n </trkseg>\n </trk>\n </gpx>";
									fileBuf.write(trsegEnd);
									fileBuf.flush();
									fileBuf.close();
									if (test)
										Log.v(TAG, "file write sucessfull " + filePathStr);
									

								} catch (IOException e) {

									e.printStackTrace();
									// Unable to create file, likely because external storage is
									// not currently mounted.
									if (test)
										Log.w("TAG", "Error writing " + filePathStr);
								}
							} // if media mounted
						} // run
					}).start();
		}
	    
// Methods to handle WhitewaterTable
	    
	    // create, insertPoint, updatePoint, deleteEntry, fetchEntry, 
	    // fetchTable, fetchAllEntries, truncateTable, deleteTable
	    
	    /**
	     * create a fixed soundings table 
	     * @param pIdentifier  identifier of the table see AISPlotterGlobals.
	     * @throws android.database.SQLException
	     */
	    public void createFixedWhitewaterTable(String pIdentifier) throws SQLException {
            String sqlString = FIXED_WHITEWATER_CREATE + FIXED_WHITEWATER_TABLE + pIdentifier + FIXED_WHITEWATER_CREATE_PARAMS;
			mDb.execSQL(sqlString);

		}
	    
	    /**
	     * delete all entries from the table
	     * @param pIdentifier
	     * @throws android.database.SQLException
	     */
	    
	    public void truncateFixedWhiteWaterTable(String pIdentifier) throws SQLException {
			//mDb.execSQL("TRUNCATE TABLE " + ROUTE_TABLE + aRouteTableNumber);
			mDb.delete( FIXED_WHITEWATER_TABLE + pIdentifier, null, null);
		}
        
	    /**
	     * delete the table
	     * @param pIdentifier
	     * @throws android.database.SQLException
	     */
		public void deleteFixedWhiteWaterTable(String pIdentifier) throws SQLException {

			mDb.execSQL("DROP TABLE IF EXISTS " + FIXED_WHITEWATER_TABLE + pIdentifier);

		}
		
		/*
        + KEY_WW_NUMBER + " text not null, " 
        + KEY_WW_TYPE + " text not null, " 
		+ KEY_WW_NAME + " text not null, " 
		+ KEY_WW_DESCRIPTION + " text not null, " 
		+ KEY_UTC + " text not null, " 
		+ KEY_LAT + " text not null, "   
        + KEY_LON + " text not null);";
 */
		
		/**
		 * 
		 * @param pIdentifier
		 * @param pNumberStr
		 * @param pTypeStr
		 * @param pName
		 * @param pDescription
		 * @param pUTC
		 * @param pLATStr
		 * @param pLONStr
		 * @return
		 */
		public long insertPointToFixedWhitewaterTable(String pIdentifier, String pNumberStr,String pTypeStr, String pName, 
				                                      String pDescription, String pUTC, String pLATStr, String pLONStr) {
				                                    
			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_WW_NUMBER, pNumberStr);
			initialValues.put(KEY_WW_TYPE, pTypeStr);
			initialValues.put(KEY_WW_NAME, pName);
			initialValues.put(KEY_WW_DESCRIPTION, pDescription);
			initialValues.put(KEY_UTC, pUTC);
			initialValues.put(KEY_LAT, pLATStr);
			initialValues.put(KEY_LON, pLONStr);
			
			

			long id = -1;
			try {
			id = mDb.insert(FIXED_WHITEWATER_TABLE + pIdentifier, null, initialValues);
			} catch (SQLException e) {
			Log.d(TAG, " Error while inserting Point in Fixed Whitewater Table to " + pIdentifier);
			//Logger.d(TAG, " Error while inserting Point in Fixed Whitewater Table to " + aIdentifier);
			}
			return id;
			
			}
		/**
		 * delete the entry belonging to rowId from the table
		 * @param pIdentifier
		 * @param rowId
		 * @return
		 */
		public boolean deleteEntryInFixedWhiteWaterTable(String pIdentifier, long rowId) {
			int theResult = -1;
			boolean result = false;
			try {
		          theResult = mDb.delete(FIXED_WHITEWATER_TABLE + pIdentifier,KEY_ROWID + "=" + rowId, null);
			        if (theResult >  1) {
			        	Log.d(TAG,"error in delete entry db"  );
			        }
			        result =  theResult > 0;
				}  catch (SQLException e) {
					Log.d(TAG, " Error deleteEntry Fixed Whitewater Table to " + pIdentifier + e.toString());
				}  catch  (Exception e ){
					Log.d(TAG, " Error delete Entry Fixed Whitetable Table to " + pIdentifier + e.toString());
				}
				return result;
		}
		
		/**
		 * update a whitewater entry in the table with rowId
		 * @param pIdentifier
		 * @param pRowId
		 * @param pNumberStr
		 * @param pDescriptionStr
		 * @param pLATStr
		 * @param pLONStr
		 * @param pTypeStr
		 * @return
		 */
		
		/*
        + KEY_WW_NUMBER + " text not null, " 
        + KEY_WW_TYPE + " text not null, " 
		+ KEY_WW_NAME + " text not null, " 
		+ KEY_WW_DESCRIPTION + " text not null, " 
		+ KEY_UTC + " text not null, " 
		+ KEY_LAT + " text not null, "   
        + KEY_LON + " text not null);";
 */
		
		public boolean updatePointInFixedWhitewaterTable (String pIdentifier, long pRowId,String pNumberStr, String pTypeStr,
				                                          String pNameStr, String pDescriptionStr, String pUtcStr, 
				                                          String pLATStr, String pLONStr) {
			ContentValues args = new ContentValues();
			args.put(KEY_WW_NUMBER, pNumberStr);
			args.put(KEY_WW_TYPE, pTypeStr);
			args.put(KEY_WW_NAME, pNameStr);
			args.put(KEY_WW_DESCRIPTION, pDescriptionStr);
			args.put(KEY_UTC, pUtcStr);
			args.put(KEY_LAT, pLATStr);
			args.put(KEY_LON, pLONStr);
			int theResult = -1;
			boolean result = false;
			try {
			theResult = mDb.update(FIXED_WHITEWATER_TABLE + pIdentifier, args, KEY_ROWID + "=" + pRowId, null);
			if (theResult >  1) {
			Log.d(TAG,"error in update db"  );
			}
			result =  theResult > 0;
			}  catch (SQLException e) {
			Log.d(TAG, " Error updating Fixed whitewater Table to " + pIdentifier + e.toString());
			}  catch  (Exception e ){
			Log.d(TAG, " Error updating Fixed whitewater Table to " + pIdentifier + e.toString());
			}
			Log.d(TAG,"Fixed whitewater entry " +  pNumberStr + " " + pNameStr + " " + pDescriptionStr  );
			return result;
		}
		
		/**
		 * 
		 * @param pIdentifier
		 * @param pRowId
		 * @return
		 * @throws android.database.SQLException
		 */
		
		public Cursor fetchFixedWhitewaterTableEntry(String pIdentifier,long pRowId) throws SQLException {
			
			String [] columns = new String[] {KEY_ROWID,
                    KEY_WW_NUMBER,KEY_WW_TYPE, KEY_WW_NAME, KEY_WW_DESCRIPTION, KEY_LAT,KEY_LON};
                    
			// params: table, columns,
		    //         selection , selectionArgs, groupBy, having, orderBy
	        Cursor mCursor = mDb.query(true, FIXED_WHITEWATER_TABLE + pIdentifier,columns , KEY_ROWID + "=" + pRowId, 
	            		       null,null, null, null, null);
	        if (mCursor != null) {
	            mCursor.moveToFirst();
	        }
	        return mCursor;

		}
		/**
		 * 
		 * @param pIdentifier
		 * @return
		 * @throws android.database.SQLException
		 */
		
		/*
        + KEY_WW_NUMBER + " text not null, " 
        + KEY_WW_TYPE + " text not null, " 
		+ KEY_WW_NAME + " text not null, " 
		+ KEY_WW_DESCRIPTION + " text not null, " 
		+ KEY_UTC + " text not null, " 
		+ KEY_LAT + " text not null, "   
        + KEY_LON + " text not null);";
 */
		public Cursor fetchFixedWhitewaterTable(String pIdentifier) throws SQLException {
			String [] columns = new String[] { KEY_ROWID, KEY_WW_NUMBER,KEY_WW_TYPE, KEY_WW_NAME, KEY_WW_DESCRIPTION, 
					                           KEY_LAT, KEY_LON, KEY_UTC};
			try {
				Cursor aCursor = mDb.query(FIXED_WHITEWATER_TABLE + pIdentifier, 
						columns,
						null, null, null, null, null); // name,
														// columns[]
														// selection,no
														// selectionArgs none
														// no groupBy,
														// no having,
														// no orderBy
														
				return aCursor;
			} catch (SQLException e) {
				Log.d(TAG, " Error quering Fixed whitewater Table to " + pIdentifier + e.toString());
			}
			return null;
		}
		
		/**
		 * 
		 * @param pIdentifier
		 * @return
		 * @throws android.database.SQLException
		 */
		public Cursor fetchFixedWhitewaterAllEntriesNumberAndName(String pIdentifier)throws SQLException {
	           // params: table, columns,
		       // selection , selectionArgs, groupBy, having, orderBy
		      
		        
		        String [] columns = new String[] {KEY_ROWID, KEY_WW_NUMBER,KEY_WW_NAME};
		        Cursor aCursor = mDb.query(FIXED_WHITEWATER_TABLE + pIdentifier, columns, 
		        		null, null, null, null, null);
		        return aCursor;
		    }

    public boolean existsWWTable(String pTableName){
        String aCompleteName = FIXED_WHITEWATER_TABLE + pTableName;
        ArrayList<String> wwTables = this.listAllInfoTablesForSelectorInDatabase(FIXED_WHITEWATER_TABLE);
        if (wwTables != null) {
            for (int index = 0;index < wwTables.size();index ++){
                if (aCompleteName.equals(wwTables.get(index))) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Construct the table name for the current info table
     * @return
     */
		
		
		// end of FixedWhitetwaterTable ------------------------------------------------------
	    
		public ArrayList<String> listAllInfoTablesForSelectorInDatabase(String selector) {
	    	Cursor aCursor = null;
			try {
				// see How to get all table names in SQL database in Android Developers
				// http://groups.google.com/group/android-developers/browse_thread/thread/13cd2537a0adc9b9
				// www.sqlite.org/faq.html How do I list all tables/indices contained in an SQLite database
				// String aQuery = "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;";
				String SQL_GET_ALL_TABLES = "SELECT name FROM " + "sqlite_master WHERE type='table' ORDER BY name";
				ArrayList<String> aStringList = new ArrayList<String>();
				aCursor = this.mDb.rawQuery(SQL_GET_ALL_TABLES, null);
				// Cursor aCursor = this.mDb.query("sqlite_master", null, "type='table'", null, null, null, "name");

				String[] names = aCursor.getColumnNames();
				int count = names.length;

				int rowcount = aCursor.getCount();
				aCursor.moveToFirst();
				int index = aCursor.getColumnIndexOrThrow("name");
				String s1 = aCursor.getString(index);
				if (s1.contains (selector)) aStringList.add(s1);
				while (aCursor.moveToNext()) {
					index = aCursor.getColumnIndexOrThrow("name");
					s1 = aCursor.getString(index);
					if (s1.contains (selector)) aStringList.add(s1);
				}

				aCursor.close();

				return aStringList;

			} catch (SQLException e) {
				Log.d(TAG, " Error getting getting tables from the database ");
				e.printStackTrace();
				return null;

			} catch (Exception e) {
				Log.d(TAG," other error getting tables from database");
				return null;
			}
			finally {
				if (aCursor != null) aCursor.close(); 
			}
		
		}
		 
	    
	    
	   
}
