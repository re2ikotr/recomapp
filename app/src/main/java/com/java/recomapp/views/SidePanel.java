package com.java.recomapp.views;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.java.recomapp.R;
import com.java.recomapp.SideBarService;
import com.java.recomapp.utils.PermissionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Sidebar left & right
 *
 * @author majh
 */
public class SidePanel implements View.OnClickListener {

    private Context mContext;
    private boolean mLeft;
//    private LinearLayout mContentView;
    private androidx.constraintlayout.widget.ConstraintLayout mContentView;
    private WindowManager mWindowManager;
    private LinearLayout mArrowView;
    private SideBarService mSideBarService;
    private ControlBar mControlBar;
    private LinearLayout mSeekBarView;
    private LinearLayout mAnotherArrowView;
    private Button exit_button;
    private int mTagTemp = -1;

    private ArrayList<Map.Entry<String, Integer>> appList;
    private PackageManager packageManager;
    private List<PackageInfo> packageInfoList;
    private String TAG = "side_panel";

    private static final int COUNT_DOWN_TAG = 1;
    private static final int COUNT_DOWN_TIME = 5000;

    private LayoutInflater mLayoutInflater;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case COUNT_DOWN_TAG:
                    goNormal();
                    break;
            }
        }
    };

    androidx.constraintlayout.widget.ConstraintLayout getView(Context context,
                         boolean left,
                         WindowManager windowManager,
                         WindowManager.LayoutParams params,
                         LinearLayout arrowView,
                         SideBarService sideBarService,
                         LinearLayout anotherArrowView) {
        mContext = context;
        mLeft = left;
        mWindowManager = windowManager;
        mArrowView = arrowView;
        mSideBarService = sideBarService;
        mAnotherArrowView = anotherArrowView;
        // get layout
        if (mLayoutInflater == null) {
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        mContentView = (androidx.constraintlayout.widget.ConstraintLayout) mLayoutInflater.inflate(R.layout.layout_panel, null, false);
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Click", "onClick in SidePanel");
            }
        });


        Map<String, Integer> appCount = SideBarService.app_count;
        appList = new ArrayList<>();
        if(appCount != null) {
            appList = new ArrayList<Map.Entry<String, Integer>>(appCount.entrySet());
            for(int i = 0; i < appList.size(); i++) {
                if(SideBarService.systemPackages.contains(appList.get(i).getKey())) {
                    appList.get(i).setValue(0);
                }
            }
            Collections.sort(appList, new Comparator<Map.Entry<String, Integer>>() {
                public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
                    return (e2.getValue() - e1.getValue());
                }
            });
        }

        packageManager = mContext.getPackageManager();
        packageInfoList = packageManager.getInstalledPackages(0);
        // To get a certain app's icon (like meituan), and link the imageView with that app, use:
        ImageView imageView1 = mContentView.findViewById(R.id.app1);
        ImageView imageView2 = mContentView.findViewById(R.id.app2);
        ImageView imageView3 = mContentView.findViewById(R.id.app3);
        ImageView imageView4 = mContentView.findViewById(R.id.app4);
        ImageView imageView5 = mContentView.findViewById(R.id.app5);
        exit_button = mContentView.findViewById(R.id.exit_button);
        Log.i(TAG, appList.toString());
        exit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Click", "onClick in exit_button");
                goNormal();
            }
        });
        for (PackageInfo packageInfo: packageInfoList) {
            if (appList.size() > 0 && packageInfo.applicationInfo.packageName.equals(appList.get(0).getKey())) {
                String app1 = appList.get(0).getKey();
                imageView1.setImageDrawable(packageInfo.applicationInfo.loadIcon(packageManager));
                imageView1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mContext.startActivity(packageManager.getLaunchIntentForPackage(app1));
                    }
                });
            }
            if (appList.size() > 1 && packageInfo.applicationInfo.packageName.equals(appList.get(1).getKey())) {
                String app2 = appList.get(1).getKey();
                imageView2.setImageDrawable(packageInfo.applicationInfo.loadIcon(packageManager));
                imageView2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mContext.startActivity(packageManager.getLaunchIntentForPackage(app2));
                    }
                });
            }
            if (appList.size() > 2 && packageInfo.applicationInfo.packageName.equals(appList.get(2).getKey())) {
                String app3 = appList.get(2).getKey();
                imageView3.setImageDrawable(packageInfo.applicationInfo.loadIcon(packageManager));
                imageView3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mContext.startActivity(packageManager.getLaunchIntentForPackage(app3));
                    }
                });
            }
            if (appList.size() > 3 && packageInfo.applicationInfo.packageName.equals(appList.get(3).getKey())) {
                String app4 = appList.get(3).getKey();
                imageView4.setImageDrawable(packageInfo.applicationInfo.loadIcon(packageManager));
                imageView4.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mContext.startActivity(packageManager.getLaunchIntentForPackage(app4));
                    }
                });
            }
            if (appList.size() > 4 && packageInfo.applicationInfo.packageName.equals(appList.get(4).getKey())) {
                String app5 = appList.get(4).getKey();
                imageView5.setImageDrawable(packageInfo.applicationInfo.loadIcon(packageManager));
                imageView5.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mContext.startActivity(packageManager.getLaunchIntentForPackage(app5));
                    }
                });
            }
        }
        mWindowManager.addView(mContentView,params);
        return mContentView;
    }

    @Override
    public void onClick(View v) {
    }

    private void brightnessOrVolume(int tag) {
        if(mTagTemp == tag) {
            if(null != mSeekBarView) {
                removeSeekBarView();
            }else {
                addSeekBarView(tag);
            }
            return;
        }
        mTagTemp = tag;
        if(null == mControlBar) {
            mControlBar = new ControlBar();
        }
        if(null == mSeekBarView) {
            addSeekBarView(tag);
        }else {
            removeSeekBarView();
            addSeekBarView(tag);
        }
    }

    private void addSeekBarView(int tag) {
        mSeekBarView = mControlBar.getView(mContext,mLeft,tag,this);
        mWindowManager.addView(mSeekBarView, mControlBar.mParams);
    }

    private void removeSeekBarView() {
        if(null != mSeekBarView) {
            mWindowManager.removeView(mSeekBarView);
            mSeekBarView = null;
        }
    }

    private void arrowsShow() {
        mContentView.setVisibility(View.GONE);
        mArrowView.setVisibility(View.VISIBLE);
        mAnotherArrowView.setVisibility(View.VISIBLE);
    }

    void clearSeekBar() {
        if(null != mSeekBarView) {
            mWindowManager.removeView(mSeekBarView);
            mSeekBarView = null;
        }
    }

    private void goNormal() {
        arrowsShow();
        clearSeekBar();
    }

    private void annotationGo() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.notes", "com.android.notes.MainActivity"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mContext.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(mContext, mContext.getString(R.string.app_not_find), Toast.LENGTH_SHORT).show();
        }
    }

    void removeOrSendMsg(boolean remove, boolean send) {
        if(remove) {
            mHandler.removeMessages(COUNT_DOWN_TAG);
        }
        if(send) {
            mHandler.sendEmptyMessageDelayed(COUNT_DOWN_TAG,COUNT_DOWN_TIME);
        }
    }

    /**
     * when AccessibilityService is forced closed
     */
    void clearCallbacks() {
        if(null != mHandler) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    private void brightnessPermissionCheck() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!PermissionUtil.isSettingsCanWrite(mContext)) {
                goNormal();
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                Toast.makeText(mContext,mContext.getString(R.string.setting_modify_toast),Toast.LENGTH_LONG).show();
            }else {
                brightnessOrVolume(0);
            }
        }else {
            brightnessOrVolume(0);
        }
    }
}
