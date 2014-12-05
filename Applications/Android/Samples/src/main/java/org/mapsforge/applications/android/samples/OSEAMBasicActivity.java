package org.mapsforge.applications.android.samples;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.mapsforge.applications.android.samples.common.FilterByFileExtension;
import org.mapsforge.applications.android.samples.filefilter.ValidMapFile;
import org.mapsforge.applications.android.samples.filefilter.ValidRenderTheme;
import org.mapsforge.applications.android.samples.filepicker.FilePicker;
import org.mapsforge.applications.android.samples.seamark.SeamarkOSM;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.graphics.AndroidSvgBitmapStore;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * Created by vkmapsforge05 on 04.11.14.
 */
public class OSEAMBasicActivity extends SamplesBaseActivity {
    private static final String TAG = "OSEAMBasicActivity";
    private static final FileFilter FILE_FILTER_EXTENSION_MAP = new FilterByFileExtension(".map");
    private static final FileFilter FILE_FILTER_EXTENSION_XML = new FilterByFileExtension(".xml");
    private static final int SELECT_MAP_FILE = 0;
    private static final int SELECT_RENDER_THEME_FILE = 1;

    private TileRendererLayer mTileRendererLayer = null;

    private String HOME_DIR= Environment.getExternalStorageDirectory().getAbsolutePath();
    private String DEFAULT_MAP_PATH = HOME_DIR + "/germany.map";
    private String mMapfilePath = DEFAULT_MAP_PATH;
    private String mRenderThemeFileName ="";
    protected XmlRenderTheme mXMLRenderTheme = null;
    protected File mMapFile = null;


