package com.creepy.triplemzim.creepy;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Created by Zim on 11/30/2017.
 */

public class WifiWorker extends ListenableWorker {

    private UserLogin userLogin = null;
    private static final String TAG = "WifiWorker";


    public WifiWorker(@NonNull Context appContext, @NonNull WorkerParameters params) {
        super(appContext, params);
    }

    @Override
    public ListenableFuture<Result> startWork() {

        return CallbackToFutureAdapter.getFuture(completer -> {
            Log.d(TAG, "onStartJob: job started");

            if(!checkValidity()) return Result.failure();

            Log.d(TAG, "onStartJob: validity true");
            userLogin = new UserLogin(getApplicationContext(), true);
            userLogin.execute((Void) null);
            return Result.success();
        });
    }

    @Override
    public void onStopped() {

    }

    private boolean checkValidity() {
        WifiManager wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();

        ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isWiFi = false;
        if(activeNetwork != null){
            isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        }

        return ssid.contains("ReveSystems") && isWiFi;
    }


}
