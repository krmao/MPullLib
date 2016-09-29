package com.mlibrary.widget.pull.header.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mlibrary.widget.pull.R;
import com.mlibrary.widget.pull.MOverScrollState;
import com.mlibrary.widget.pull.header.MIPullHeader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MPullHeaderArrowing extends MIPullHeader {

    private ImageView mImage;
    private TextView mText;
    private TextView mTextTime;
    private TextView mTextRefreshSuccess;
    private LinearLayout mTextLayout;
    private Matrix mArrowMatrix = new Matrix();
    private boolean mIsArrowDown = true;
    private Bitmap mArrowBitmap;
    private SimpleDateFormat mTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private RotateAnimation mRotateAnimation = new RotateAnimation(0, 359, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

    public MPullHeaderArrowing(Context context) {
        this(context, null);
    }

    public MPullHeaderArrowing(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @Override
    public TextView getTitleTextView() {
        return mText;
    }

    @Override
    public TextView getTimeTextView() {
        return mTextTime;
    }

    @Override
    public TextView getResultTextView() {
        return mTextRefreshSuccess;
    }

    private void setArrowDirection(boolean isArrowDown) {
        if (mIsArrowDown == !isArrowDown) {
            rotateImage(180);
            mIsArrowDown = isArrowDown;
        }
    }

    private void rotateImage(int degrees) {
        mImage.clearAnimation();
        mArrowMatrix.setRotate(degrees);
        mArrowBitmap = Bitmap.createBitmap(mArrowBitmap, 0, 0, mArrowBitmap.getWidth(), mArrowBitmap.getHeight(), mArrowMatrix, true);
        mImage.setImageBitmap(mArrowBitmap);
    }

    @Override
    protected void onStateChange(MOverScrollState newState) {
        processOnStateChange(newState);
    }

    //==============================================================================================
    // public -- private for proguard
    //==============================================================================================

    @SuppressWarnings("deprecation")
    private void init(Context context) {
        View content = LayoutInflater.from(context).inflate(R.layout.mlibrary_pull_header_arrow_layout, this);
        if (content != null) {
            mImage = (ImageView) content.findViewById(R.id.mImage);
            mText = (TextView) content.findViewById(R.id.mText);
            mTextTime = (TextView) content.findViewById(R.id.mTextTime);
            mTextRefreshSuccess = (TextView) content.findViewById(R.id.mTextRefreshSuccess);
            mTextLayout = (LinearLayout) content.findViewById(R.id.mTextLayout);

            if (getResources() != null) {
                Drawable drawable = getResources().getDrawable(R.drawable.mlibrary_pulldown_arrow);
                if (drawable != null && drawable instanceof BitmapDrawable)
                    mArrowBitmap = ((BitmapDrawable) drawable).getBitmap();
            }
            mImage.setImageBitmap(mArrowBitmap);
            mRotateAnimation.setDuration(1000);
            mRotateAnimation.setFillAfter(true);
            mRotateAnimation.setRepeatCount(-1);
            mRotateAnimation.setInterpolator(new LinearInterpolator());
        }
    }

    private void processOnStateChange(MOverScrollState newState) {
        mText.setText(newState.getText());
        switch (newState) {
            case HEADER_REFRESH_SUCCESS:
            case HEADER_REFRESH_FAILURE:
                mTextTime.setText(String.format("最近刷新：%s", mTimeFormat.format(new Date())));
                mImage.clearAnimation();
                mImage.setImageBitmap(mArrowBitmap);
                mTextLayout.setVisibility(View.INVISIBLE);
                mImage.setVisibility(View.INVISIBLE);
                mTextRefreshSuccess.setVisibility(View.VISIBLE);
                mTextRefreshSuccess.setText(newState.getText());
                if (newState.equals(MOverScrollState.HEADER_REFRESH_SUCCESS))
                    mTextRefreshSuccess.setCompoundDrawablesWithIntrinsicBounds(R.drawable.mlibrary_pullrefresh_success, 0, 0, 0);
                else if (newState.equals(MOverScrollState.HEADER_REFRESH_FAILURE))
                    mTextRefreshSuccess.setCompoundDrawablesWithIntrinsicBounds(R.drawable.mlibrary_pullrefresh_failed, 0, 0, 0);
                break;
            case HEADER_NORMAL:
                mTextLayout.setVisibility(View.VISIBLE);
                mImage.setVisibility(View.VISIBLE);
                mTextRefreshSuccess.setVisibility(View.INVISIBLE);
                setArrowDirection(true);
                break;
            case HEADER_READY_TO_RELEASE:
                setArrowDirection(false);
                break;
            case HEADER_REFRESHING:
                mImage.setImageResource(R.drawable.mlibrary_pullloading);
                mImage.clearAnimation();
                mImage.startAnimation(mRotateAnimation);
                break;
        }
    }

}