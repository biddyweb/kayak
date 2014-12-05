package org.mapsforge.applications.android.samples;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
    private static final String TAG = "OSEAMBasicActivity";
    private static final boolean test = true;
    private KayakTapLayer mKayakTapLayer = null;
    private KayakLayer mKayakLayer = null;
    private Button mAddLocationButton = null;


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
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteCurrentWhitewaterTable() {
        if (mKayakLayer != null){
                mKayakLayer.deleteCurrentWhitewaterTable();
        }
    }

    @Override
    protected void createLayers() {
        super.createLayers();
        Log.i(TAG, "Create Layers in KayakBasicActivity");
        //KayakTapLayer aKayakTapLayer = new KayakTapLayer(this, AndroidGraphicFactory.INSTANCE, this.mapView, mMapFile, mXMLRenderTheme,null);
        //this.mapView.getLayerManager().getLayers().add(aKayakTapLayer);
        //mKayakTapLayer = aKayakTapLayer;
        //aKayakTapLayer.updateWhitewaterFile();

        KayakLayer aKayakLayer = new KayakLayer(this);
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

    private void fixedWhitewaterDialog(KayakInfoItem pKayakInfoItem) {
        final KayakInfoItem aKayakInfoItem = pKayakInfoItem;
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
                        putNewFixedWhitewaterPositionIntoDatabase(aKayakInfoItem);
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
        fixedWhitewaterDialog(aKayakInfoItem);
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
