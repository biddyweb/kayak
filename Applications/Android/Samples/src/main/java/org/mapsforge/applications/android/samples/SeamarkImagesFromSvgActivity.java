package org.mapsforge.applications.android.samples;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.shapes.PathShape;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.mapsforge.applications.android.samples.common.FilterByFileExtension;
import org.mapsforge.applications.android.samples.common.PositionTools;
import org.mapsforge.applications.android.samples.filefilter.ValidSeamarkFile;
import org.mapsforge.applications.android.samples.filepicker.FilePicker;
import org.mapsforge.applications.android.samples.seamark.SeamarkDrawable;
import org.mapsforge.applications.android.samples.seamark.SeamarkNode;
import org.mapsforge.applications.android.samples.seamark.SeamarkOSM;
import org.mapsforge.applications.android.samples.seamark.SeamarkSymbol;
import org.mapsforge.core.model.Tag;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;


public class SeamarkImagesFromSvgActivity extends Activity {
	private static final String TAG = "SeamarkSymbolsFromSvgTest";
	private static final boolean test = false;
	private static final int minOccurrency = 0;
	long mUsedTimeToPreload = 0;
    private SeamarkOSM mSeamarkOSM = null;
	private LinkedHashMap<String,SeamarkNode> mSeamarksDictionary = null;
	private ArrayList<SeamarkNode> mSeamarksList = null;
	private static Handler mReadSeamarksHandler = new Handler();
	private static Handler mReadSeamarkSymbolsHandler = new Handler();
	private static final FileFilter FILE_FILTER_EXTENSION_XML = new FilterByFileExtension(".xml");
	private static final FileFilter FILE_FILTER_EXTENSION_DAT = new FilterByFileExtension(".dat");
	private static final int SELECT_SEAMARKS_FILE = 2;
	private String mSeamarkFilePath = "";
	private String mFilePath = "";
	private boolean mCanWork = false;
	private TextView mTextView= null; 
	private Button mSaveFrequentSymbolsBtn=null;
	private Button mSaveOSMTagMappingBtn=null;
	
	private LinkedHashMap<String,SeamarkDrawableTypeCounter> mSeamarkDrawableTypeCounterDictionary =null;
    private LinkedHashMap<String,ButtonImageViewEntry> mButtonImageViewDictionary ;
    private ArrayList<ButtonImageViewEntry> mButtonImageViewEntryList;
    private String mExternalPathName = "";
	private File mExternalStorageDir = null;
	private String mImagesDirName="";
	private static final String SEAMARK_IMAGES_DIR_NAME =  "seamark_symbols";
    public static final String DEFAULT_SEAMARKS_SYMBOL_FILENAME = "symbols.xml";
    public static final String DEFAULT_APP_DATA_DIRECTORY ="Test_OpenSeaMapSamples";



