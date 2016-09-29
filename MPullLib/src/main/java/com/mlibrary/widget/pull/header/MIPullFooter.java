package com.mlibrary.widget.pull.header;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.mlibrary.widget.pull.MLogUtil;
import com.mlibrary.widget.pull.MOverScrollState;

public abstract class MIPullFooter extends LinearLayout implements MIPullHandler {
    public String TAG = getClass().getSimpleName();

    private OnFooterProxyListener mOnFooterProxyListener;
    private MOverScrollState mLastState = MOverScrollState.FOOTER_NORMAL;

    public interface OnFooterProxyListener {
        void onLoading();

        boolean canLoading();

        void onFooterStateChanged(MIPullFooter loadMoreView, MOverScrollState oldState, MOverScrollState newState, boolean isInDrag);
    }

    public MIPullFooter(Context context) {
        this(context, null);
    }

    public MIPullFooter(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MIPullFooter(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected abstract void onStateChange(MOverScrollState newState);

    public void processClick() {
        if (mLastState == MOverScrollState.FOOTER_NORMAL || mLastState == MOverScrollState.FOOTER_LOAD_FAILURE)
            setNewState(MOverScrollState.FOOTER_LOADING, false);
    }

    public void setOnFooterProxyListener(OnFooterProxyListener onFooterProxyListener) {
        this.mOnFooterProxyListener = onFooterProxyListener;
    }

    public MOverScrollState getState() {
        return mLastState;
    }

    public boolean isLoading() {
        return !MOverScrollState.FOOTER_LOADING.isLoadCompletely();
    }

    //NO1 通过滑动的 currentOverScrollY 设置当前状态
    public void dispatchOverScroll(int currentOverScrollY, boolean isInDrag) {
        if (currentOverScrollY >= getHeight()) {//代表 上拉距离超过 loadMoreViewHeight [currentState>=loadMoreViewHeight]
            MLogUtil.v(TAG, "[dispatchOverScroll 当前位置:释放] currentOverScrollY:" + currentOverScrollY + " , isInDrag:" + isInDrag);
            switch (mLastState) {
                case FOOTER_NORMAL:
                    if (isInDrag)
                        setNewState(MOverScrollState.FOOTER_READY_TO_RELEASE, true);
                    else
                        setNewState(MOverScrollState.FOOTER_NORMAL, false);
                    break;
                case FOOTER_READY_TO_RELEASE:
                    if (isInDrag)
                        setNewState(MOverScrollState.FOOTER_READY_TO_RELEASE, true);
                    else {
                        if (mOnFooterProxyListener == null || mOnFooterProxyListener.canLoading())
                            setNewState(MOverScrollState.FOOTER_LOADING, false);
                        else
                            setNewState(MOverScrollState.FOOTER_NORMAL, false);
                    }
                    break;

                case FOOTER_NO_MORE_DATA:
                case FOOTER_LOADING:
                case FOOTER_LOAD_SUCCESS:
                case FOOTER_LOAD_FAILURE:
                    setNewState(mLastState, isInDrag);
                    break;
            }
        } else if (currentOverScrollY >= 0) {//代表 下拉距离没有超过 loadMoreViewHeight [0=<currentState<loadMoreViewHeight]
            MLogUtil.v(TAG, "[dispatchOverScroll 当前位置:上拉] currentOverScrollY:" + currentOverScrollY + " , isInDrag:" + isInDrag);
            switch (mLastState) {
                case FOOTER_NO_MORE_DATA:
                case FOOTER_LOADING:
                case FOOTER_NORMAL:
                    setNewState(mLastState, isInDrag);
                    break;
                case FOOTER_LOAD_SUCCESS:
                case FOOTER_LOAD_FAILURE:
                    if (isInDrag)
                        setNewState(MOverScrollState.FOOTER_NORMAL, true);
                    else
                        setNewState(mLastState, false);
                    break;
                case FOOTER_READY_TO_RELEASE:
                    setNewState(MOverScrollState.FOOTER_NORMAL, isInDrag);
                    break;
            }
        }

        if (currentOverScrollY == 0 && !isInDrag)
            MOverScrollState.FOOTER_LOADING.setLoadCompletely(true);
    }

    private boolean isLastInDrag = false;

    //NO2 设置当前状态,并且触发滚动
    public void setNewState(MOverScrollState newState, boolean isInDrag) {
        triggeringScroll(mLastState, newState, isInDrag);
        if (mLastState != newState || isLastInDrag != isInDrag) {
            MLogUtil.d(TAG, "[setNewState:设置新值] oldState:" + mLastState.name() + ", newState:" + newState + ",isInDrag:" + isInDrag + ",isLastInDrag:" + isLastInDrag);
            onStateChange(newState);
            triggeringOnLoading(mLastState, newState, isInDrag);
            this.mLastState = newState;
            //开始刷新,在没有完全还原之前,不会至true
            if (!isInDrag && mLastState == MOverScrollState.FOOTER_LOADING)
                MOverScrollState.FOOTER_LOADING.setLoadCompletely(false);
        } else {
            MLogUtil.d(TAG, "[setNewState:重复拦截] oldState:" + mLastState.name() + ", newState:" + newState + ",isInDrag:" + isInDrag + ",isLastInDrag:" + isLastInDrag);
        }
        isLastInDrag = isInDrag;
    }

    //触发滚动到目标状态
    private void triggeringScroll(MOverScrollState oldState, MOverScrollState newState, boolean isInDrag) {
        if (mOnFooterProxyListener != null && !isInDrag /*&& MOverScrollState.FOOTER_LOADING.isLoadCompletely()*/) {
            MLogUtil.d(TAG, "[processScroll] oldState:" + mLastState.name() + ", newState:" + newState + ", isInDrag:" + false);
            mOnFooterProxyListener.onFooterStateChanged(this, oldState, newState, false);
        }
    }

    //触发刷新事件
    private void triggeringOnLoading(MOverScrollState oldState, MOverScrollState newState, boolean isInDrag) {
        if (oldState != newState && mOnFooterProxyListener != null && newState == MOverScrollState.FOOTER_LOADING && !isInDrag)
            mOnFooterProxyListener.onLoading();
    }
}
