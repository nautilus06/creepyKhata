package com.creepy.triplemzim.creepy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * created by Zim on 11/30/2017.
 */

public class WifiReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiReceiver";
    Handler handler = new Handler(Looper.getMainLooper());

    Context context;
    String ssid;

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "onReceive: Broadcast Received ");
        this.context = context;
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        ssid = wifiInfo.getSSID();
        Log.d(TAG, "onReceive: ssid: " + ssid);
//        Log.d(TAG, "onReceive: " +String.valueOf(LoginActivity.autoCheck));

        if(!checkValidity()) return;

        handler.removeCallbacks(handleWorkerRunnable);
        handler.postDelayed(handleWorkerRunnable, 1000);
    }

    private boolean checkValidity() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();

        ConnectivityManager cm =
                (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isWiFi = false;
        if (activeNetwork != null) {
            isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        }

        return ssid.contains("ReveSystems") && isWiFi;
    }

    Runnable handleWorkerRunnable = () -> handleWorker();

    private void handleWorker() {
        Log.i(TAG, "starting: handleWorker");
        SharedPreferences pref = context.getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        String temp = pref.getString("autoCheck", "false");
        boolean autoCheck = !temp.equals("false");
        if (!ssid.contains("ReveSystems") || !autoCheck) {
            Log.d(TAG, "onReceive: Cancelling broadcast");
            try {
                WorkManager.getInstance(context).cancelUniqueWork("Login-Hajirakhata");
            } catch (Exception e) {
                //do nothing
            }
        } else {
            if (pref.getString("lastDate", "a").equals(
                    DateFormat.getDateInstance().format(new Date()).toString())) {
                Log.w(TAG, "Already logged in.. returning");
                return;
            }
            Log.d(TAG, "Configuring WorkManager for logging in");

            Constraints constraints = new Constraints.Builder()
                    // The Worker needs Network connectivity
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WifiWorker.class)
                    // Sets the input data for the ListenableWorker
//                    .setInputData(inputData)
                    // If you want to delay the start of work by 60 seconds
                    .setInitialDelay(30, TimeUnit.SECONDS)
                    // Set a backoff criteria to be used when retry-ing
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                    // Set additional constraints
                    .setConstraints(constraints)
                    .build();

            Log.d(TAG, "onReceive: Enqueueing WorkManager");
            WorkManager.getInstance(context)
                    // Use ExistingWorkPolicy.REPLACE to cancel and delete any existing pending
                    // (uncompleted) work with the same unique name. Then, insert the newly-specified
                    // work.
                    .enqueueUniqueWork("Login-Hajirakhata", ExistingWorkPolicy.REPLACE, workRequest);
        }
    }
}