    @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_show_seamark_images_scroll_view);
	        mSeamarkDrawableTypeCounterDictionary = new LinkedHashMap<String,SeamarkDrawableTypeCounter>();
	        mButtonImageViewDictionary= new LinkedHashMap<String,ButtonImageViewEntry>();
	       
	        mSeamarkOSM = new SeamarkOSM(this);
	        
	        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	 	    paint.setStyle(Paint.Style.STROKE);
	 	    paint.setColor(Color.BLACK);
	 	    paint.setStrokeWidth(5);
	 	   // mUsedTimeToPreload = SeamarkSymbol.preloadSymbols();
	 	    mUsedTimeToPreload = SeamarkSymbol.preloadFromDefsFile(DEFAULT_SEAMARKS_SYMBOL_FILENAME);
	        
	        mTextView = (TextView)findViewById(R.id.svg_textview);
            mExternalStorageDir = Environment.getExternalStorageDirectory();
	        // mExternalStorageDir = Environment2.getCardDirectory();
			mExternalPathName = mExternalStorageDir.getAbsolutePath();
			Log.i(TAG,"externalPathName: "  + mExternalPathName); 
			//String appNameDir = "/" + AISPlotterGlobals.DEFAULT_APP_DATA_DIRECTORY;  // AISPlotter
			String imagesDirName = "/" + DEFAULT_APP_DATA_DIRECTORY +"/" + SEAMARK_IMAGES_DIR_NAME;        // AISPlotter/Cachedata/EniroAerial
			mImagesDirName =imagesDirName;
			Log.i(TAG,"ImagesDirName: "  + imagesDirName);

			PositionTools.createExternalDirectoryIfNecessary(imagesDirName);
			
	        mReadSeamarksHandler.postDelayed(waitReadSeamarkSymbolsComplete, 1);
	        
	        /*String[] names = {//"Light_Major"
	        		         "Barrel","Can","Cone","Float","LandTower","Light","Light_House","Light_Major",
		                     "Light_Minor","Pillar","Spar","Sphere","Stake",	
	        };*/
	        //LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
	        //showExample();
	        //startSeamarksFilePicker();
	        //showAllSymbols(layout);
	 }
	 
	 private Runnable waitReadSeamarkSymbolsComplete = new Runnable() {
			public void run() {
				if (SeamarkSymbol.getSymbolDefsComplete()){
					Log.d(TAG,"loading Symbols file finished");
					mTextView.append("display seamarkImages from file\n");
					startSeamarksFilePicker();
					//showExample();
				} else {
					// try again
                    String svgFilePath = SeamarkSymbol.getSVG_File_Name(DEFAULT_SEAMARKS_SYMBOL_FILENAME);
                    File aFile = new File(svgFilePath);
                    if (aFile.exists()) {
                        String msg ="wait while loading Symbols file \n" ;
                        Log.d(TAG,msg);
                        mTextView.append(msg);
                        mReadSeamarkSymbolsHandler.postDelayed(this,1000);
                    } else {
                        String msg ="loading Symbols file failed\n provided filename: " + svgFilePath ;
                        Log.d(TAG,msg);
                        mTextView.append(msg);
                    }

				}
			}
		}; 
	 
	 public void setSeamarkFilePathAndRead ( String aPath ) {
			mSeamarkOSM.clear();
			mSeamarkFilePath = aPath;
			
			if (aPath.endsWith(".xml")){
				File xmlFile = new File (aPath);
		    	if (xmlFile.exists()) {
		    		mSeamarkFilePath = aPath;
		    		mSeamarkOSM.readSeamarkFile(aPath);
		    	} else {
		    		String info = "no seamarks file: " + aPath;
		    		showToastOnUiThread(info);
		    	}
			}
			if (aPath.endsWith(".dat")){
				File xmlFile = new File (aPath);
		    	if (xmlFile.exists()) {
		    		mSeamarkFilePath = aPath;
		    		mSeamarkOSM.readSeamarkFile(aPath);
		    	} else {
		    		String info = "no seamarks file: " + aPath;
		    		showToastOnUiThread(info);
		    	}
			}
		}
	 
	 
	 private void showExample(){
		 /*
		  * <node id="291886347" lat="52.846269" lon="5.306536">
			<tag k="name" v="LC3"/>
			<tag k="seamark" v="beacon"/>
			<tag k="seamark:buoy_lateral:category" v="starboard"/>
			<tag k="seamark:buoy_lateral:colour" v="green"/>
			<tag k="seamark:buoy_lateral:shape" v="conical"/>
			<tag k="seamark:name" v="LC 3"/>
			<tag k="seamark:topmark:colour" v="green"/>
			<tag k="seamark:topmark:shape" v="cone, point up"/>
			<tag k="seamark:type" v="buoy_lateral"/>
			<tag k="source" v="Rijkswaterstaat.nl"/>
	</node>

		  */
		 SeamarkNode aSeamarkNode = new SeamarkNode("1");
		 aSeamarkNode.addTag((new Tag("seamark:buoy_lateral:category", "starboard")));
		 aSeamarkNode.addTag((new Tag("seamark:buoy_lateral:colour" ,"green")));
		 aSeamarkNode.addTag((new Tag("seamark:buoy_lateral:shape","conical")));
		 aSeamarkNode.addTag((new Tag("seamark:topmark:shape","cone, point up")));
		 aSeamarkNode.addTag((new Tag("seamark:topmark:colour","green")));
		 aSeamarkNode.addTag((new Tag("seamark:type" ,"buoy_lateral")));
		 aSeamarkNode.addTag((new Tag("seamark:name","LC 3")));
		 byte aZoomLevel = 15;
		 float aDisplayFactor = 1.0f;
		 SeamarkDrawable aSeamarkDrawable = new SeamarkDrawable(aSeamarkNode,aZoomLevel,aDisplayFactor);
		 showDrawable(aSeamarkDrawable);
	 }
	 
	 private void showDrawable(SeamarkDrawable pSeamarkDrawable){
		 LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		 Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	 	    paint.setStyle(Paint.Style.FILL);
	 	    paint.setColor(Color.WHITE);
	 	    paint.setStrokeWidth(5); 
	 	    ImageView imageView = new ImageView(this);
	 	    
	 	    Bitmap seamarkBitmap = pSeamarkDrawable.getBitmap();
	 	    int bitmapHeight = seamarkBitmap.getHeight();
	 	    int bitmapWidth = seamarkBitmap.getWidth();
	 	    Bitmap aNewBitmap = Bitmap.createBitmap(bitmapWidth,bitmapHeight,Bitmap.Config.ARGB_8888);
	 	    Canvas aCanvas = new Canvas(aNewBitmap);
	 	    RectF rectF = new RectF(1,1,bitmapWidth - 1, bitmapHeight-1);
	 	    aCanvas.drawRect(rectF, paint);
	 	    aCanvas.drawBitmap(seamarkBitmap, 0, 0, null);
	 	    imageView.setImageBitmap(aNewBitmap);
           
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                   LinearLayout.LayoutParams.FILL_PARENT,
                   LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layout.addView(imageView, p);
	 }
	 
	 private void createDrawableAndButton(int pNumber,SeamarkDrawable pSeamarkDrawable, String pShapeIdentifier){
		 LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		 Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	 	    paint.setStyle(Paint.Style.FILL);
	 	    paint.setColor(Color.WHITE);
	 	    paint.setStrokeWidth(5); 
	 	    ImageView imageView = new ImageView(this);
	 	    
	 	    Bitmap seamarkBitmap = pSeamarkDrawable.getBitmap();
	 	   
	 	    int bitmapHeight = 60;
	 	    int bitmapWidth = 80; 
	 	    if (seamarkBitmap != null) {
	 	    	bitmapHeight = seamarkBitmap.getHeight();
	 	    	bitmapWidth =  seamarkBitmap.getWidth();
	 	    }
	 	    Bitmap aNewBitmap = Bitmap.createBitmap(bitmapWidth,bitmapHeight,Bitmap.Config.ARGB_8888);
	 	    Canvas aCanvas = new Canvas(aNewBitmap);
	 	    RectF rectF = new RectF(1,1,bitmapWidth - 1, bitmapHeight-1);
	 	    aCanvas.drawRect(rectF, paint);
	 	    if (seamarkBitmap != null){
	 		   aCanvas.drawBitmap(seamarkBitmap, 0, 0, null);
	 	    }
	 	    imageView.setImageBitmap(aNewBitmap);
	 	    Button buttonView = new Button(this);
            buttonView.setText(pShapeIdentifier  );
           //buttonView.setText("nr: " + Integer.toString(i) + " SymbolName: " + aShapeIdentifier + "\nl "+ seamarkDescription );
            buttonView.setOnClickListener(onButtonSaveClick);
            buttonView.setGravity(Gravity.LEFT);
            //layout.addView(buttonView, p);
            ButtonImageViewEntry aButtonImageViewEntry = new ButtonImageViewEntry(pShapeIdentifier,imageView,buttonView,seamarkBitmap);
            mButtonImageViewDictionary.put(pShapeIdentifier,aButtonImageViewEntry );
            
	 }
	 
	 final View.OnClickListener onButtonSaveClick=new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonSaveServicePressed((Button) v);
			}
		};
		
     final View.OnClickListener onButtonSaveFrequentSymbolsClick=new View.OnClickListener() {
    	 @Override
    	 public void onClick (View v) {
    		 buttonSaveFrequentSymbolsPressed();
    	 }
     };
     
     final View.OnClickListener onButtonSaveTagMappingsClick=new View.OnClickListener() {
    	 @Override
    	 public void onClick (View v) {
    		 listSymbolsDir();
    	 }
     };
     
     private void buttonSaveFrequentSymbolsPressed() {
    	 int count = 0;
    	 Log.d(TAG,"buttonSaveFeqoentSymbolsPressed begin");
    	 if (mButtonImageViewEntryList != null ){
    		 int countElements = mButtonImageViewEntryList.size();
    		 Log.d(TAG,"mButtonImageViewEntryList.size= " + countElements);
	    	 for (ButtonImageViewEntry aButtonImageView : mButtonImageViewEntryList){ 
	    			String aIdentifier = aButtonImageView.getIdentifier();
	    			int countOccurrences = aButtonImageView.getOccurrences();
	    			Bitmap aSymbolBitmap = aButtonImageView.getBitmap();
	    			if (countOccurrences > minOccurrency ) {
	    				String aFilePath = mExternalPathName + mImagesDirName + "/" + aIdentifier ; 
	     	 			File aTestFile = new File(aFilePath+".png");
	     	 			if (!aTestFile.exists()){
	     	 				writeImageToFile(aFilePath,aSymbolBitmap);
	     	 				Log.d(TAG,"save symbol "+ aIdentifier);
	     	 				count++;
	     	 			}
	    			}
	    			
	    	 }
	    	 if (count > 0 ){
	    		this.showToastOnUiThread(count + " Symbols saved to " + mExternalPathName + mImagesDirName) ;
	    	 } else {
	    		 this.showToastOnUiThread( " all Symbols exists with n >  "+ minOccurrency) ; 
	    	 }
	    	 
    	 }
     }
     
     private void listSymbolsDir() {
    	 String aFilePath = mExternalPathName + mImagesDirName ;
    	 File aSymbolDir = new File(aFilePath);
    	 ArrayList<String> aFilenameList = new  ArrayList<String>();
    	 String[] chld = aSymbolDir.list();
    	 if(chld == null){
    	     Log.d(TAG,"Specified directory does not exist or is not a directory.");
    	 } else {
    	    for(int i = 0; i < chld.length; i++){
    	      String fileName = chld[i];
    	      aFilenameList.add(fileName);
    	      Log.d(TAG,fileName);
    	    }
    	    String aTagMappingFilename ="tagmappings001";
    	    String aDirPath = "/" + DEFAULT_APP_DATA_DIRECTORY +"/" + "OSM-TAG_MAPPING/";
    	    saveSymbolDataNamesToFile(aTagMappingFilename, aDirPath,aFilenameList) ;
    	}
     }
     
     /**
 	 * 
 	 * @param pFilename
 	 * @param pDirPath
 	 */
 	@SuppressLint("DefaultLocale")
 	private void saveSymbolDataNamesToFile(String pFilename, String pDirPath, ArrayList<String> pFilenameList) {
 		if (test)
 			Log.v(TAG, "save SymbolDataNames");
 		final String aDirPath = pDirPath;
 		final String aFilename = pFilename;
 		final  ArrayList<String> aFilenameList = pFilenameList;
 		
 		new Thread(new Runnable() { // as we want response to the user we must use a separate thread
 					@SuppressLint("DefaultLocale")
 					public void run() {
 						PositionTools.createExternalDirectoryIfNecessary(aDirPath);
 						String result = Environment.getExternalStorageState();
 						if (result.equals(Environment.MEDIA_MOUNTED)) {
 							File path = PositionTools.getExternalStorageDir();
 							StringBuffer buf = new StringBuffer();
 							buf.append(aDirPath);
 							buf.append("/TAG_MAPPING_");
 							buf.append(aFilename);
 							buf.append(".xml");
 							String fileName = buf.toString();
 							File file = new File(path, fileName);
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
 								
 								
 									StringBuffer headerbuf = new StringBuffer();
 									headerbuf.append("<?xml version='1.0' encoding='UTF-8'?>\n");
 									headerbuf.append("<osm version='0.6' generator='TAG_MAPPING_GENERATOR'>\n");
 									
 									String aHeaderString = headerbuf.toString();
 									fileBufWriter.write(aHeaderString);
 									
 									// <pois> 
 									// <osm-tag key="seamark:type" value ="beacon_lateral" zoom-appear="10"/>   
                                    // </pois>
 									fileBufWriter.write("<pois>");
 								    fileBufWriter.write("<!-- osm tags for seamark_symbols -->");
 									for (int index=0;index < aFilenameList.size(); index++){
 										String aSeamarkTypeIdentifier = aFilenameList.get(index);
 										StringBuffer tagMapEntryBuf = new StringBuffer();
 										tagMapEntryBuf.append("\n <osm-tag "
 												+ "key='seamark:type_identifier' " 
 												+ "value='" + aSeamarkTypeIdentifier +"' zoom-appear='8'" 
 												+ "/>"
 												);
 										
 										String aString = tagMapEntryBuf.toString();
 										fileBufWriter.write(aString);
 									}
 								
                                    fileBufWriter.write("\n</pois>");
                                    fileBufWriter.write("\n</osm>");
 								
 								
 								
 								fileBufWriter.flush();
 								fileBufWriter.close();
 								if (test)
 									Log.v(TAG, "file write sucessfull " + filePathStr);
 								showToastOnUiThread("osm tag mapping saved  to " + fileName);

 							} catch (IOException e) {
                                 showToastOnUiThread(" could not write osm tag mapping to " + filePathStr);
 								Log.d(TAG,e.toString());
 								// Unable to create file, likely because external storage is
 								// not currently mounted.
 								if (test)
 									Log.w("TAG", "Error writing " + filePathStr);
 							} catch (Exception e) {
 								showToastOnUiThread(" could not write osm tag mapping to " + filePathStr);
 								Log.d(TAG,e.toString());
 								// Unable to create file, likely because external storage is
 								// not currently mounted.
 								if (test)
 									Log.w("TAG", "Other Error writing " + filePathStr);
 							}
 						} // if media mounted
 					} // run
 				}).start();
 	}
 	
		
	  private void buttonSaveServicePressed(Button pButton){
	 	 String aText = pButton.getText().toString();
	 	 Log.i(TAG,"Button pressed " + aText);
	 	 String [] fields = aText.split(":");
	 	 String aIdentifier = "";
	 	 if (fields.length > 1) {
	 		aIdentifier = fields[0]; 
	 	 }
	 	 Log.i(TAG," Image Identifier " + aIdentifier);
	 	 /*try {
		 	 aText = aText.replace(",", "_");
		 	 aText = aText.replace(" ", "_");
		 	aText = aText.replace(";", "_");
	 	 } catch (NullPointerException e) {};*/
	 	 Log.i(TAG," filename " + aIdentifier);
	 	 String aFilename = aIdentifier;
	 	 ButtonImageViewEntry aButtonImageViewEntry = mButtonImageViewDictionary.get( aIdentifier);
	 	 if (aButtonImageViewEntry != null){
	 		 //ImageView aImageView = aButtonImageViewEntry.getImageView();
	 		 //boolean isDrawingEnabled = aImageView.isDrawingCacheEnabled();
	 		 //aImageView.setDrawingCacheEnabled(true);
	 		 Bitmap aBitmap = aButtonImageViewEntry.getBitmap();
	 		 if (aBitmap != null){
	 			String aFilePath = mExternalPathName + mImagesDirName + "/" + aFilename ; 
	 			File aTestFile = new File(aFilePath+".png");
	 			if (aTestFile.exists()){
	 				Log.i(TAG,"file exists " + aFilePath);
	 				this.showToastOnUiThread("File: " +aFilename + " exists!");
	 			} else {
	 				writeImageToFile(aFilePath,aBitmap);
	 			}
	 			
	 		 }
	 		 //aImageView.setDrawingCacheEnabled(isDrawingEnabled);
	 	 }
	 	 
	  }
	 
	  private void writeImageToFile (String imageFilePathName, Bitmap bitmap){
			OutputStream os = null;
			try {
				String filename = imageFilePathName+ ".png";
			    os = new FileOutputStream(filename );
			    boolean ok = bitmap.compress(CompressFormat.PNG, 50, os); // PNG: 50 is ignored
			    if (ok) Log.i(TAG,"file created " + filename);
			} catch(IOException e) {
			    e.printStackTrace();
			    if (test) {
			    	Log.d(TAG,e.toString());
			    }
			} finally {
				if(os!=null){
					try {
					 os.close();
					} catch (IOException e) {}
				}
			}
		}
	 
