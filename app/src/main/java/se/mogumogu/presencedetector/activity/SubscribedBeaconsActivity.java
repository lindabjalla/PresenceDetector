package se.mogumogu.presencedetector.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;

import java.util.Set;

import se.mogumogu.presencedetector.PresenceDetectorApplication;
import se.mogumogu.presencedetector.R;
import se.mogumogu.presencedetector.RangeHandler;
import se.mogumogu.presencedetector.fragment.BeaconAliasNameDialogFragment;
import se.mogumogu.presencedetector.model.SubscribedBeacon;

public final class SubscribedBeaconsActivity extends ToolbarProvider implements BeaconConsumer, BeaconAliasNameDialogFragment.BeaconAliasNameDialogListener {

    private static final String TAG = SubscribedBeaconsActivity.class.getSimpleName();
    public static boolean active;

    private Context context;
    private BeaconManager beaconManager;
    private Region allBeaconsRegion;
    private String subscribedBeaconsJson;
    private Set<SubscribedBeacon> subscribedBeacons;
    private Gson gson;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_subscribed_beacons);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.subscribed_beacons_toolbar);
        setToolbar(toolbar, false);

        context = this;
        allBeaconsRegion = new Region("allBeacons", null, null, null);
        preferences = getSharedPreferences(PresenceDetectorApplication.PRESENCE_DETECTOR_PREFERENCES, Context.MODE_PRIVATE);

        final RangeHandler rangeHandler = new RangeHandler(this, getSupportFragmentManager());
        initializeBeaconManager(rangeHandler);

        gson = new Gson();

        subscribedBeaconsJson = preferences.getString(PresenceDetectorApplication.SUBSCRIBED_BEACONS, null);
        subscribedBeacons = PresenceDetectorApplication.initializeSubscribedBeacons(subscribedBeaconsJson);
    }

    @Override
    public void onBeaconServiceConnect() {

        try {
            beaconManager.startRangingBeaconsInRegion(allBeaconsRegion);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {

        super.onStart();
        active = true;
    }

    @Override
    protected void onPause() {

        Log.d(TAG, "onPause");
        super.onPause();
        beaconManager.unbind(this);
        Log.d("beaconManager", beaconManager.getRangingNotifier().toString());
    }

    @Override
    protected void onStop() {

        Log.d(TAG, "onStop");
        super.onStop();
        active = false;
    }

    @Override
    public void onDialogPositiveClick(final DialogFragment dialog, final View view) {

        final EditText editText = (EditText) view.findViewById(R.id.alias_name_edit_text);
        final String aliasName = editText.getText().toString();
        final Bundle bundle = dialog.getArguments();
        final SubscribedBeacon subscribedBeacon = bundle.getParcelable(PresenceDetectorApplication.SUBSCRIBED_BEACON);

        if (subscribedBeacon != null) {

            replaceAliasName(subscribedBeacon, aliasName);

        } else {

            Log.d(TAG, "subscribed beacon is null");
        }

        Toast.makeText(context, "The changes were successfully saved.", Toast.LENGTH_LONG).show();
        dialog.dismiss();
    }

    @Override
    public void onDialogNegativeClick(final DialogFragment dialog) {

        dialog.dismiss();
    }

    private void initializeBeaconManager(final RangeHandler rangeHandler) {

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.setRangeNotifier(rangeHandler);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);
    }

    private void replaceAliasName(final SubscribedBeacon subscribedBeacon, final String aliasName) {

        subscribedBeacons.remove(subscribedBeacon);
        subscribedBeacons.add(subscribedBeacon.setAliasName(aliasName));
        subscribedBeaconsJson = gson.toJson(subscribedBeacons);
        preferences.edit().putString(PresenceDetectorApplication.SUBSCRIBED_BEACONS, subscribedBeaconsJson).apply();
    }
}
