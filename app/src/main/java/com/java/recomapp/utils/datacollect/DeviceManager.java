package com.java.recomapp.utils.datacollect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeviceManager {
    public final static String TAG = "DeviceManager";

    public enum DeviceType {
        EARPHONE, SPEAKER
    }

    public List<DeviceType> getDevices() {
        return new ArrayList<>(Arrays.asList(DeviceType.EARPHONE));
    }
}