private void showAllSeamarkImages() {
	        TextView aTextView = mTextView;
	        //aTextView.append("\nTime to preload symbols: " + mUsedTimeToPreload + " ms\n");
            String aFilePath = mExternalPathName + mImagesDirName ;
            aTextView.append("\n seamark_symbols directory: " + aFilePath);
	        aTextView.append("\n nr of  nodes: " + mSeamarksList.size() );
          int l;
          int max =8000;
          if (mSeamarksList.size() < max){
         	 l=mSeamarksList.size();
          }else {
         	 l= max;
         	 this.showToastOnUiThread(mSeamarksList.size() + "seamarks  clipped to" + max );
          }
          
	       for (int i = 0; i <l ;i++){
	    	   //Log.d(TAG,"working on node " + i);
	    	  
	            SeamarkNode aSeamarkNode = mSeamarksList.get(i);
	            String aShapeIdentifier = aSeamarkNode.getShapeIdentifier();
	            String aShortShapeIdentifier = calculateShortShapeIdentifier(aShapeIdentifier);
	            aShapeIdentifier = aShortShapeIdentifier;
	            if (aShapeIdentifier == null) {
	                String aTypeValue  = aSeamarkNode.getValueToKey("seamark:type");
	                if(test)Log.d(TAG,"node type " +aTypeValue);
	                /*if (aTypeValue != null){
	                	Log.d(TAG,"adding button " + i);
	                	Button buttonView = new Button(this);
			            buttonView.setText("nr: " + Integer.toString(i) + " SymbolType: " + aTypeValue);
			            buttonView.setGravity(Gravity.LEFT);
			            layout.addView(buttonView, p);
	                }*/
	            } else {
	            	countKeys (aShapeIdentifier);
		            byte aZoomLevel = 15;
		   		    float aDisplayFactor = 1.0f;
		   		    
		   		    
		   		    SeamarkDrawable aSeamarkDrawable = new SeamarkDrawable(aSeamarkNode,aZoomLevel,aDisplayFactor);
		   		    SeamarkDrawableTypeCounter aTypeCounter = (SeamarkDrawableTypeCounter)mSeamarkDrawableTypeCounterDictionary.get(aShapeIdentifier);
		   		    int aCount = aTypeCounter.getCount() ;
		   		    if(test)Log.d(TAG,"identifier  " + aShapeIdentifier + " count "+ aCount);
		   		    if (aCount == 1){ // we have a new one 
		   		    	if (test)Log.d(TAG,"adding image and button " + i);
			            createDrawableAndButton(i,aSeamarkDrawable,aShapeIdentifier);

			            //String seamarkDescription = aSeamarkNode.tagsToString();
		   		    } // aCount == 1
	            } // else
	        } //for

	       sortToListAndDisplay();
	   	   
	 }

