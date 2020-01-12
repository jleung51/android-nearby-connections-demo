package com.jleung.nearbyconnectionsdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.google.android.gms.nearby.connection.Strategy.P2P_CLUSTER;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    private static final String DEVICE_ID = UUID.randomUUID().toString();

    private static final String SERVICE_ID =  "com.jleung.nearbyconnectionsdemo";

    private static Strategy STRATEGY = P2P_CLUSTER;

    private static Map<String, String> connectedDevices = new HashMap<>();

    @SuppressWarnings("unused")
    public static String getDeviceId() {
        return DEVICE_ID;
    }

    private Activity thisActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION);

        startAdvertising(this, new ReceivePayloadListener());
        startDiscovering(this, new ReceivePayloadListener());

        startService(new Intent(this, MeshNetworkService.class));
    }

    // For permission, use Manifest.permission
    private static void requestPermissions(
            Activity thisActivity,
            @SuppressWarnings("SameParameterValue") String permission) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(thisActivity, permission)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            //noinspection StatementWithEmptyBody
            if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
                    permission)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(thisActivity, new String[]{permission},0);
            }

        }
    }

    /*
     * P2P_CLUSTER by default.
     */
    public static void setStrategy(Strategy s) {
        STRATEGY = s;
    }

    public static void startAdvertising(final Context context, PayloadCallback payloadCallback) {

        final ConnectionLifecycleCallback connectionLifecycleCallback =
                createConnectionLifecycleCallback(context, payloadCallback);

        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        Nearby.getConnectionsClient(context)
                .startAdvertising(
                        DEVICE_ID,
                        SERVICE_ID,
                        connectionLifecycleCallback,
                        advertisingOptions)
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG, "Mesh network activated on [" + SERVICE_ID + "]. Advertising.");
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "Unable to activate mesh network advertising: " + e.getMessage());
                            }
                        });
    }

    public static void startDiscovering(final Context context, PayloadCallback payloadCallback) {
        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();

        final ConnectionLifecycleCallback connectionLifecycleCallback =
                createConnectionLifecycleCallback(context, payloadCallback);

        final EndpointDiscoveryCallback endpointDiscoveryCallback =
                new EndpointDiscoveryCallback() {

                    @Override
                    public void onEndpointFound(
                            @NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {

                        Log.d(TAG, "Discovered device [" + endpointId + "].");

                        Nearby.getConnectionsClient(context)
                                .requestConnection(
                                        DEVICE_ID,
                                        endpointId,
                                        connectionLifecycleCallback)
                                .addOnSuccessListener(
                                        new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                Log.d(TAG, "Connected (as discoverer).");
                                            }
                                        })
                                .addOnFailureListener(
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.d(TAG, "Failed to connect as discoverer: " + e.getMessage());
                                            }
                                        });
                    }

                    @Override
                    public void onEndpointLost(@NonNull String endpointId) {
                        // A previously discovered endpoint has gone away.
                    }
                };

        Nearby.getConnectionsClient(context)
                .startDiscovery(
                        SERVICE_ID,
                        endpointDiscoveryCallback,
                        discoveryOptions)
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG, "Mesh network activated on [" + SERVICE_ID + "]. Discovering.");
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "Unable to activate mesh network discovery: " + e.getMessage());
                            }
                        });
    }

    public static void sendStringToAllEndpoints(Context context, String s) {
        Payload bytesPayload = Payload.fromBytes(s.getBytes());

        for (String device : connectedDevices.keySet()) {

            Nearby.getConnectionsClient(context).sendPayload(device, bytesPayload);
        }
    }

    private static ConnectionLifecycleCallback createConnectionLifecycleCallback(
            final Context context,
            final PayloadCallback payloadCallback) {
        return new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(
                    @NonNull String endpointId,
                    @NonNull ConnectionInfo connectionInfo) {

                Log.d(TAG, "Found device [" + endpointId + "].");

                // Automatically accept the connection on both sides.
                Nearby.getConnectionsClient(context)
                        .acceptConnection(endpointId, payloadCallback);
            }

            @Override
            public void onConnectionResult(@NonNull String endpointId,
                                           @NonNull ConnectionResolution result) {
                switch (result.getStatus().getStatusCode()) {
                    case ConnectionsStatusCodes.STATUS_OK:
                        Log.i(TAG, "Connected to device [" + endpointId + "].");
                        connectedDevices.put(endpointId, endpointId);
                        break;
                    case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                        Log.e(TAG, "Connection rejected by x4device [" + endpointId + "].");
                        break;
                    case ConnectionsStatusCodes.STATUS_ERROR:
                        Log.e(TAG, "Unable to connect to device [" + endpointId + "].");
                    default:
                        Log.e(TAG, "Unable to connect to device [" + endpointId + "].");
                }
            }

            @Override
            public void onDisconnected(@NonNull String endpointId) {
                Log.d(TAG, "Disconnected from device [" + endpointId + "].");
                connectedDevices.remove(endpointId);
            }
        };
    }



    public class ReceivePayloadListener extends PayloadCallback {

        @Override
        public void onPayloadReceived(@NonNull String endpointId, Payload payload) {
            // This always gets the full data of the payload. Will be null if it's not a BYTES
            // payload.
            // Check the payload type with payload.getType().
            byte[] receivedBytes = payload.asBytes();
            if (receivedBytes != null) {
                Log.d(TAG, "Received data: " + new String(receivedBytes));
            }
            else {
                Log.d(TAG, "Empty data received.");
            }

            TextView view = thisActivity.findViewById(R.id.output);
            view.setText("Connected.\nLatest received data: " + new String(receivedBytes));
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId,
                                            @NonNull PayloadTransferUpdate update) {
            // Action after the completed call to onPayloadReceived
        }
    }

}
