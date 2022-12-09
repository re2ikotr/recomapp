package com.java.recomapp.utils.datacollect;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

public class DeviceManager {
    public final static String TAG = "DeviceManager";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private Context mContext;

    public DeviceManager(Context context, ScheduledExecutorService executorService) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    @SuppressLint("MissingPermission")
    public List<Integer> getDevices() {
        if (bluetoothAdapter == null)
            return new ArrayList<>();
        
        Set<BluetoothDevice> bluetoothDevices = bluetoothAdapter.getBondedDevices();
        if (bluetoothDevices == null)
            return new ArrayList<>();

        List<Integer> result = new ArrayList<>();
        for (BluetoothDevice bluetoothDevice: bluetoothDevices) {
            if (!isConnected(bluetoothDevice))
                continue;
            int major_class = bluetoothDevice.getBluetoothClass().getMajorDeviceClass();
//            major_class 详见 https://developer.android.google.cn/reference/android/bluetooth/BluetoothClass.Device.Major
            boolean already_contains = false;
            for (Integer item: result) {
                if (item == major_class) {
                    already_contains = true;
                    break;
                }
            }
            if (!already_contains)
                result.add(major_class);
        }
        return result;
    }

    private boolean isConnected(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("isConnected", (Class[]) null);
            return (boolean) m.invoke(device, (Object[]) null);
        } catch (Exception e) {
//            throw new IllegalStateException(e);
            return false;
        }
    }
}