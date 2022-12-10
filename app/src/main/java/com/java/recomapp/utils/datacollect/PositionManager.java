package com.java.recomapp.utils.datacollect;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.java.recomapp.MainActivity;
import com.java.recomapp.utils.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PositionManager {
    public final static String TAG = "PositionManager";
    private Context mContext;
    private ScheduledExecutorService executor;
    private List<Position> positionList;
    private Position latest_position;
    private LocationManager locationManager;
    private WifiManager wifiManager;
//    public BroadcastReceiver mReceiver;

    public PositionManager(Context context, ScheduledExecutorService executorService) {
        mContext = context;
        executor = executorService;
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        positionList = getPositionsFromFile();
//        mReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
//                    List results =wifiManager.getScanResults();
//                    if (results !=null) {
//                        Log.d(TAG,"results size: " + results.size());
//                    }
//                }
//            }
//        };
//        IntentFilter filter =new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
//        mContext.registerReceiver(mReceiver, filter);
//        executor.scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//                wifiManager.getScanResults();
//            }
//        }, 0, 30 * 1000, TimeUnit.MILLISECONDS);
        latest_position = new Position("0_0_0@0", 0, 0, new ArrayList<>());
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // permission denied
        } else {
            if (locationManager != null) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30 * 1000, 0, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        updateLatestPosition(location);
                    }
                });
            }
        }
    }

    private void updateLatestPosition(Location location) {
        Position temp = new Position("", 0, 0, new ArrayList<>());
        temp.setLatitude(location.getLatitude());
        temp.setLongitude(location.getLongitude());
        List<ScanResult> scanResults = new ArrayList<>();
        if (wifiManager != null)
            scanResults = wifiManager.getScanResults();
        List<String> wifiList = new ArrayList<>();
        for (ScanResult scanResult: scanResults) {
            wifiList.add(scanResult.SSID + "_" + scanResult.BSSID);
        }
        temp.setWifiIds(wifiList);
        boolean old_pos = false;
        for (Position position: positionList) {
            if (temp.sameAs(position)) {
                Log.e(TAG, "old position: " + position.getId());
                latest_position = position;
                old_pos = true;
                break;
            }
        }
        if (!old_pos) {
            temp.setId("" + temp.getLatitude() + "_" + temp.getLongitude() + "_" + temp.getWifiIds().size() + "@" + System.currentTimeMillis());
            latest_position = temp;
            Log.e(TAG, "new position: " + temp.getId());
            positionList.add(temp);
            writePositionsToFile();
        }
    }

    public String getPosition() {
        return latest_position.getId();
    }

    public List<Position> getPositionsFromFile() {
        Type type = new TypeToken<List<Position>>(){}.getType();
        List<Position> result = MainActivity.gson.fromJson(
                FileUtils.getFileContent(MainActivity.FILE_FOLDER + "position.json"),
                type
        );
        if (result == null)
            result = new ArrayList<>();
        return result;
    }

    public void writePositionsToFile() {
        String result = MainActivity.gson.toJson(positionList);
        Log.e(TAG, "Position List: " + result);
        FileUtils.writeStringToFile(result, new File(MainActivity.FILE_FOLDER + "position.json"));
    }
}
