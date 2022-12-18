package com.java.recomapp.feedback;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.java.recomapp.R;

public class FeedbackDialog extends Dialog {
    private TextView mTipOneView;
    private Button mOkView;
    private Button mCancelView;

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
    }
    public FeedbackDialog(Context context,String title,String oneTip,String twoTip,View.OnClickListener ok,View.OnClickListener cancel,DialogListener know) {
        this(context);
        this.title = title;
        this.oneTip = oneTip;
        this.twoTip = twoTip;
        mOkListener = ok;
        mCancelListener = cancel;
        mKnowListener = know;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback_dialog);
        mCancelView = (Button) findViewById(R.id.cancel);
        mOkView = (Button) findViewById(R.id.ok);
        mTipOneView = (TextView) findViewById(R.id.content1);

        mCancelView.setOnClickListener(mCancelListener);
        mOkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
