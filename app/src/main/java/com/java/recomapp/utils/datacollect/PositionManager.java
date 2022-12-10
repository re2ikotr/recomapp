package com.java.recomapp.utils.datacollect;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

public class PositionManager {
    public final static String TAG = "PositionManager";
    private Context mContext;
    private ScheduledExecutorService executor;
    private List<Position> positionList;
    private Position latest_position;

    public PositionManager(Context context, ScheduledExecutorService executorService) {
        mContext = context;
        executor = executorService;
    }

    public String getPosition() {
        return "116.50_39.76@1665783298368";
    }
}