/**
 * get a shorted Type key 
 * @param key
 * @return
 */

private String getShortTypeKey (String key ){
	String result = "";
	if (key.equals("buoy_lateral"))                   result = "buoy_lat";
		 else if (key.equals("buoy_cardinal"))            result = "buoy_car";
		 else if (key.equals("beacon_lateral"))           result = "beac_lat";
		 else if (key.equals("beacon_cardinal"))          result = "beac_car";
		 else if (key.equals("beacon_special_purpose"))   result = "beac_spe";
		 else if (key.equals("buoy_special_purpose"))     result = "buoy_spe";
		 else if (key.equals("buoy_safe_water"))          result = "buoy_saf";
		 else if (key.equals("mooring"))                  result = "mooring";
		 else if (key.equals("distance_mark"))            result = "dist_m";
		 else {
			 result = key;
		 }
	return result;
}

private String makeShortMultiColourString(String colourStr){
	String result = "";
	StringBuffer buf = new StringBuffer();
	String[] colours = null;
	 colours = colourStr.split(";");
	 if (colours != null && colours.length > 0) {
		 for (int i = 0; i < colours.length;i++) {
			 String aColour= colours[i];
			 if (aColour.equals( "green")) { buf.append("g");}
			 else if (aColour.equals("red")) {buf.append("r");}
			 else if (aColour.equals("yellow")) { buf.append("y");}
			 else if (aColour.equals("white")) {buf.append("w");}
			 else if (aColour.equals("black")) {buf.append("b");}
			 else buf.append(aColour);
		 }
		result = buf.toString();
	 }
	return result;
}

/**
 * calculate a short Identifier from pShapeIdentifier used from 2�14_10_30 
 * @param pShapeIdentifier
 * @return
 */
