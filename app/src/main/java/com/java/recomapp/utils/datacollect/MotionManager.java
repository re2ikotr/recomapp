package com.java.recomapp.utils.datacollect;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.constraintlayout.widget.ConstraintSet;

import java.util.Timer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MotionManager {
    public final static String TAG = "MotionManager";
    private Context mContext;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SensorEventListener sensorEventListener;
    private int step_latest;
    private int step_10s_ago;
    private int step_10s;

    private ScheduledExecutorService executor;

    public MotionManager(Context context, ScheduledExecutorService executorService) {
        mContext = context;
        sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        executor = executorService;
        step_latest = 0;
        step_10s_ago = 0;
        step_10s = 0;
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                    step_latest = Math.round(event.values[0]);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        sensorManager.registerListener(sensorEventListener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);

        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                step_10s = step_latest - step_10s_ago;
                step_10s_ago = step_latest;
            }
        }, 0, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    public int getStepCount() {
        return step_10s;
    }
}