    private static final String currentMapNamePathKey = "current_mapfilename";
    private static final String currentRenderThemeNamePathKey = "current_rendertheme_filename";



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_oseambasic_options_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_preferences:
                intent = new Intent(this, Settings.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                if (renderThemeStyleMenu != null) {
                    intent.putExtra(Settings.RENDERTHEME_MENU, renderThemeStyleMenu);
                }
                startActivity(intent);
                return true;
            case R.id.menu_position_enter_coordinates:
                showDialog(DIALOG_ENTER_COORDINATES);
                break;
            case R.id.menu_svgclear:
                AndroidSvgBitmapStore.clear();
                break;
            case R.id.menu_select_mapfile:
                startMapFilePicker();
                break;
            case R.id.menu_select_rendertheme:
                startRenderThemeFilePicker();
                break;
            case R.id.menu_show_parameters:
                //showCurrentParameters();
                showParameterDialog();
                break;
        }
        return false;
    }


    private void showParameterDialog() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.oseam_basic_activity_current_parameters_alert_dialog, null);
        final TextView aMsgEdit = (TextView)textEntryView.findViewById(R.id.current_parameters_dialog_msg_edit);
        String nl = "\n";
        if (mMapfilePath != null )aMsgEdit.append("Mapfile: " + mMapfilePath + nl);
        if (mRenderThemeFileName != null ) aMsgEdit.append("Rendertheme: " +mRenderThemeFileName+nl );
        byte aZoom = mapView.getModel().mapViewPosition.getZoomLevel();
        aMsgEdit.append("Zoom: " + Integer.toString(aZoom));
        final AlertDialog aDialog =  new AlertDialog.Builder(this)
                .setIcon(R.drawable.alert_dialog_icon)
                .setTitle(R.string.current_parameters_dialog_current_map_label)
                .setView(textEntryView)
                .setPositiveButton(R.string.current_parameters_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                })
                /*.setNeutralButton(R.string.fixed_depth_soundings_alert_dialog_cancel_btn, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                })
                */
                .create();
        aDialog.show();
    }



    private void showCurrentParameters() {
        String nl = "\n";
        StringBuilder buf = new StringBuilder();
        buf.append("Mapfile: ");
        if (mMapfilePath != null ) buf.append(mMapfilePath);
        buf.append(nl);
        buf.append("Rendertheme: ");
        if (mRenderThemeFileName != null ) buf.append(mRenderThemeFileName);
        buf.append(nl);
        buf.append("Zoom: ");
        byte aZoom = mapView.getModel().mapViewPosition.getZoomLevel();
        buf.append(Integer.toString(aZoom));
        String msg = buf.toString();
        showToastOnUiThread(msg);

    }

    /**
     * we add a new neutral button to the dialog
     * @param id
     * @return
     */
    @Deprecated
    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater factory = LayoutInflater.from(this);
        switch (id) {
            case DIALOG_ENTER_COORDINATES:
                builder.setIcon(android.R.drawable.ic_menu_mylocation);
                builder.setTitle(R.string.dialog_location_title);
                final View view = factory.inflate(R.layout.dialog_enter_coordinates, null);
                builder.setView(view);
                builder.setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        double lat = Double.parseDouble(((EditText) view.findViewById(R.id.latitude)).getText()
                                .toString());
                        double lon = Double.parseDouble(((EditText) view.findViewById(R.id.longitude)).getText()
                                .toString());
                        byte zoomLevel = (byte) ((((SeekBar) view.findViewById(R.id.zoomlevel)).getProgress()) +
                                OSEAMBasicActivity.this.mapView.getModel().mapViewPosition.getZoomLevelMin());

                        OSEAMBasicActivity.this.mapView.getModel().mapViewPosition.setMapPosition(
                                new MapPosition(new LatLong(lat, lon), zoomLevel));
                    }
                });
                builder.setNegativeButton(R.string.cancelbutton, null);
                // when the mapfile changes, the position of the map may be outside the viewport, so we set it to the provided map center position
                builder.setNeutralButton(R.string.enter_ccordinates_dialog_center_screen_to_map_center, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        OSEAMBasicActivity.this.mapView.getModel().mapViewPosition.setMapPosition(
                                getInitialPosition());
                    }
                });
                return builder.create();
        }
        return null;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == SELECT_MAP_FILE) {

            if (resultCode == RESULT_OK) {

                if (intent != null && intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
                    File selectedFile = new File(intent.getStringExtra(FilePicker.SELECTED_FILE));
                    mMapfilePath = selectedFile.getAbsolutePath();
                    Log.d(SamplesApplication.TAG, "Mapfile: " + mMapfilePath);
                    this.preferencesFacade.putString(currentMapNamePathKey, mMapfilePath);
                    this.preferencesFacade.save();
                    AndroidUtil.restartActivity(this);
                }
            } else {
                if (resultCode == RESULT_CANCELED && mTileRendererLayer == null) {
                    finish();
                }
            }
        }
        if (requestCode == SELECT_RENDER_THEME_FILE){
            if (resultCode == RESULT_OK) {
                if (intent != null && intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
                    File selectedFile = new File(intent.getStringExtra(FilePicker.SELECTED_FILE));
                    mRenderThemeFileName = selectedFile.getAbsolutePath();
                    Log.d(SamplesApplication.TAG, "RenderTheme: " + mRenderThemeFileName);
                    this.preferencesFacade.putString(currentRenderThemeNamePathKey,mRenderThemeFileName);
                    this.preferencesFacade.save();
                    AndroidUtil.restartActivity(this);
                }
            }
        }
    }



    /**
     * Sets all file filters and starts the FilePicker to select a map file.
     */
    private void startMapFilePicker() {
        FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_MAP);
        FilePicker.setFileSelectFilter(new ValidMapFile());
        startActivityForResult(new Intent(this, FilePicker.class), SELECT_MAP_FILE);
    }

    @Override
    protected void onResume() {
        super.onResume();


    }
    @Override
    protected void onPause() {
        Log.d(SamplesApplication.TAG, "executing onPause");
        Log.d(SamplesApplication.TAG, "last current map " + mMapfilePath);
        Log.d(SamplesApplication.TAG, "last current rendertheme " + mRenderThemeFileName);
        this.preferencesFacade.putString(currentMapNamePathKey, mMapfilePath);
        this.preferencesFacade.putString(currentRenderThemeNamePathKey,mRenderThemeFileName);
        this.preferencesFacade.save();
        super.onPause();
    }

    @Override
    protected MapPosition getDefaultInitialPosition() {
        return new MapPosition(new LatLong(52.9, 5.5), (byte) 12);
    }


    @Override
    protected File getMapFile() {
        File mapfile = new File (mMapfilePath);
        return mapfile;
    }

    protected File getMapFileDirectory() {
        String aDirPath = getMapFile().getParent();
        if (aDirPath != null)  {
            File aDirectory = null;
            aDirectory = new File (aDirPath);
            return aDirectory;
        } else {
          return super.getMapFileDirectory();
        }

    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_basicbuoyviewer;
    }

    /**
     * Sets all file filters and starts the FilePicker to select a map file.
     */
    private void startRenderThemeFilePicker() {
        FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_XML);
        FilePicker.setFileSelectFilter(new ValidRenderTheme());
        startActivityForResult(new Intent(this, FilePicker.class), SELECT_RENDER_THEME_FILE);
    }

    /*
    @Override

    protected XmlRenderTheme getRenderTheme() {
        return InternalRenderTheme.OSMARENDER;
    }
   */

    @Override
    protected XmlRenderTheme getRenderTheme() {
        XmlRenderTheme aRenderTheme = null;
        String renderThemePath = this.preferencesFacade.getString(currentRenderThemeNamePathKey,"");

        Log.d(SamplesApplication.TAG,"getRenderTheme -> RenderThemePath: " +  renderThemePath);
        if (renderThemePath.equals("")){
            aRenderTheme = InternalRenderTheme.OSMARENDER;
        } else {
            File renderThemeFile = new File (renderThemePath);
            try {
                aRenderTheme = new ExternalRenderTheme(renderThemeFile);
                Log.d(SamplesApplication.TAG,"valid render theme found: " +  renderThemePath);
                mRenderThemeFileName = renderThemePath;
            } catch ( IOException e ){
                Log.d(SamplesApplication.TAG," no valid render theme ");
                aRenderTheme = InternalRenderTheme.OSMARENDER;
            }
        }
        String aRelativePathPrefix = aRenderTheme.getRelativePathPrefix();
        return aRenderTheme;
    }

    @Override
    protected void createLayers() {
        String aMapFilePath = this.preferencesFacade.getString(currentMapNamePathKey,DEFAULT_MAP_PATH);
        mMapfilePath = aMapFilePath;
        // sometimes mMapFileName starts with /mnt  or /sdcard, we test this in getMapFile();
        File mapfile = getMapFile();

        if (mapfile.exists()) {
            mMapFile = mapfile;
            String fileNamePath = mapfile.getAbsolutePath();
            Log.d(SamplesApplication.TAG, " createLayers -> current map " + fileNamePath);

            mXMLRenderTheme = getRenderTheme();
            TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(this.tileCaches.get(0),
                    mapView.getModel().mapViewPosition, mapfile, mXMLRenderTheme, false, true);
            this.mapView.getLayerManager().getLayers().add(tileRendererLayer);

            // needed only for samples to hook into Settings.
            setMaxTextWidthFactor();
            String aShowTileGridKey = getResources().getString(R.string.preferences_key_show_tile_grid);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean mustShowTileGrid = prefs.getBoolean(aShowTileGridKey,false);
            if (mustShowTileGrid) {
                mapView.getLayerManager().getLayers()
                        .add(new TileGridLayer(AndroidGraphicFactory.INSTANCE, this.mapView.getModel().displayModel));
                mapView.getLayerManager().getLayers()
                        .add(new TileCoordinatesLayer(AndroidGraphicFactory.INSTANCE, this.mapView.getModel().displayModel));
            }

        }else {
            // maybe the old map file was deleted or moved
            startMapFilePicker();
        }
    }

    /**
     * Uses the UI thread to display the given text message as toast notification.
     *
     * @param text
     *            the text message to display
     */
    public void showToastOnUiThread(final String text) {

        if (AndroidUtil.currentThreadIsUiThread()) {
            Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
            toast.show();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(OSEAMBasicActivity.this, text, Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
    }



}