private String calculateShortShapeIdentifier(String pShapeIdentifier){
	 String result = null;
	 // as something in the chain does not accept = we use __ as a keyValDelimiter
	 String keyValPairDelimiter ="---";
	 String keyValDelimiter="__";
	 StringBuffer buf = new StringBuffer();
	 if (pShapeIdentifier != null) {
	   String [] fields = pShapeIdentifier.split("!!!");
	   int count = fields.length;
	   for (int index = 0; index < count;index++){
		 String aKeyValPair = fields[index];
		 if (aKeyValPair.contains("=")) {
			 String[] temp = fields[index].split("="); 
			 String key= temp[0];
			 String value = temp[1];
		 
		 if (key.equals("type")) {
			 String t_key="t";
			 buf.append(t_key+ keyValDelimiter+getShortTypeKey(value));
			 
		 } // key.equals type 
		 else if (key.equals("category")){
			 String cat_key = "cat";
			 if (value.equals("dolphin")) buf.append(cat_key+keyValDelimiter+"dolp");
			 else {
				 buf.append("cat__"+value);
			 }
		 }
		 else if (key.equals("shape")) {
			 String sh_key= "sh";
			 buf.append(sh_key+ keyValDelimiter+value);
		 }
		 else if (key.equals("colour")) {
			 String c_key = "c";
			 if (value.equals("red")) {buf.append(c_key + keyValDelimiter+"r");} 
			 else if (value.equals("green")) {buf.append(c_key + keyValDelimiter+"g");}
			 else if (value.equals("yellow")) {buf.append(c_key + keyValDelimiter+"y");}
			 else if (value.equals("black")) buf.append(c_key + keyValDelimiter+"b");
			 else if (value.equals("white")) buf.append(c_key + keyValDelimiter+"w");
			 else if (value.contains(";")) { buf.append(c_key + keyValDelimiter+makeShortMultiColourString(value));} 
			 else buf.append(c_key + keyValDelimiter+value);
		 }
		 else if (key.equals("colour_pattern")){
			 String c_pat_Key = "c_pat";
			 if (value.equals("horizontal")) buf.append(c_pat_Key + keyValDelimiter+"hor");
			 else if (value.equals("colour_pattern=vertical")) buf.append(c_pat_Key + keyValDelimiter+"ver"); 
			 else buf.append(c_pat_Key + keyValDelimiter+value);
		 }
		 else if (key.equals("daymark_colour") || key.equals("topmark_colour")){
			 String top_c_Key= "top_c";
			 if (value.equals("green")) buf.append(top_c_Key + keyValDelimiter+"g");
			 else if (value.equals("red")) buf.append(top_c_Key + keyValDelimiter+"r");
			 else if (value.equals("yellow")) buf.append(top_c_Key + keyValDelimiter+"y");
			 else if (value.equals("black")) buf.append(top_c_Key + keyValDelimiter+"b");
			 else if (value.equals("white")) buf.append(top_c_Key + keyValDelimiter+"w");
			 else if (value.contains(";")) { buf.append(top_c_Key + keyValDelimiter+makeShortMultiColourString(value));} 
			 else buf.append(top_c_Key + keyValDelimiter+value);
		 }
		 else if (key.equals("daymark_shape") || key.equals("topmark_shape")) {
			 String top_s_Key = "top_s";
			 if (value.equals("triangle, point up")) buf.append(top_s_Key + keyValDelimiter+"tri_p_u");
			 else if (value.equals("triangle, point down")) buf.append(top_s_Key + keyValDelimiter+"tri_p_d");
			 else if (value.equals("cone, point up")) buf.append(top_s_Key + keyValDelimiter+"cone_p_u");
			 else if (value.equals("cone, point down")) buf.append(top_s_Key + keyValDelimiter+"cone_p_d");
			 else if (value.equals("cylinder")) buf.append(top_s_Key + keyValDelimiter+"cyl");
			 else if (value.equals("x-shape")) buf.append(top_s_Key + keyValDelimiter+"x_s");
			 else if (value.equals("2 cones down")) buf.append(top_s_Key + keyValDelimiter+"2_cones_d");
			 else if (value.equals("2 cones point together")) buf.append(top_s_Key + keyValDelimiter+"2_cones_p_tog");
			 else if (value.equals("2 cones base together")) buf.append(top_s_Key + keyValDelimiter+"2_cones_b_tog");
			 else if (value.equals("2 cones up")) buf.append(top_s_Key + keyValDelimiter+"2_cones_up");
			 else buf.append(top_s_Key + keyValDelimiter+value);
		 }
		 else buf.append(key+keyValDelimiter+value);
		 
		 
		 buf.append(keyValPairDelimiter);
		 } // keyValPair contains =
	 }
	 result = buf.toString();
	 if (result.length() > 3 && result.endsWith(keyValPairDelimiter)){
		 result = result.substring(0,result.length()-3);
	 }
	 } // pShapeIdentifier != null
	 return result;
 }



/**
 * calculate a short Identifier from pShapeIdentifier used till 2014_10_30
 * @param pShapeIdentifier
 * @return
 */

