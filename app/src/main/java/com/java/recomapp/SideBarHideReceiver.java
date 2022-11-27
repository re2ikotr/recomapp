package com.java.recomapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.java.recomapp.views.FloatButton;

/**
 * a receiver to accept broadcast form launcher to hide the sidebar.
 *
 * @author majh
 */
public class SideBarHideReceiver extends BroadcastReceiver {

    private FloatButton mRight = null;

    private static final String ACTION_HIDE = "com.android.sidebar.ACTION_HIDE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_HIDE)) {
            if (null != mRight) {
                mRight.launcherInvisibleSideBar();
            }
        }
    }

    public void setSideBar(FloatButton right) {
        this.mRight = right;
    }

}
