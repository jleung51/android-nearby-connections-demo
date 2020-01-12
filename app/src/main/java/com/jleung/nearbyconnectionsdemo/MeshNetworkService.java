package com.jleung.nearbyconnectionsdemo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class MeshNetworkService extends Service {

    private static final String TAG = MeshNetworkService.class.getName();

    private Integer counter = 0;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        repeatOperation();

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    private void repeatOperation() {
        final Context context = this;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                Log.d(TAG, "Sending data.");
                NearbyConnections.sendStringToAllEndpoints(context, Integer.toString(counter));
                counter++;

                repeatOperation();

            }
        }, 2000);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