private String calculateShortShapeIdentifier2old(String pShapeIdentifier){
	 String result = null;
	 // as something in the chain does not accept = we use __ as a keyValDelimiter
	 String keyValPairDelimiter ="---";
	 String keyValDelimiter="__";
	 StringBuffer buf = new StringBuffer();
	 if (pShapeIdentifier != null) {
	   String [] fields = pShapeIdentifier.split("!!!");
	   int count = fields.length;
	   for (int index = 0; index < count;index++){
		 String aKeyValPair = fields[index];
		 if (aKeyValPair.contains("=")) {
			 String[] temp = fields[index].split("="); 
			 String key= temp[0];
			 String value = temp[1];
		 
		 if (key.equals("type")) {
			 String t_key="t";
			 if (value.equals("buoy_lateral")) buf.append(t_key+ keyValDelimiter+"buoy_lat");
			 else if (value.equals("buoy_cardinal")) buf.append(t_key+keyValDelimiter+"buoy_car");
			 else if (value.equals("beacon_lateral")) buf.append(t_key+keyValDelimiter+"beac_lat");
			 else if (value.equals("beacon_cardinal")) buf.append(t_key+keyValDelimiter+"beac_car");
			 else if (value.equals("buoy_special_purpose")) buf.append(t_key+keyValDelimiter+"buoy_spe");
			 else if (value.equals("buoy_safe_water")) buf.append(t_key+keyValDelimiter+"buoy_saf");
			 else if (value.equals("mooring")) buf.append(t_key+keyValDelimiter+"mooring");
			 else if (value.equals("distance_mark")) buf.append(t_key+keyValDelimiter+"dist_m");
			 else {
				 buf.append(t_key+keyValDelimiter+""+value);
			 }
		 } // key.equals type 
		 else if (key.equals("category")){
			 String cat_key = "cat";
			 if (value.equals("dolphin")) buf.append(cat_key+keyValDelimiter+"dolp");
			 else {
				 buf.append("cat="+value);
			 }
		 }
		 else if (key.equals("shape")) {
			 String sh_key= "sh";
			 if (value.equals("spar")) buf.append(sh_key+ keyValDelimiter+"spar");
			 else if (value.equals("stake")) buf.append(sh_key+ keyValDelimiter+"stake");
			 else if (value.equals("can")) buf.append(sh_key+ keyValDelimiter+"can");
			 else if (value.equals("conical")) buf.append(sh_key+ keyValDelimiter+"conical");
			 else if (value.equals("spherical")) buf.append(sh_key+ keyValDelimiter+"spherical");
			 else buf.append(sh_key+ keyValDelimiter+""+value);
		 }
		 else if (key.equals("colour")) {
			 String c_key = "c";
			 if (value.equals("red")) {buf.append(c_key + keyValDelimiter+"red");} 
			 else if (value.equals("red;white")) {buf.append(c_key + keyValDelimiter+"rw");}
			 else if (value.equals("red;green;red")) {buf.append(c_key + keyValDelimiter+"rgr");}
			 else if (value.equals("red;white;red;white")) {buf.append(c_key + keyValDelimiter+"rwrw");}
			 else if (value.equals("green")) {buf.append(c_key + keyValDelimiter+"green");}
			 else if (value.equals("green;red;green")) {buf.append(c_key + keyValDelimiter+"grg");}
			 else if (value.equals("green;white;green;white")) {buf.append(c_key + keyValDelimiter+"gwgw");}
			 else if (value.equals("yellow")) {buf.append(c_key + keyValDelimiter+"yellow");}
			 else if (value.equals("yellow;black")) {buf.append(c_key + keyValDelimiter+"yb");}
			 else if (value.equals("black;yellow;black")) {buf.append(c_key + keyValDelimiter+"byb");}
			 else buf.append(c_key + keyValDelimiter+""+value);
		 }
		 else if (key.equals("colour_pattern")){
			 String c_pat_Key = "c_pat";
			 if (value.equals("horizontal")) buf.append(c_pat_Key + keyValDelimiter+"hor");
			 else if (value.equals("colour_pattern=vertical")) buf.append(c_pat_Key + keyValDelimiter+"ver"); 
			 else buf.append(c_pat_Key + keyValDelimiter+""+value);
		 }
		 else if (key.equals("daymark_colour") || key.equals("topmark_colour")){
			 String top_c_Key= "top_c";
			 if (value.equals("green")) buf.append(top_c_Key + keyValDelimiter+"green");
			 else if (value.equals("red")) buf.append(top_c_Key + keyValDelimiter+"red");
			 else if (value.equals("yellow")) buf.append(top_c_Key + keyValDelimiter+"yellow");
			 else if (value.equals("black")) buf.append(top_c_Key + keyValDelimiter+"black");
			 else if (value.equals("red;white;red")) buf.append(top_c_Key + keyValDelimiter+"rwrw");
			 else buf.append(top_c_Key + keyValDelimiter+""+value);
		 }
		 else if (key.equals("daymark_shape") || key.equals("topmark_shape")) {
			 String top_s_Key = "top_s";
			 if (value.equals("triangle, point up")) buf.append(top_s_Key + keyValDelimiter+"tri_p_u");
			 else if (value.equals("triangle, point down")) buf.append(top_s_Key + keyValDelimiter+"tri_p_d");
			 else if (value.equals("cone, point up")) buf.append(top_s_Key + keyValDelimiter+"cone_p_u");
			 else if (value.equals("cylinder")) buf.append(top_s_Key + keyValDelimiter+"cyl");
			 else if (value.equals("x-shape")) buf.append(top_s_Key + keyValDelimiter+"x_s");
			 else if (value.equals("2 cones down")) buf.append(top_s_Key + keyValDelimiter+"2_cones_d");
			 else if (value.equals("2 cones point together")) buf.append(top_s_Key + keyValDelimiter+"2_cones_p_tog");
			 else if (value.equals("2 cones base together")) buf.append(top_s_Key + keyValDelimiter+"2_cones_b_tog");
			 else if (value.equals("2 cones up")) buf.append(top_s_Key + keyValDelimiter+"2_cones_up");
			 else buf.append(top_s_Key + keyValDelimiter+""+value);
		 }
		 else buf.append(key+keyValDelimiter+value);
		 
		 
		 buf.append(keyValPairDelimiter);
		 } // keyValPair contains =
	 }
	 result = buf.toString();
	 if (result.length() > 3 && result.endsWith(keyValPairDelimiter)){
		 result = result.substring(0,result.length()-3);
	 }
	 } // pShapeIdentifier != null
	 return result;
 }
/**
 * calculate a short Identifier from pShapeIdentifier
 * @param pShapeIdentifier
 * @return
 */

