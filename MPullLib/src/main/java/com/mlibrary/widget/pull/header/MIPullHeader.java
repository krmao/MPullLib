package com.mlibrary.widget.pull.header;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.mlibrary.widget.pull.MLogUtil;
import com.mlibrary.widget.pull.MOverScrollState;
import com.mlibrary.widget.pull.MPullToRefreshLayout;

public abstract class MIPullHeader extends LinearLayout implements MIPullHandler {
    public static final String TAG = MPullToRefreshLayout.TAG;

    private OnHeaderProxyListener mOnHeaderProxyListener;
    private MOverScrollState mLastState = MOverScrollState.HEADER_NORMAL;
    private boolean isLastInDrag = false;

    public interface OnHeaderProxyListener {
        boolean canRefreshing();

        void onRefresh();

        int getFinalY();

        void onHeaderStateChanged(MIPullHeader MIPullHeader, MOverScrollState oldState, MOverScrollState newState, boolean isInDrag);
    }

    public MIPullHeader(Context context) {
        this(context, null);
    }

    public MIPullHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MIPullHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected abstract void onStateChange(MOverScrollState newState);

    public void setOnHeaderProxyListener(OnHeaderProxyListener mOnHeaderProxyListener) {
        this.mOnHeaderProxyListener = mOnHeaderProxyListener;
    }

    public MOverScrollState getState() {
        return mLastState;
    }

    public boolean isRefreshing() {
        return !MOverScrollState.HEADER_REFRESHING.isRefreshCompletely();
        /*return !mLastState.equals(MOverScrollState.HEADER_NORMAL)
                && !mLastState.equals(MOverScrollState.HEADER_REFRESH_SUCCESS)
                && !mLastState.equals(MOverScrollState.HEADER_REFRESH_FAILURE);*/
    }

    //NO1 通过滑动的 currentOverScrollY 设置当前状态
    public void dispatchOverScroll(int currentOverScrollY, boolean isInDrag) {
        if (currentOverScrollY <= -getHeight()) {//代表 下拉距离超过 headerViewHeight [currentState>=headerViewHeight]
            MLogUtil.v(TAG, "[MIPullHeader:dispatchOverScroll] currentOverScrollY:" + currentOverScrollY + " , isInDrag:" + isInDrag);
            switch (mLastState) {
                case HEADER_NORMAL:
                    if (isInDrag)
                        setNewState(MOverScrollState.HEADER_READY_TO_RELEASE, true);
                    else
                        setNewState(MOverScrollState.HEADER_NORMAL, false);
                    break;
                case HEADER_READY_TO_RELEASE:
                    if (isInDrag)
                        setNewState(MOverScrollState.HEADER_READY_TO_RELEASE, true);
                    else {
                        if (mOnHeaderProxyListener != null && mOnHeaderProxyListener.canRefreshing() && mOnHeaderProxyListener.getFinalY() != 0)
                            setNewState(MOverScrollState.HEADER_REFRESHING, false);//如果 getFinalY==0 则代表快速滑动返回,会出现bug
                        else
                            setNewState(MOverScrollState.HEADER_NORMAL, false);
                    }
                    break;
                case HEADER_REFRESH_SUCCESS:
                case HEADER_REFRESH_FAILURE:
                    triggeringScroll(mLastState, mLastState, isInDrag);
                    //setNewState(MOverScrollState.HEADER_NORMAL, isInDrag);
                    break;
                case HEADER_REFRESHING:
                    setNewState(mLastState, isInDrag);
                    break;
                default:
                    break;
            }
        } else if (currentOverScrollY <= 0) {//代表 下拉距离没有超过 headerViewHeight [0=<currentState<headerViewHeight]
            MLogUtil.v(TAG, "[MIPullHeader:dispatchOverScroll] currentOverScrollY:" + currentOverScrollY + " , isInDrag:" + isInDrag);
            switch (mLastState) {
                case HEADER_REFRESHING:
                case HEADER_NORMAL:
                    setNewState(mLastState, isInDrag);
                    break;
                case HEADER_REFRESH_SUCCESS:
                case HEADER_REFRESH_FAILURE:
                    if (isInDrag)
                        setNewState(MOverScrollState.HEADER_NORMAL, true);
                    else
                        setNewState(mLastState, false);
                    break;
                case HEADER_READY_TO_RELEASE:
                    setNewState(MOverScrollState.HEADER_NORMAL, isInDrag);
                    break;
            }
        }

        if (currentOverScrollY == 0 && !isInDrag)
            MOverScrollState.HEADER_REFRESHING.setRefreshCompletely(true);
    }

    //NO2 设置当前状态,并且触发滚动
    public void setNewState(MOverScrollState newState, boolean isInDrag) {
        triggeringScroll(mLastState, newState, isInDrag);
        if (mLastState != newState || isLastInDrag != isInDrag) {//与上一次状态不完全相等
            MLogUtil.d(TAG, "[setNewState:设置新值] oldState:" + mLastState.name() + ", newState:" + newState + ",isInDrag:" + isInDrag + ",isLastInDrag:" + isLastInDrag);
            onStateChange(newState);
            triggeringOnRefresh(mLastState, newState, isInDrag);
            this.mLastState = newState;
            //开始刷新,在没有完全还原之前,不会至true
            if (!isInDrag && mLastState == MOverScrollState.HEADER_REFRESHING) {
                MOverScrollState.HEADER_REFRESHING.setRefreshCompletely(false);
            }
        } else {
            MLogUtil.d(TAG, "[setNewState:重复拦截] oldState:" + mLastState.name() + ", newState:" + newState + ",isInDrag:" + isInDrag + ",isLastInDrag:" + isLastInDrag);
        }
        isLastInDrag = isInDrag;
    }

    //触发滚动到目标状态
    private void triggeringScroll(MOverScrollState oldState, MOverScrollState newState, boolean isInDrag) {
        if (mOnHeaderProxyListener != null && !isInDrag /*&& MOverScrollState.HEADER_REFRESHING.isRefreshCompletely()*/) {
            MLogUtil.d(TAG, "[processScroll] oldState:" + mLastState.name() + ", newState:" + newState + ", isInDrag:" + false);
            mOnHeaderProxyListener.onHeaderStateChanged(this, oldState, newState, false);
        }
    }

    //触发刷新事件
    private void triggeringOnRefresh(MOverScrollState oldState, MOverScrollState newState, boolean isInDrag) {
        if (oldState != newState && mOnHeaderProxyListener != null && newState == MOverScrollState.HEADER_REFRESHING && !isInDrag)
            mOnHeaderProxyListener.onRefresh();
    }
}