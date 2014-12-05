package org.mapsforge.applications.android.samples;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.mapsforge.applications.android.samples.common.PositionTools;
import org.mapsforge.applications.android.samples.whitewater.KayakInfoItem;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import java.io.File;

/**
 * Created by vkmapsforge05 on 04.12.2014.
 */
public class KayakBasicActivity extends OSEAMBasicActivity  {
    private static final String TAG = "KayakBasicActivity";
    private static final boolean test = true;
    private KayakTapLayer mKayakTapLayer = null;
    private KayakLayer mKayakLayer = null;
    private Button mAddLocationButton = null;
    private KayakJosm mKayakJosm = null;


    /**
     * Android Activity life cycle method.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mKayakJosm = new KayakJosm(this);
    }

    protected void onDestroy() {
        if (mKayakJosm != null){
            mKayakJosm.destroy();
        }
        super.onDestroy();

    }
    @Override
    protected int getLayoutId() {
        return R.layout.activity_kayak_basic;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_kayak_basic_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_clear_ww_table:
                deleteCurrentWhitewaterTable();
                break;
            case R.id.menu_save_ww_table:
                saveCurrentWhitewaterTableToExternal();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteCurrentWhitewaterTable() {
        if (mKayakLayer != null){
                mKayakLayer.deleteCurrentWhitewaterTable();
        }
    }

    private void saveCurrentWhitewaterTableToExternal(){
        String aTableName = mKayakLayer.getCurrentInfoTableName();
        String aRenderThemeFileName = mXMLRenderTheme.getRelativePathPrefix();
        File aFile = new File(aRenderThemeFileName);
        String aParentName = aFile.getParent();
        File aDir = new File(aParentName);
        aParentName = aDir.getParent();
        mKayakJosm.saveFixedKayakItems_Menu(aTableName,aParentName);

    }

    @Override
    protected void createLayers() {
        super.createLayers();
        Log.i(TAG, "Create Layers in KayakBasicActivity");
        //KayakTapLayer aKayakTapLayer = new KayakTapLayer(this, AndroidGraphicFactory.INSTANCE, this.mapView, mMapFile, mXMLRenderTheme,null);
        //this.mapView.getLayerManager().getLayers().add(aKayakTapLayer);
        //mKayakTapLayer = aKayakTapLayer;
        //aKayakTapLayer.updateWhitewaterFile();

        KayakLayer aKayakLayer = new KayakLayer(this,AndroidGraphicFactory.INSTANCE, this.mapView);
        this.mapView.getLayerManager().getLayers().add(aKayakLayer);
        mKayakLayer = aKayakLayer;

        mAddLocationButton = (Button) findViewById(R.id.put_location);
        mAddLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG,"Button clicked ");
                showToastOnUiThread("Button Clicked");
                createNewFixedWhitewaterPosition();
            }
        });


    }

    public void fixedWhitewaterDialog(KayakInfoItem pKayakInfoItem, boolean pUpdate) {
        final KayakInfoItem aKayakInfoItem = pKayakInfoItem;
        final boolean aUpdateFlag = pUpdate;
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.edit_fixed_whitewater_alert_dialog, null);
        final TextView aTypeField = (TextView) textEntryView.findViewById(R.id.fixed_whitewater_type_view_edit);
        final TextView aNameField = (TextView) textEntryView.findViewById((R.id.fixed_whitewater_name_view_edit));
        final TextView aDescriptionField = (TextView) textEntryView.findViewById((R.id.fixed_whitewater_description_view_edit));
        String aTypeStr = aKayakInfoItem.getType();
        aTypeField.setText(aTypeStr);
        String aNameStr = aKayakInfoItem.getName();
        aNameField.setText(aNameStr);
        String aDescriptionStr = aKayakInfoItem.getDescription();
        aDescriptionField.setText(aDescriptionStr);

        final TextView aNumberField = (TextView)textEntryView.findViewById(R.id.fixed_whitewater_number_view);
        String numberStr = Integer.toString(pKayakInfoItem.getNumber());
        aNumberField.setText(numberStr);
        final TextView aLatEditField = (TextView)textEntryView.findViewById(R.id.fixed_whitewater_lat_view_edit);
        String aLatStr  = PositionTools.getLATString(pKayakInfoItem.getLAT());
        aLatEditField.setText(aLatStr);
        final TextView aLonEditField = (TextView)textEntryView.findViewById(R.id.fixed_whitewater_lon_view_edit);
        String aLonStr  = PositionTools.getLONString( pKayakInfoItem.getLON());
        aLonEditField.setText(aLonStr);
        final Button aUpdateButton = (Button) textEntryView.findViewById(R.id.fixed_whitewater_update_btn);
        final Button aPut_InButton = (Button) textEntryView.findViewById(R.id.fixed_whitewater_put_in_btn);
        aPut_InButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aTypeField.setText("put_in");
            }
        });
        final Button aEgressButton = (Button) textEntryView.findViewById(R.id.fixed_whitewater_egress_btn);
        aEgressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aTypeField.setText("egress");
            }
        });
        final Button aIn_OutButton = (Button) textEntryView.findViewById(R.id.fixed_whitewater_in_out_btn);
        aIn_OutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aTypeField.setText("put_in;egress");
            }
        });
        final Button aRapidButton = (Button) textEntryView.findViewById(R.id.fixed_whitewater_rapid_btn);
        aRapidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aTypeField.setText("rapid");
            }
        });
        final Button aHazardButton = (Button) textEntryView.findViewById(R.id.fixed_whitewater_hazard_btn);
        aHazardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aTypeField.setText("hazard");
            }
        });

        final AlertDialog aDialog =  new AlertDialog.Builder(this)
                .setIcon(R.drawable.alert_dialog_icon)
                .setTitle(R.string.fixed_whitewater_alert_dialog_title)
                .setView(textEntryView)
                .setPositiveButton(R.string.fixed_whitewater_alert_dialog_ok_btn, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String aTypeStr = aTypeField.getText().toString();
                        aKayakInfoItem.setType(aTypeStr);
                        String aNameStr = aNameField.getText().toString();
                        aKayakInfoItem.setName(aNameStr);
                        String aDescriptionStr = aDescriptionField.getText().toString();
                        aKayakInfoItem.setDescription(aDescriptionStr);
                        if (aUpdateFlag) {

                        }else {
                            putNewFixedWhitewaterPositionIntoDatabase(aKayakInfoItem);
                        }
                    }
                })
                .setNeutralButton(R.string.fixed_whitewater_alert_dialog_cancel_btn, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

	                    /* User clicked cancel so do some stuff */
                    }
                })
                .setNegativeButton(R.string.fixed_whitewater_alert_dialog_delete_btn, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

	                    /* User clicked delete  */
                        //checkFixedWhitewaterDeleteItem(item);
                    }
                })
                .create();
        aDialog.show();
    }

    private void createNewFixedWhitewaterPosition() {
        // LatLong pPoint, int pNumber,String pTypeStr,String pName ,  String pDescription , long pUTC)
        LatLong aLatLong = mapView.getModel().mapViewPosition.getMapPosition().latLong;
        int aNumber = mKayakLayer.getNextEntryNumberFromCurrentTable();
        String aType = "";
        String aName = "";
        String aDescription = "";
        long aUTC = System.currentTimeMillis();

        KayakInfoItem aKayakInfoItem = new KayakInfoItem(aLatLong,aNumber,aType,aName,aDescription,aUTC);
        boolean update = false;
        fixedWhitewaterDialog(aKayakInfoItem, update);
    }

    private void updateWhitewaterPositionIntoDatabase(KayakInfoItem pKayakInfoItem){
        if (test)  {
            Log.i(TAG,"put Item into Db: " + pKayakInfoItem.getType() + ", " + pKayakInfoItem.getName() +", " + pKayakInfoItem.getDescription());
        }
        if (mKayakLayer!= null){
            mKayakLayer.updateWhitewaterPositionIntoDatabase(pKayakInfoItem);
            mKayakLayer.requestRedraw();
        }
    }

    private void putNewFixedWhitewaterPositionIntoDatabase(KayakInfoItem pKayakInfoItem){
        if (test)  {
            Log.i(TAG,"put Item into Db: " + pKayakInfoItem.getType() + ", " + pKayakInfoItem.getName() +", " + pKayakInfoItem.getDescription());
        }
        if (mKayakLayer!= null){
            mKayakLayer.putNewFixedWhitewaterPositionIntoDatabase(pKayakInfoItem);
            mKayakLayer.requestRedraw();
        }
    }

    private void createFixWhitewaterDatabaseIfNecessary() {

    }


}