private String calculateShortShapeIdentifierOld(String pShapeIdentifier){
	 String result = null;
	 String keyValPairDelimiter ="---";
	 StringBuffer buf = new StringBuffer();
	 if (pShapeIdentifier != null) {
	   String [] fields = pShapeIdentifier.split("!!!");
	   int count = fields.length;
	   for (int index = 0; index < count;index++){
		 String aKeyValPair = fields[index];
		 if (aKeyValPair.contains("=")) {
			 String[] temp = fields[index].split("="); 
			 String key= temp[0];
			 String value = temp[1];
		 
		 if (key.equals("type")) {
			 if (value.equals("buoy_lateral")) buf.append("t=buoy_lat");
			 else if (value.equals("buoy_cardinal")) buf.append("t=buoy_car");
			 else if (value.equals("beacon_lateral")) buf.append("t=beac_lat");
			 else if (value.equals("beacon_cardinal")) buf.append("t=beac_car");
			 else if (value.equals("buoy_special_purpose")) buf.append("t=buoy_spe_pu");
			 else if (value.equals("buoy_safe_water")) buf.append("t=buoy_safe_w");
			 else if (value.equals("mooring")) buf.append("t=mooring");
			 else if (value.equals("distance_mark")) buf.append("t=dist_m");
			 else {
				 buf.append("t="+value);
			 }
		 } // key.equals type 
		 else if (key.equals("category")){
			 if (value.equals("dolphin")) buf.append("cat=dolp");
			 else {
				 buf.append("cat="+value);
			 }
		 }
		 else if (key.equals("shape")) {
			 if (value.equals("spar")) buf.append("sh=spar");
			 else if (value.equals("stake")) buf.append("sh=stake");
			 else if (value.equals("can")) buf.append("sh=can");
			 else if (value.equals("conical")) buf.append("sh=conical");
			 else if (value.equals("spherical")) buf.append("sh=spherical");
			 else buf.append("s="+value);
		 }
		 else if (key.equals("colour")) {
			 if (value.equals("red")) {buf.append("c=red");} 
			 else if (value.equals("red;white")) {buf.append("c=rw");}
			 else if (value.equals("red;green;red")) {buf.append("c=rgr");}
			 else if (value.equals("red;white;red;white")) {buf.append("c=rwrw");}
			 else if (value.equals("green")) {buf.append("c=green");}
			 else if (value.equals("green;red;green")) {buf.append("c=grg");}
			 else if (value.equals("green;white;green;white")) {buf.append("c=gwgw");}
			 else if (value.equals("yellow")) {buf.append("c=yellow");}
			 else if (value.equals("yellow;black")) {buf.append("c=yb");}
			 else if (value.equals("black;yellow;black")) {buf.append("c=byb");}
			 else buf.append("c="+value);
		 }
		 else if (key.equals("colour_pattern")){
			 if (value.equals("horizontal")) buf.append("c_pat=hor");
			 else if (value.equals("colour_pattern=vertical")) buf.append("c_pat=ver"); 
			 else buf.append("c_pat="+value);
		 }
		 else if (key.equals("daymark_colour") || key.equals("topmark_colour")){
			 if (value.equals("green")) buf.append("top_c=green");
			 else if (value.equals("red")) buf.append("top_c=red");
			 else if (value.equals("yellow")) buf.append("top_c=yellow");
			 else if (value.equals("black")) buf.append("top_c=black");
			 else if (value.equals("red;white;red")) buf.append("top_c=rwrw");
			 else buf.append("top_c="+value);
		 }
		 else if (key.equals("daymark_shape") || key.equals("topmark_shape")) {
			 if (value.equals("triangle, point up")) buf.append("top_s=tri_p_u");
			 else if (value.equals("triangle, point down")) buf.append("top_s=tri_p_d");
			 else if (value.equals("cone, point up")) buf.append("top_s=cone_p_u");
			 else if (value.equals("cylinder")) buf.append("top_s=cyl");
			 else if (value.equals("x-shape")) buf.append("top_s=x_s");
			 else if (value.equals("2 cones down")) buf.append("top_s=2_cones_d");
			 else if (value.equals("2 cones point together")) buf.append("top_s=2_cones_p_tog");
			 else if (value.equals("2 cones base together")) buf.append("top_s=2_cones_b_tog");
			 else if (value.equals("2 cones up")) buf.append("top_s=2_cones_up");
			 else buf.append("top_s="+value);
		 }
		 else buf.append(key+"="+value);
		 
		 
		 buf.append(keyValPairDelimiter);
		 } // keyValPair contains =
	 }
	 result = buf.toString();
	 if (result.length() > 3 && result.endsWith(keyValPairDelimiter)){
		 result = result.substring(0,result.length()-3);
	 }
	 } // pShapeIdentifier != null
	 return result;
 }
     /**
      * the SeamarkImages and Buttons are stored in a dictionary to count 
      * we put them in a list and sort with the occurrences
      */
     private void sortToListAndDisplay(){
    	 ArrayList<ButtonImageViewEntry> aList = new ArrayList<ButtonImageViewEntry>();
    	 Set<Entry<String,ButtonImageViewEntry>> aEntrySet = mButtonImageViewDictionary.entrySet();
	   	   for (Iterator <Entry<String,ButtonImageViewEntry>> aIterator = aEntrySet.iterator();aIterator.hasNext();) {
	   		Entry<String,ButtonImageViewEntry> aEntry = aIterator.next();
	   		String aKey = aEntry.getKey();
	   		ButtonImageViewEntry aButtonImageView = aEntry.getValue();
	   		SeamarkDrawableTypeCounter aCounter = mSeamarkDrawableTypeCounterDictionary.get(aKey);
	   		if (aCounter != null){
	   			int count = aCounter.getCount();
	   			aButtonImageView.setOccurrences(count);
	   			aList.add(aButtonImageView);
	   		}
	   	   } // for
	   	Collections.sort(aList,new ButtonImageViewEntryComparator()); // high occurrences come first
	   	mButtonImageViewEntryList = aList;
	   	LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
         int controlCountImages = 0;
         int countImagesWithHighOccurrences = 0;
         int countNewImages = 0;
         int countExistingImages=0;
    	 mTextView.append("\n nr of  images: " + aEntrySet.size() );
    	 mSaveFrequentSymbolsBtn = new Button(this);
    	 mSaveFrequentSymbolsBtn.setText("save all frequent Symbols n > " +minOccurrency  );
    	 mSaveFrequentSymbolsBtn.setOnClickListener(onButtonSaveFrequentSymbolsClick);
    	 mSaveFrequentSymbolsBtn.setGravity(Gravity.LEFT);
    	 layout.addView(mSaveFrequentSymbolsBtn, params); 
    	 mSaveOSMTagMappingBtn = new Button(this);
    	 mSaveOSMTagMappingBtn.setText("save all file names to osm-tag-mappings " );
    	 mSaveOSMTagMappingBtn.setOnClickListener(onButtonSaveTagMappingsClick);
    	 mSaveOSMTagMappingBtn.setGravity(Gravity.LEFT);
    	 layout.addView(mSaveOSMTagMappingBtn, params); 
    	 

    	 
    	 
    	 int [] countOcs = new int [10];
	   	 for (ButtonImageViewEntry aButtonImageView : aList){
	   		Button aButton = aButtonImageView.getButton();
   			ImageView aImageView = aButtonImageView.getImageView();
   			String aIdentifier = aButton.getText().toString();
   			int count = aButtonImageView.getOccurrences();
   			if (count >0 && count < 11) {
   				countOcs[count-1]++;
   			}
   			if (count > 10 ) {
   				mTextView.append("\n" + count  +" " + aIdentifier );
   				controlCountImages++;
   				countImagesWithHighOccurrences++;
   			}
   			// if (count > 10 )mTextView.append("\n" + count  +" " + this.calculateShortShapeIdentifier(aIdentifier ));
   			if ( aButton != null){
   				aButton.append(":" +" occurences=" + count );
   				String aFilePath = mExternalPathName + mImagesDirName + "/" + aIdentifier ; 
	 			File aTestFile = new File(aFilePath+".png");
	 			if (aTestFile.exists()){
	 				aButton.append(" : exists");
	 				Log.i(TAG,"Symbol filePath:" + aFilePath +".png");  
	 				countExistingImages++;
	 			} else {
	 				countNewImages++;
	 			}
   				layout.addView(aImageView,params);
   				layout.addView(aButton, params); 
   				
   			}
	   	 } // for
	     for (int index =countOcs.length -1;index >=0;index--){
	    	 mTextView.append("\n nr of  images with " +(index+1) + " occurrences: "+ countOcs[index]); 
	    	 controlCountImages = controlCountImages + countOcs[index];
	     }
	     mTextView.append("\n found " + controlCountImages + " images");
	     mTextView.append("\n found " + countImagesWithHighOccurrences + " images with occurences > 10");
	     mTextView.append("\n found " + countNewImages + " new images");
	     mTextView.append("\n found " + countExistingImages + " existing images");
     }
     /**
      * not used 
      * @param layout
      */
	 
	 private void showAllSymbols(LinearLayout layout) {
		 
		 Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	 	    paint.setStyle(Paint.Style.STROKE);
	 	    paint.setColor(Color.BLACK);
	 	    paint.setStrokeWidth(5);
		 String[] names = SeamarkSymbol.getKeys();
	        TextView aTextView = (TextView) findViewById(R.id.svg_textview);
	        aTextView.setText("Time to preload symbols: " + mUsedTimeToPreload + " ms");
         int l;
          if (names.length < 400){
         	 l=names.length;
          }else {
         	 l= 400;
          }
	       for (int i = 0; i <l ;i++){
	       // for (int i = 0; i <250 ;i++){
	            ImageView imageView = new ImageView(this);
	            
	            SeamarkSymbol seamarkSymbol = SeamarkSymbol.getSeamarkSymbol(names[i]);
	            Log.d(TAG,"display symbol " + names[i]);
		        //PathShape pathShape = seamarkSymbol.getPathShape();
	            float width = seamarkSymbol.getWidth();
	 	    	float height = seamarkSymbol.getHeight();
	            int bitmapWidth = (int)width /2;
		 	    int bitmapHeight = (int)(height)/2;
		 	    if (bitmapWidth < 20 || bitmapHeight < 20) {
		 	    	Log.d(TAG," Very small Bitmap " + names[i]);
		 	    	bitmapHeight = 20;
		 	    	bitmapWidth = 200;
		 	    	
		 	    }
		 	    Bitmap seamarkBitmap = Bitmap.createBitmap(bitmapWidth,bitmapHeight,Bitmap.Config.ARGB_8888);
		 	    Canvas canvas = new Canvas(seamarkBitmap);
		 	    String seamarkSymbolName = seamarkSymbol.getName();
		 	    if (seamarkSymbolName.equals("")) {
		 	    	// no path for the symbol
		 	    	paint.setStrokeWidth(1);
		 	    	canvas.drawText(" no drawing available ", 50, 100, paint);
		 	    } else {
		 	    	paint.setStrokeWidth(2);
			 	    Path path = seamarkSymbol.getPath();
			 	    PathShape pathShape = new PathShape(path,width,height);
			        pathShape.resize(width/2,height/2);
			        pathShape.draw(canvas, paint);
		 	    }
	            imageView.setImageBitmap(seamarkBitmap);
	            
	            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
	                    LinearLayout.LayoutParams.FILL_PARENT,
	                    LinearLayout.LayoutParams.WRAP_CONTENT
	            );
	            layout.addView(imageView, p);

	            Button buttonView = new Button(this);
	            buttonView.setText("nr: " + Integer.toString(i) + " SymbolName: " + names[i] + "          width: " + seamarkSymbol.getWidth()+ " height: " + seamarkSymbol.getHeight() );
	            layout.addView(buttonView, p);
	            
	        }
	 }
	 
	 /**
		 * Uses the UI thread to display the given text message as toast notification.
		 * 
		 * @param text
		 *            the text message to display
		 */
		void showToastOnUiThread(final String text) {

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast toast = Toast.makeText( SeamarkImagesFromSvgActivity.this, text, Toast.LENGTH_LONG);
						toast.show();
					}
				});

		}
		
		
		
		private Runnable readPois = new Runnable() {
			public void run() {
				if (mSeamarkOSM.getSeamarkFileReadComplete()){
					//mSeamarksDictionary = mSeamarkOsm.getSeamarksAsDictionary();
					mSeamarksList = mSeamarkOSM.getSeamarksAsArrayList();
					mCanWork = true;
					Log.d(TAG,"starting showAllSeamarkImages()");
					mTextView.append("display semarkImages from " + mSeamarkFilePath);
					showAllSeamarkImages();
				} else {
					// try again
					Log.d(TAG,"new try working on file " + mSeamarkFilePath);
					mReadSeamarksHandler.postDelayed(this,1000);
				}
			}
		}; 
		
		/**
		 * Sets all file filters and starts the FilePicker to select a map file.
		 */
		private void startSeamarksFilePicker() {
			FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_DAT);
			FilePicker.setFileSelectFilter(new ValidSeamarkFile());
			startActivityForResult(new Intent(this, FilePicker.class), SELECT_SEAMARKS_FILE);
		}
		
		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		    if (requestCode == SELECT_SEAMARKS_FILE && resultCode == RESULT_OK && intent != null
					&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
				
					String aFilename = intent.getStringExtra(FilePicker.SELECTED_FILE);
					File selectedFile = new File(aFilename);
					Log.d(TAG,"working on file " + aFilename);
					this.setSeamarkFilePathAndRead(selectedFile.getAbsolutePath());
					mReadSeamarksHandler.postDelayed(readPois, 1000);  // it takes some time to read the seamarks file
				 	
				 
			} 
		}
		
		 private void countKeys(String pShapeIdentifier){
			  if (mSeamarkDrawableTypeCounterDictionary.containsKey(pShapeIdentifier)) {
				  SeamarkDrawableTypeCounter aSeamarkDrawableTypeCounter= mSeamarkDrawableTypeCounterDictionary.get(pShapeIdentifier) ;
				  aSeamarkDrawableTypeCounter.incrementOne();
			  }else {
				  SeamarkDrawableTypeCounter pShapeIdentifierCounter = new SeamarkDrawableTypeCounter(pShapeIdentifier);
				  mSeamarkDrawableTypeCounterDictionary.put(pShapeIdentifier, pShapeIdentifierCounter);
			  }
		  }
		 
		 private class ButtonImageViewEntryComparator implements Comparator<ButtonImageViewEntry>{
			 @Override
			 public int compare (ButtonImageViewEntry entry1, ButtonImageViewEntry entry2){
				 return entry2.getOccurrences()-entry1.getOccurrences();
			 }
		 }
		 
		 private class ButtonImageViewEntry {
			 private Button mButton;
			 private ImageView mImageView;
			 private Bitmap mBitmap;
			 private int mOccurences =0;
			 private String mIdentifier;
			 
			 private ButtonImageViewEntry (String pIdentifier,ImageView pImageView, Button pButton, Bitmap pBitmap){
				 mImageView =pImageView;
				 mButton = pButton;
				 mBitmap = pBitmap;
				 mIdentifier = pIdentifier;
			 }
			 
			 private Button getButton(){
				 return mButton;
			 }
			 
			 private ImageView getImageView(){
				 return mImageView;
			 }
			 
			 private Bitmap getBitmap(){
				 return mBitmap;
			 }
			 
			 private String getIdentifier() {
				 return mIdentifier;
			 }
			 private void setOccurrences(int pOccurences){
				 mOccurences =pOccurences;
			 }
			 
			 private int getOccurrences(){
				 return mOccurences;
			 }
		 }

    /**
     * @author vkADM
     * count the occurrences of the SeamarkDrawableTypeKey
     */
    private class SeamarkDrawableTypeCounter {
        private int mCount;
        private String mSeamarkDrawableTypeKey;


        public SeamarkDrawableTypeCounter(String pSeamarkDrawableTypeKey) {
            mSeamarkDrawableTypeKey = pSeamarkDrawableTypeKey;

            mCount = 1;
        }

        public void incrementOne(){
            mCount++;
        }

        public int getCount() {
            return mCount;
        }
        public String getKey () {
            return mSeamarkDrawableTypeKey;
        }


    }

}
	 
