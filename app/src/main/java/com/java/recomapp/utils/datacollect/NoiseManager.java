package com.java.recomapp.utils.datacollect;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NoiseManager {
    public final static String TAG = "NoiseManager";
    private Context mContext;
    private ScheduledExecutorService executor;
    private double latest_noise;
    private AudioRecord audioRecord;
    private static final int SAMPLE_RATE_IN_HZ = 8000;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    private long start_record_time;

    public NoiseManager(Context context, ScheduledExecutorService executorService) {
        latest_noise = 0;
        mContext = context;
        executor = executorService;
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                start_record_time = System.currentTimeMillis();
                Log.e(TAG, "begin to collect noise");
                getNoiseLevel();
            }
        }, 0, 30 * 1000, TimeUnit.MILLISECONDS);
    }

    public double getNoise() {
        return latest_noise;
    }

    private void getNoiseLevel() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.e(TAG, "Permission denied");
            latest_noise = 0;
            return;
        }
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                audioRecord.startRecording();
                short[] buffer = new short[BUFFER_SIZE];
                int epoch_cnt = 0;
                double volume_cnt = 0;
                while (System.currentTimeMillis() - start_record_time < 1000 * 3) {
                    int r = audioRecord.read(buffer, 0, BUFFER_SIZE);
                    long v = 0;
                    for (short value : buffer) {
                        v += value * value;
                    }
                    double mean = (r > 0) ? v / (double) r : 0;
                    double volume = (mean > 0) ? 10 * Math.log10(mean) : 0;
                    volume_cnt += volume;
                    epoch_cnt += 1;
                }
                latest_noise = (epoch_cnt > 0) ? volume_cnt / (double) epoch_cnt : 0;
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
        }).start();
    }
}
