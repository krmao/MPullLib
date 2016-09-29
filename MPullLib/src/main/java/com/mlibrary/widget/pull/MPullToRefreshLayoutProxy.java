package com.mlibrary.widget.pull;

import android.support.annotation.NonNull;


@SuppressWarnings("unused")
public class MPullToRefreshLayoutProxy {
    protected String TAG = getClass().getSimpleName();
    protected IPullToRefresh pullLayout;
    protected MPullLayout overScrollLayout;
    protected OnComputeScrollListener onComputeScrollListener;


    public MPullToRefreshLayoutProxy(@NonNull MPullLayout overScrollLayout) {
        this.overScrollLayout = overScrollLayout;
    }

    public boolean isAnimationNotFinished() {
        return overScrollLayout.isAnimationNotFinished();
    }

    public boolean isInDrag() {
        return overScrollLayout.isInDrag();
    }

    public void onActionDown() {
        onComputeScrollListener.onActionDown();
    }

    public void onActionUp() {
        onComputeScrollListener.onActionUp();
    }

    public void forceFinished() {
        overScrollLayout.forceFinished();
    }

    public int getFinalY() {
        return overScrollLayout.getFinalY();
    }

    public int getFinalX() {
        return overScrollLayout.getFinalX();
    }

    public int getCurrentY() {
        return overScrollLayout.getCurrentY();
    }

    public int getCurrentX() {
        return overScrollLayout.getCurrentX();
    }

    public boolean isEnabledPullRefresh() {
        return pullLayout != null;
    }

    public void setPullLayout(IPullToRefresh pullLayout) {
        this.pullLayout = pullLayout;
    }

    public void onComputeScroll(int currentX, int currentY) {
        if (onComputeScrollListener != null)
            onComputeScrollListener.onOverScroll(currentX, currentY, overScrollLayout.isInDrag());
    }

    public void smoothScrollHeaderToRefreshing() {
        if (isEnabledPullRefresh() && !overScrollLayout.isInDrag()) {
            overScrollLayout.abortNormalAnimation();
            MLogUtil.d(TAG, "[smoothScrollHeaderToRefreshing] currentX:" + getCurrentX() + ", currentY:" + getCurrentY() + ", finalX:" + getFinalX() + ", finalY:" + getFinalY());
            overScrollLayout.smoothScrollByFinalXY(0, pullLayout == null ? 0 : -pullLayout.getHeaderViewHeight(),
                    pullLayout == null ? MPullToRefreshLayout.getGlobalDurationHeaderToRefreshing() : pullLayout.getDurationHeaderToRefreshing());
        }
    }

    public void smoothScrollHeaderToNormal() {
        if (isEnabledPullRefresh() && !overScrollLayout.isInDrag()) {
            overScrollLayout.smoothScrollTo(0, 0, pullLayout == null ? MPullToRefreshLayout.getGlobalDurationHeaderToNormal() : pullLayout.getDurationHeaderToNormal());
            //MLogUtil.d(TAG, "[smoothScrollHeaderToNormal] currentX:" + getCurrentX() + ", currentY:" + getCurrentY() + ", finalX:" + getFinalX() + ", finalY:" + getFinalY());
            /*if (getFinalY() != 0)
                overScrollLayout.smoothScrollByFinalXY(0, 0, pullLayout.getDurationHeaderToNormal());
            else
                overScrollLayout.smoothScrollByCurrentXY(0, 0, pullLayout.getDurationFooterToNormal());*/
        }
    }

    public void smoothScrollFooterToNormal() {
        if (isEnabledPullRefresh() && !overScrollLayout.isInDrag()) {
            overScrollLayout.smoothScrollTo(0, 0, pullLayout == null ? MPullToRefreshLayout.getGlobalDurationFooterToNormal() : pullLayout.getDurationFooterToNormal());
            //MLogUtil.d(TAG, "[smoothScrollFooterToNormal] currentX:" + getCurrentX() + ", currentY:" + getCurrentY() + ", finalX:" + getFinalX() + ", finalY:" + getFinalY());
            /*overScrollLayout.smoothScrollByCurrentXY(0, 0,
                    pullLayout == null ? MPullToRefreshLayout.getGlobalDurationFooterToNormal() : pullLayout.getDurationFooterToNormal());*/
        }
    }

    public void smoothScrollFooterToLoading() {
        if (isEnabledPullRefresh() && !overScrollLayout.isInDrag()) {
            MLogUtil.d(TAG, "[smoothScrollFooterToLoading] currentX:" + getCurrentX() + ", currentY:" + getCurrentY() + ", finalX:" + getFinalX() + ", finalY:" + getFinalY());
            overScrollLayout.smoothScrollByCurrentXY(0, pullLayout == null ? 0 : pullLayout.getLoadMoreViewHeight(),
                    pullLayout == null ? MPullToRefreshLayout.getGlobalDurationFooterToLoading() : pullLayout.getDurationFooterToLoading());
        }
    }


    public void setOnComputeScrollListener(OnComputeScrollListener onComputeScrollListener) {
        this.onComputeScrollListener = onComputeScrollListener;
    }

    public int getHeaderViewHeight() {
        return pullLayout == null ? 0 : pullLayout.getHeaderViewHeight();
    }

    public int getLoadMoreViewHeight() {
        return pullLayout == null ? 0 : pullLayout.getLoadMoreViewHeight();
    }

    public boolean isRefreshing() {
        return pullLayout != null && pullLayout.isRefreshing();
    }

    public boolean isLoading() {
        return pullLayout != null && pullLayout.isLoading();
    }

    public boolean isEnableScrollOnRefreshing() {
        return pullLayout != null && pullLayout.isEnableTouchWhileRefreshingOrLoading();
    }

}