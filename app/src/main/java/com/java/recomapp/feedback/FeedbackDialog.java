package com.java.recomapp.feedback;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.java.recomapp.R;
import com.java.recomapp.SideBarService;
import com.java.recomapp.decisiontree.Decision;
import com.java.recomapp.decisiontree.Features;

import java.util.ArrayList;

public class FeedbackDialog extends Dialog {
    private TextView mTipOneView;
    private Button mOkView;
    private Button mCancelView;

    private Context mContext;
    private Context testContext;

    private CheckBox mSport;
    private CheckBox mBluetooth;
    private CheckBox mTime;
    private CheckBox mLoud;
    private CheckBox mPlace;
    private CheckBox mLastApp;

    private View.OnClickListener mOkListener;
    private View.OnClickListener mCancelListener;
    private DialogListener mKnowListener;

    private String title;
    private String oneTip;
    private String twoTip;

    private void setOnDialogListener(DialogListener listener){
        this.mKnowListener = listener;
    }


    public FeedbackDialog(Context context) {
        super(context);
        mContext = context;
    }
    public FeedbackDialog(Context context,String title,String oneTip,String twoTip,View.OnClickListener ok,View.OnClickListener cancel,DialogListener know) {
        this(context);
        mContext = context;
        this.title = title;
        this.twoTip = twoTip;
        mOkListener = ok;
        mCancelListener = cancel;
        mKnowListener = know;
        testContext = this.getContext();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback_dialog);
        mCancelView = (Button) findViewById(R.id.cancel);
        mOkView = (Button) findViewById(R.id.ok);
        mTipOneView = (TextView) findViewById(R.id.content1);

        mSport = (CheckBox) findViewById(R.id.case_sports);
        mBluetooth = (CheckBox) findViewById(R.id.case_bluetooth);
        mTime = (CheckBox) findViewById(R.id.case_time);
        mLoud = (CheckBox) findViewById(R.id.case_loud);
        mPlace = (CheckBox) findViewById(R.id.case_place);
        mLastApp = (CheckBox) findViewById(R.id.case_lastapp);

        mCancelView.setOnClickListener(mCancelListener);
        mOkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String last_app = SideBarService.last_packageName;
                ArrayList<Integer> temp = new ArrayList<>();
                if(mTime.isChecked()) {
                    temp.add(Features.TIME);
                }
                if(mLastApp.isChecked()) {
                    temp.add(Features.APP);
                }
                if(mBluetooth.isChecked()) {
                    temp.add(Features.DEVICE);
                }
                if(mLoud.isChecked()) {
                    temp.add(Features.NOISE);
                }

                if(mPlace.isChecked()) {
                    temp.add(Features.POSITION);
                }
                if(mSport.isChecked()) {
                    temp.add(Features.STEP);
                }
                int a[] = new int[temp.size()];
                for(int i = 0; i < temp.size(); i++) {
                    a[i] = temp.get(i);
                }

                SideBarService.decisionTree.update(last_app, a);
                Toast.makeText(mContext, "反馈成功，感谢支持", Toast.LENGTH_SHORT).show();
                Log.i("feedback clicked", "feedback" + temp.toString() + a.toString());
                mKnowListener.onClick(FeedbackDialog.this,view);
            }
        });
        mCancelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mKnowListener.onClick(FeedbackDialog.this,view);
            }
        });
    }
}
