package org.mapsforge.applications.android.samples;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.mapsforge.applications.android.samples.common.Environment2;
import org.mapsforge.applications.android.samples.common.PositionTools;
import org.mapsforge.applications.android.samples.seamark.SeamarkNode;
import org.mapsforge.applications.android.samples.service.TrackDbAdapter;
import org.mapsforge.applications.android.samples.utils.DirectoryUtils;
import org.mapsforge.applications.android.samples.whitewater.KayakInfoItem;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by vkmapsforge05 on 05.12.2014.
 */
public class KayakJosm {
    private static final String TAG = "KayakJosm";
    private static final boolean test = true;
    private KayakBasicActivity mContext;
    private TrackDbAdapter mDbAdapter = null;

    KayakJosm (KayakBasicActivity pContext) {
      mContext=pContext;
        mDbAdapter = new TrackDbAdapter(mContext);
        mDbAdapter.open(); // assert that the Adapter is closed when KayakJosm is destroyed
    }

    public void destroy () {
        if (mDbAdapter != null){
            mDbAdapter.close();
        }
    }

    public void saveFixedKayakItems_Menu(String pTableName,String pDirectoryName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final String aDirectoryName = pDirectoryName;
        final String aTableName = pTableName;
        LayoutInflater factory = LayoutInflater.from(mContext);
        final View textEntryView = factory.inflate(R.layout.save_kayak_item_alert_dialog_text_entry, null);
        final TextView aFilenameEditField = (TextView)textEntryView.findViewById(R.id.filename_edit);
        aFilenameEditField.setText(aTableName);
        final TextView aDirectoryEditField = (TextView)textEntryView.findViewById(R.id.basedirectory_edit);
        String prev_kayak_items_dir_key =  mContext.getResources().getString(R.string.pref_kayak_item_directory_key);
        String aKayakItems_dir_str = prefs.getString(prev_kayak_items_dir_key, KayakGlobals.DEFAULT_FIXED_KAYAK_ITEMS_DATA_DIRECTORY);
        //String curDirPath = getCurrentDirectory().getAbsolutePath();
        aDirectoryEditField.setText(aKayakItems_dir_str);
        AlertDialog aDialog =  new AlertDialog.Builder(mContext)
                .setIcon(R.drawable.alert_dialog_icon)
                .setTitle(R.string.menu_alert_dialog_fixed_kayak_items_title)
                .setView(textEntryView)
                .setPositiveButton(R.string.menu_alert_dialog_fixed_kayak_items_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String aFilename =  aFilenameEditField.getText().toString();
                        String aDirPath = aDirectoryEditField.getText().toString();
                        aDirPath = aDirectoryName +"/"+ aDirPath;
                        saveFixedKayakItems_as_JOSM_Layer_ToExternalStorage(aTableName, aFilename, aDirPath);

                    }
                })
                .setNegativeButton(R.string.menu_alert_dialog_fixed_kayak_items_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked cancel so do some stuff */
                    }
                })
                .create();
        aDialog.show();

    }

    /**
     *
     * @param pFilename
     * @param pDirPath
     */
    @SuppressLint("DefaultLocale")
    private void saveFixedKayakItems_as_JOSM_Layer_ToExternalStorage(String pTablename, String pFilename, String pDirPath) {
        if (test){
            Log.v(TAG, "save JOSM Layer");
        }

        final String aDirPath = pDirPath;
        final String aFilename = pFilename;
        final String aTablename = pTablename;

        new Thread(new Runnable() { // as we want response to the user we must use a separate thread
            @SuppressLint("DefaultLocale")
            public void run() {
                // Create a path where we will place our data in the user's
                // public directory. Note that you should be careful about
                // what you place here, since the user often manages these files.
                // we write the data in a directory called Trackdata
                DirectoryUtils.createExternalDirectoryIfNecessary(aDirPath);

                String result = Environment.getExternalStorageState();
                if (result.equals(Environment.MEDIA_MOUNTED)) {
                    //String  aDirPath = DirectoryUtils.getFixedKayakItemsDirectoryPath();
                    File aDir = new File(aDirPath);
                    StringBuffer buf = new StringBuffer();
                    buf.append(aDirPath);
                    buf.append("/JOSM_LAYER_KAYAK_ITEMS_");
                    buf.append(aFilename);
                    buf.append("_");
                    buf.append(PositionTools.getCurrentDateTimeForFilename());
                    buf.append(".xml");
                    String fileName = buf.toString();
                    File file = new File(fileName);
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

                        BufferedWriter fileBufWriter = new BufferedWriter(new FileWriter(file));

                        Cursor cursor = mDbAdapter.fetchFixedWhitewaterTable(aTablename);
                        int count = cursor.getCount();
                        int updateProgressbar = count / 100; // The progressbar is 0 to 100,
                        // we calculate the counter if we have to update the progressbar
                        int updateCounter = 0;
                        if ((cursor != null) && (cursor.getCount() > 0)) {
                            // we first calculate the dimensions of the bounding Box and write the header
                            @SuppressWarnings("unused")
                            String aId;
                            @SuppressWarnings("unused")
                            String aNumberStr;

                            String aTypeStr;
                            String aNameStr;
                            String aDescriptionStr;

                            String aLATStr;
                            String aLONStr;
                            double aLAT;
                            double aLON;
                            // we calculate this for the bbox
                            double aMaxLat = 90.0d;
                            double aMinLat = -80.0d;
                            double aMinLon = -180.0d;
                            double aMaxLon = +180.d;
                            cursor.moveToFirst();
                            aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
                            aNumberStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_NUMBER));
                            aTypeStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_TYPE));
                            aNameStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_NAME));
                            aDescriptionStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_DESCRIPTION));
                            aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
                            aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
                            aLAT = Double.parseDouble(aLATStr);
                            aLON = Double.parseDouble(aLONStr);

                            aMaxLat = aLAT + 1E-6d;
                            aMinLat = aLAT - 1E-6d;
                            aMinLon = aLON - 1E-6d;
                            aMaxLon = aLON + 1E-6d;
                            while (cursor.moveToNext()) {
                                aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
                                aNumberStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_NUMBER));
                                aTypeStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_TYPE));
                                aNameStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_NAME));
                                aDescriptionStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_DESCRIPTION));
                                aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
                                aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
                                aLAT = Double.parseDouble(aLATStr);
                                aLON = Double.parseDouble(aLONStr);
                                if (aMaxLat < aLAT) aMaxLat = aLAT;
                                if (aMinLat > aLAT) aMinLat = aLAT;
                                if (aMaxLon < aLON) aMaxLon = aLON;
                                if (aMinLon > aLON) aMinLon = aLON;
                            }
                            StringBuffer headerbuf = new StringBuffer();
                            headerbuf.append("<?xml version='1.0' encoding='UTF-8'?>\n");
                            headerbuf.append("<osm version='0.6' generator='FIXED_KAYAK_ITEMS_GENERATOR'>\n");
                            headerbuf.append("<bounds minlat='" + aMinLat +"' minlon='" + aMinLon +"' maxlat='"+ aMaxLat +"' maxlon='"+ aMaxLon+"' />\n");
                            String aHeaderString = headerbuf.toString();
                            fileBufWriter.write(aHeaderString);
                            // header and bounding box written


                            int aNegativeNodeID = -10000;
                            String aNodeIdStr = Integer.toString(aNegativeNodeID);
                            cursor.moveToFirst();
                            aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
                            aNumberStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_NUMBER));
                            aTypeStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_TYPE));
                            aNameStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_NAME));
                            aDescriptionStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_DESCRIPTION));
                            aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
                            aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
                            /*
                            KayakInfoItem(LatLong pPoint, int pNumber,String pTypeStr,
			              String pName ,  String pDescription , long pUTC)
                             */
                            LatLong aLatLong = new LatLong((Double.parseDouble(aLATStr)), Double.parseDouble(aLONStr));

                            StringBuffer bufPoint = new StringBuffer();
                            // <node id='-10000' action='modify' visible='true' lat='51.442762' lon='6.960523'>
                            // <tag k='whitewater' v='put_in' />
                            // <tag k='whitewater:description v='Papenheim' />
                            // <tag k='whitewater:name'  v='something' />
                            // <tag k='source' v='GPS'/>
                            // </node>


                            bufPoint.append("\n   <node "

                                            + "id='" + aNodeIdStr + "' action='modify' visible='true' lat='" + aLatLong.latitude + "' lon='" + aLatLong.longitude + "'> \n"
                                            + "      <tag k='whitewater' v='" + aTypeStr +"' />\n"
                                            + "      <tag k='whitewater:name' v='" + aNameStr + "' />\n"
                                            + "      <tag k='whitewater:description' v='" + aDescriptionStr + "' />\n"
                            );


                            bufPoint.append(  "      <tag k='source' v='GPS'/>\n"
                                            + "   </node>\n");


                            String aString = bufPoint.toString();
                            fileBufWriter.write(aString);
                            while (cursor.moveToNext()) {
                                aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
                                aNumberStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_NUMBER));
                                aTypeStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_TYPE));
                                aNameStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_NAME));
                                aDescriptionStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_WW_DESCRIPTION));
                                aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
                                aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));


                                double lat = Double.parseDouble(aLATStr);
                                double lon = Double.parseDouble(aLONStr);
                                aLatLong = new LatLong((Double.parseDouble(aLATStr)), Double.parseDouble(aLONStr));
                                aNegativeNodeID --;
                                aNodeIdStr = Integer.toString(aNegativeNodeID);
                                bufPoint = new StringBuffer();
                                bufPoint.append("\n   <node "

                                                + "id='" + aNodeIdStr + "' action='modify' visible='true' lat='" + aLatLong.latitude + "' lon='" + aLatLong.longitude + "'> \n"
                                                + "      <tag k='whitewater' v='" + aTypeStr +"' />\n"
                                                + "      <tag k='whitewater:name' v='" + aNameStr + "' />\n"
                                                + "      <tag k='whitewater:description' v='" + aDescriptionStr + "' />\n"
                                );


                                bufPoint.append("<tag k='source' v='GPS'/>\n"
                                        + "</node>\n");

                                aString = bufPoint.toString();
                                fileBufWriter.write(aString);

                            } // while cursor(moveToNext())
                            fileBufWriter.write("</osm>");
                        }
                        cursor.close();

                        fileBufWriter.flush();
                        fileBufWriter.close();
                        if (test)
                            Log.v(TAG, "file write sucessfull " + filePathStr);
                        //showToastOnUiThread("Fixed depth soundings saved to " + fileName);

                    } catch (IOException e) {
                        //showToastOnUiThread(" could not write fixed depth soundings to " + filePathStr);

                        // Unable to create file, likely because external storage is
                        // not currently mounted.
                        if (test)
                            Log.w("TAG", "Error writing " + filePathStr);
                    } catch (Exception e) {
                        //showToastOnUiThread(" could not write fixed depth soundings to " + filePathStr);

                        // Unable to create file, likely because external storage is
                        // not currently mounted.
                        if (test)
                            Log.w("TAG", "Other Error writing " + filePathStr);
                    }finally {

                    } // finally
                } // if media mounted
            } // run
        }).start();
    }




    /**
     *
     * @param pFile
     *    aFile which contains a route, selected by FilePicker
     */
    public void prepareLoadFixedKayakItems_JOSM_Layer(String pTableName, File pFile) {

        String aInfo = mContext.getResources().getString(R.string.guiupdate_info_error_fixed_kayak_could_not_load_data_for_JOSM_Layer);

        loadFixedKayakItemsDataWithThread(pTableName,pFile);

        //showToastOnUiThread(aInfo);

    }


    /**
     *
     * @param pFile a JOSM Layer file that contains the depth soundings, selected via the filepicker
     *    load the fixed depth soundings  from aFile to the daatbase
     *    uses a thread
     *    visual feedback to the user with progress bar
     */

    private void loadFixedKayakItemsDataWithThread(String pTableName, File pFile) {

        final String aFilename = pFile.getAbsolutePath();
        final String aTableName = pTableName;
        final String aUIMsg = mContext.getResources().getString(R.string.guiupdate_info_fixed_kayak_failed_in_parsing_JOSM_Layer_file);

        new Thread(new Runnable() {

            public void run() {

                try {
                    int count = 1000;
                    int updateProgressbar = count / 500; // The progressbar is 0 to 100,
                    // we calculate the counter if we have to update
                    int updateCounter = 0;
                    XmlPullParserFactory parserCreator;

                    parserCreator = XmlPullParserFactory.newInstance();

                    XmlPullParser parser = parserCreator.newPullParser();
                    FileReader myReader = new FileReader(aFilename);
                    parser.setInput(myReader);
                    // parser.setInput(text.openStream(), null);

                    int parserEvent = parser.getEventType();
                    // we first check how much entries are in the Table, we use this to ennumerate the new entries
                    String aIdentifier = aTableName;
                    Cursor mFixedKayakItemsCursor = mDbAdapter.fetchFixedWhitewaterAllEntriesNumberAndName(aIdentifier);
                    int numberOfPointsInTable = mFixedKayakItemsCursor.getCount() ;
                    int nodeCounter = numberOfPointsInTable;

                    String aLATStr = null;;
                    String aLONStr = null;
                    SeamarkNode theCurrentSeamarkNode = null;
                    String aNumberStr = Integer.toString(nodeCounter);
                    // Parse the XML returned from the file
                    while (parserEvent != XmlPullParser.END_DOCUMENT) {

                        //Log.i(TAG,"Parser EventType " + parserEvent + " name " + parser.getName());
                        switch (parserEvent) {
                            case XmlPullParser.START_TAG:
                                String tag = parser.getName();

                                if (tag.compareTo("node") == 0) {
                                    nodeCounter++;
                                    aNumberStr = Integer.toString(nodeCounter);
                                    theCurrentSeamarkNode = new SeamarkNode(aNumberStr);

                                    aLATStr = parser.getAttributeValue(null, "lat");
                                    aLONStr = parser.getAttributeValue(null, "lon");
                                    int latE6 = (int)(Double.parseDouble(aLATStr)* 1E6);
                                    theCurrentSeamarkNode.setLatitudeE6(latE6);
                                    int lonE6 = (int)(Double.parseDouble(aLONStr)* 1E6);
                                    theCurrentSeamarkNode.setLongitudeE6(lonE6);
                                    if (test)
                                        Log.i(TAG, "  node=" + nodeCounter + " latitude=" + aLATStr
                                                + " longitude=" + aLONStr);
                                    updateCounter++;

                                } else if (tag.compareTo("tag") == 0) {
                                    String key = parser.getAttributeValue(null, "k");
                                    String value = parser.getAttributeValue(null, "v");
                                    Tag aTypeTag = new Tag(key,value);
                                    if (theCurrentSeamarkNode != null) theCurrentSeamarkNode.addTag(aTypeTag);

                                }
                                break;
                            case XmlPullParser.END_TAG:
                                tag = parser.getName();
                                //Log.i(TAG,tag);
                                if (tag.compareTo("node") == 0) {
                                    if (theCurrentSeamarkNode != null){
                                        String aTypeStr = theCurrentSeamarkNode.getValueToKey("whitewater");
                                        String aNameStr = theCurrentSeamarkNode.getValueToKey("whitewater:name");
                                        String aDescriptionStr = theCurrentSeamarkNode.getValueToKey("whitewater:description");

                                        mDbAdapter.insertPointToFixedWhitewaterTable(aIdentifier,aNumberStr,aTypeStr,aNameStr,aDescriptionStr,"",aLATStr,aLONStr);

                                    }

                                    // put the node into the Fixed_bouys database
                                }
                        }

                        parserEvent = parser.next();
                    }

                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found");
                } catch (Exception e) {
                    Log.i(TAG, "Failed in parsing XML", e);
                    //showToastOnUiThread(aUIMsg);

                } // try

            } // run
        }).start();

    }








}
