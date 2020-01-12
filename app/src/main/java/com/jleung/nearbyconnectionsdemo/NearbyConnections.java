package com.jleung.nearbyconnectionsdemo;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

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

@SuppressWarnings("unused, WeakerAccess")
public class NearbyConnections {

    private static final String TAG = NearbyConnections.class.getName();

    private static final String DEVICE_ID = UUID.randomUUID().toString();

    private static Strategy STRATEGY = P2P_CLUSTER;

    private static Map<String, String> connectedDevices = new HashMap<>();

    @SuppressWarnings("unused")
    public static String getDeviceId() {
        return DEVICE_ID;
    }

    /*
     * P2P_CLUSTER by default.
     */
    public static void setStrategy(Strategy s) {
        STRATEGY = s;
    }

    public static void startAdvertising(final Context context, PayloadCallback payloadCallback) {
        final String serviceId =  "com.jleung.nearbyconnectionsdemo";

        final ConnectionLifecycleCallback connectionLifecycleCallback =
                createConnectionLifecycleCallback(context, payloadCallback);

        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        Nearby.getConnectionsClient(context)
                .startAdvertising(
                        DEVICE_ID,
                        serviceId,
                        connectionLifecycleCallback,
                        advertisingOptions)
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG, "Mesh network activated. Advertising.");
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
                        DEVICE_ID,
                        endpointDiscoveryCallback,
                        discoveryOptions)
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG, "Mesh network activated. Discovering.");
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


    // Example of a custom Payload Listener
    private static class CustomPayloadListener extends PayloadCallback {

        @Override
        public void onPayloadReceived(@NonNull String endpointId, Payload payload) {
            // This always gets the full data of the payload. Will be null if it's not a BYTES
            // payload.
            // Check the payload type with payload.getType().
            byte[] receivedBytes = payload.asBytes();
            if (receivedBytes != null) {
                Log.d(TAG, "Received data: " + Arrays.toString(receivedBytes));
            }
            else {
                Log.d(TAG, "Empty data received.");
            }

            //TODO: Do something with the data.
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId,
                                            @NonNull PayloadTransferUpdate update) {
            // Action after the completed call to onPayloadReceived
        }
    }


}
