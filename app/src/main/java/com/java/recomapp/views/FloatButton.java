package com.java.recomapp.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatImageView;

import com.java.recomapp.R;
import com.java.recomapp.SideBarService;

/**
 * Arrow left & right
 *
 * @author majh
 */
public class FloatButton implements View.OnTouchListener {

    private WindowManager.LayoutParams mParams;
    private LinearLayout mArrowView;
    private Context mContext;
    private boolean mLeft;
    private WindowManager mWindowManager;
    private SideBarService mSideBarService;
    private SidePanel mContentBar;
    private androidx.constraintlayout.widget.ConstraintLayout mContentBarView;
    private LinearLayout mAnotherArrowView;

    double x = 0;
    double y = 0;
    double px = 0;
    double py = 0;

    boolean isMove = false;

    public LinearLayout getView(Context context,boolean left,WindowManager windowManager,SideBarService sideBarService) {
        mContext = context;
        mLeft = left;
        mWindowManager = windowManager;
        mSideBarService = sideBarService;
        mParams = new WindowManager.LayoutParams();
        // compatible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        // set bg transparent
        mParams.format = PixelFormat.RGBA_8888;
        // can not focusable
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mParams.x = 0;
        mParams.y = 0;
        // window size
        mParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        // get layout
        LayoutInflater inflater = LayoutInflater.from(context);
        mArrowView = (LinearLayout) inflater.inflate(R.layout.layout_float_button, null);
        AppCompatImageView arrow = mArrowView.findViewById(R.id.arrow);
        arrow.setOnTouchListener(this);
        if(left) {
            arrow.setRotation(180);
            mParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
            mParams.windowAnimations = R.style.LeftSeekBarAnim;
        }else {
            mParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
            mParams.windowAnimations = R.style.RightSeekBarAnim;
        }
        mWindowManager.addView(mArrowView,mParams);


        return mArrowView;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final WindowManager.LayoutParams floatWindowLayoutUpdateParam = mParams;

        switch (event.getAction()) {
            // When the window will be touched,
            // the x and y position of that position
            // will be retrieved
            case MotionEvent.ACTION_DOWN:
                x = floatWindowLayoutUpdateParam.x;
                y = floatWindowLayoutUpdateParam.y;

                // returns the original raw X
                // coordinate of this event
                px = event.getRawX();

                // returns the original raw Y
                // coordinate of this event
                py = event.getRawY();
                break;
            // When the window will be dragged around,
            // it will update the x, y of the Window Layout Parameter
            case MotionEvent.ACTION_MOVE:
                isMove = true;
                floatWindowLayoutUpdateParam.x = (int) ((x + event.getRawX()) - px);
                floatWindowLayoutUpdateParam.y = (int) ((y + event.getRawY()) - py);

                // updated parameter is applied to the WindowManager
                mWindowManager.updateViewLayout(mArrowView, floatWindowLayoutUpdateParam);
                break;

            case MotionEvent.ACTION_UP:
                if(isMove) {

                } else {
                    mArrowView.setVisibility(View.GONE);
                    mAnotherArrowView.setVisibility(View.GONE);
//                if(null == mContentBar || null == mContentBarView) {
//                    mContentBar = new SidePanel();
//                    mContentBarView = mContentBar.getView(mContext,mLeft,mWindowManager,mParams,mArrowView,mSideBarService, mAnotherArrowView);
//                }else {
//                    mContentBarView.setVisibility(View.VISIBLE);
//                }
                    mContentBar = new SidePanel();
                    mContentBarView = mContentBar.getView(mContext,mLeft,mWindowManager,mParams,mArrowView,mSideBarService, mAnotherArrowView);

                    mContentBar.removeOrSendMsg(false,true);
                }
                isMove = false;
                break;
        }
        return true;
    }

    public void setAnotherArrowBar(LinearLayout anotherArrowBar) {
        mAnotherArrowView = anotherArrowBar;
    }

    public void launcherInvisibleSideBar() {
        mArrowView.setVisibility(View.VISIBLE);
        if(null != mContentBar || null != mContentBarView) {
            mContentBarView.setVisibility(View.GONE);
            mContentBar.removeOrSendMsg(true,false);
            mContentBar.clearSeekBar();
        }
    }

    /**
     * when AccessibilityService is forced closed
     */
    public void clearAll() {
        mWindowManager.removeView(mArrowView);
        if(null != mContentBar || null != mContentBarView) {
            mWindowManager.removeView(mContentBarView);
            mContentBar.clearSeekBar();
            mContentBar.clearCallbacks();
        }
    }
}
