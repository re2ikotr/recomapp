package com.java.recomapp;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;

import com.java.recomapp.views.FloatButton;

/**
 * a service to help user simulate click event
 *
 * @author majh
 */
public class SideBarService extends AccessibilityService {

    private SideBarHideReceiver mReceiver;
    private FloatButton mFloatButton;

    private static final String ACTION_HIDE = "com.xunfeivr.maxsidebar.ACTION_HIDE";
    private static final String TAG = "side_bar_service";

    @Override
    public void onCreate() {
        super.onCreate();
        createToucher();
    }

    @SuppressLint({"RtlHardcoded", "InflateParams"})
    private void createToucher() {
        // get window manager
        WindowManager windowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        // right arrow
        mFloatButton = new FloatButton();
        LinearLayout mFloatButtonView = mFloatButton.getView(this, false, windowManager, this);
//        // left arrow
//        mLeftArrowBar = new FloatButton();
//        LinearLayout mArrowLeft = mLeftArrowBar.getView(this, true, windowManager, this);
        // handler another bar
        mFloatButton.setAnotherArrowBar(mFloatButtonView);
//        mLeftArrowBar.setAnotherArrowBar(mArrowRight);
        // register
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_HIDE);
        mReceiver = new SideBarHideReceiver();
        mReceiver.setSideBar(mFloatButton);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        mFloatButton.clearAll();
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
//        Log.i(TAG, event.toString());
    }

    @Override
    public void onInterrupt() {

    }

}
