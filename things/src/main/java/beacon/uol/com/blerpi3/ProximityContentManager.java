package beacon.uol.com.blerpi3;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.estimote.internal_plugins_api.cloud.CloudCredentials;
import com.estimote.internal_plugins_api.scanning.BluetoothScanner;
import com.estimote.internal_plugins_api.scanning.EstimoteConnectivity;
import com.estimote.internal_plugins_api.scanning.EstimoteNearable;
import com.estimote.internal_plugins_api.scanning.EstimoteTelemetryFull;
import com.estimote.proximity_sdk.proximity.EstimoteCloudCredentials;
import com.estimote.proximity_sdk.proximity.ProximityAttachment;
import com.estimote.proximity_sdk.proximity.ProximityObserver;
import com.estimote.proximity_sdk.proximity.ProximityObserverBuilder;
import com.estimote.proximity_sdk.proximity.ProximityZone;
import com.estimote.scanning_plugin.api.EstimoteBluetoothScannerFactory;

import com.estimote.scanning_plugin.packet_provider.EstimoteConnectivityPacket;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


import kotlin.Unit;
import kotlin.jvm.functions.Function1;

class ProximityContentManager
{
    private Context context;
    private CloudCredentials cloudCredentials;
    private ProximityObserver.Handler proximityObserverHandler;
    private SingBroadcastReceiver mReceiver;
    List<String> ble =new ArrayList<String>();

    public ProximityContentManager(Context context, CloudCredentials cloudCredentials) {
        this.context = context;
        this.cloudCredentials = cloudCredentials;
    }

    public void start() {

        final BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter.isDiscovering())
        {
            mBtAdapter.cancelDiscovery();
        }


        //Create Observer
        ProximityObserver proximityObserver =
                new ProximityObserverBuilder(context, (EstimoteCloudCredentials) cloudCredentials)
                .withOnErrorAction(new Function1<Throwable, Unit>()
                {
                    @Override
                    public Unit invoke(Throwable throwable) {
                        Log.e("app", "proximity observer error: " + throwable);
                        return null;
                    }
                })
                .withBalancedPowerMode()
                .build();

        //Define Zones
        ProximityZone venueZone =
                proximityObserver.zoneBuilder()
                        .forAttachmentKeyAndValue("Major|Minor", "14752|4870")
                        .inNearRange()
                        .withOnEnterAction(new Function1<ProximityAttachment, Unit>() {
                            @Override public Unit invoke(ProximityAttachment proximityAttachment) {

                                /* Do something here */
                                BluetoothScanner scanner =
                                        new EstimoteBluetoothScannerFactory(context).getSimpleScanner();
                                scanner.estimoteTelemetryFullScan()
                                        .withBalancedPowerMode()
                                        .withOnPacketFoundAction(new Function1<EstimoteTelemetryFull, Unit>()
                                        {
                                            @Override
                                            public Unit invoke(EstimoteTelemetryFull estimoteTelemetryFull) {
                                                /* Do something with the received telemetry packet here */
                                                FirebaseDatabase db = FirebaseDatabase.getInstance();
                                                DatabaseReference ref = db.getReference("Temp"); // Key
                                                ref.setValue(estimoteTelemetryFull.getTemperatureInCelsiusDegrees());
                                                return null;
                                            }
                                        })
                                        .start();


                                //Discovering Bluetooth enabled devices
                                mBtAdapter.startDiscovery();
                                mReceiver = new SingBroadcastReceiver();
                                IntentFilter ifilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                                context.registerReceiver(mReceiver, ifilter);


                              return null;
                            }
                        })
                        .withOnExitAction(new Function1<ProximityAttachment, Unit>() {
                            @Override
                            public Unit invoke(ProximityAttachment proximityAttachment) {
                                /* Do something here */
                                return null;
                            }
                        })
                        .withOnChangeAction(new Function1<List<? extends ProximityAttachment>, Unit>() {
                            @Override
                            public Unit invoke(List<? extends ProximityAttachment> proximityAttachments)
                            {
                                /* Do something here */

                                return null;
                            }
                        })
                        .create();

        proximityObserver.addProximityZone(venueZone);
        proximityObserverHandler = proximityObserver.start();
    }

    private class SingBroadcastReceiver extends BroadcastReceiver
    {

        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction(); //may need to chain this to a recognizing function
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a Toast
                String derp = device.getAddress();
                ble.add(derp);
                FirebaseDatabase db = FirebaseDatabase.getInstance();
                DatabaseReference ref = db.getReference("Beacon_Reading"); // Key
                ref.setValue(derp);
                Log.d("Device: ", String.valueOf(ble.size())+ "Device"+ derp);
            }

        }
    }

    public void stop() {
        proximityObserverHandler.stop();
    }
}
