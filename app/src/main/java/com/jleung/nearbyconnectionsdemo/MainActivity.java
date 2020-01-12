package com.jleung.nearbyconnectionsdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    private Activity thisActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NearbyConnections.startAdvertising(this, new ReceivePayloadListener());

        requestPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION);


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

    public class ReceivePayloadListener extends PayloadCallback {

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

            TextView view = thisActivity.findViewById(R.id.output);
            view.setText("Connected.\nLatest received data: " + Arrays.toString(receivedBytes));
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId,
                                            @NonNull PayloadTransferUpdate update) {
            // Action after the completed call to onPayloadReceived
        }
    }

}
