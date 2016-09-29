package com.mlibrary.widget.pull;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.mlibrary.widget.pull.header.MIPullFooter;
import com.mlibrary.widget.pull.header.MIPullHeader;
import com.mlibrary.widget.pull.header.impl.MPullFooterArrowing;
import com.mlibrary.widget.pull.header.impl.MPullHeaderArrowing;

@SuppressWarnings("unused")
public class MPullToRefreshLayout extends FrameLayout implements IPullToRefresh {
    public static final String TAG = "MPullToRefreshLayout";
    protected static int globalDurationHeaderToRefreshing = 1000;
    protected static int globalDurationHeaderToNormal = 1000;
    protected static int globalDurationFooterToNormal = 1000;
    protected static int globalDurationFooterToLoading = 1000;
    protected int durationHeaderToRefreshing = globalDurationHeaderToRefreshing;
    protected int durationHeaderToNormal = globalDurationHeaderToNormal;
    protected int durationFooterToNormal = globalDurationFooterToNormal;
    protected int durationFooterToLoading = globalDurationFooterToLoading;

    protected MIPullHeader headerView;
    protected MIPullFooter footerView;
    protected OnPullRefreshListener onPullRefreshListener;
    protected MPullToRefreshLayoutProxy mPullToRefreshLayoutProxy;
    protected boolean isAutoLoadMore = false;
    protected boolean enableTouchWhileRefreshingOrLoading = true;

    protected int lastX = 0;
    protected int lastY = 0;
    protected boolean isLastInDrag = false;

    //xml layout config start ======================================================================
    /*<attr name="pullMode" format="enum">
        <enum name="both" value="0" />
        <enum name="header" value="1" />
        <enum name="footer" value="2" />
        <enum name="none" value="3" />
    </attr>*/
    public static final int MODE_BOTH = 0;
    public static final int MODE_HEADER = 1;
    public static final int MODE_FOOTER = 2;
    public static final int MODE_NONE = 3;
    protected int pullMode = MODE_BOTH;

    protected static int globalPullBackgroundColor = Color.parseColor("#DDFFFFFF");
    protected static int globalPullTextColor = Color.parseColor("#14171E");
    protected int pullBackground = globalPullBackgroundColor;
    protected int pullTextColor = globalPullTextColor;

    //xml layout config end ========================================================================

    public MPullToRefreshLayout(Context context) {
        this(context, null);
    }

    public MPullToRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MPullToRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //xml layout config start ==================================================================
        headerView = new MPullHeaderArrowing(context);
        footerView = new MPullFooterArrowing(context);
        //typedArray 只有在构造函数里面才有值,怪事,所以预先初始化 headerView and footerView 因为 background是多样性的,既可以是色值,也可以是图片,直接复制比较方便
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MPullToRefreshLayout);
        pullMode = typedArray.getInt(R.styleable.MPullToRefreshLayout_pullMode, pullMode);
        enableTouchWhileRefreshingOrLoading = typedArray.getBoolean(R.styleable.MPullToRefreshLayout_pullEnableTouchWhileRefreshingOrLoading, enableTouchWhileRefreshingOrLoading);
        MPullToRefreshLayoutViewUtil.initAttrsBackground(typedArray, headerView, R.styleable.MPullToRefreshLayout_pullBackground, pullBackground);
        MPullToRefreshLayoutViewUtil.initAttrsBackground(typedArray, headerView, R.styleable.MPullToRefreshLayout_pullHeaderBackground, -1);
        MPullToRefreshLayoutViewUtil.initAttrsTextColor(typedArray, headerView, R.styleable.MPullToRefreshLayout_pullTextColor, pullTextColor);
        MPullToRefreshLayoutViewUtil.initAttrsTextColor(typedArray, headerView, R.styleable.MPullToRefreshLayout_pullHeaderTextColor, -1);
        MPullToRefreshLayoutViewUtil.initAttrsBackground(typedArray, footerView, R.styleable.MPullToRefreshLayout_pullBackground, pullBackground);
        MPullToRefreshLayoutViewUtil.initAttrsBackground(typedArray, footerView, R.styleable.MPullToRefreshLayout_pullFooterBackground, -1);
        MPullToRefreshLayoutViewUtil.initAttrsTextColor(typedArray, footerView, R.styleable.MPullToRefreshLayout_pullTextColor, pullTextColor);
        MPullToRefreshLayoutViewUtil.initAttrsTextColor(typedArray, footerView, R.styleable.MPullToRefreshLayout_pullFooterTextColor, -1);
        typedArray.recycle();
        //xml layout config end ====================================================================
    }

    public void setDebugAble(boolean isDebug) {
        MLogUtil.setDebugAble(isDebug);
    }

    @Override
    protected void onFinishInflate() {
        if (getChildCount() > 0) {
            View childTopView = getChildAt(0);
            if (childTopView instanceof MPullLayout) {
                final FrameLayout.LayoutParams topLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                topLayoutParams.gravity = Gravity.TOP;
                topLayoutParams.topMargin = headerView.getMeasuredHeight();
                headerView.setLayoutParams(topLayoutParams);
                final FrameLayout.LayoutParams bottomLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                bottomLayoutParams.gravity = Gravity.BOTTOM;
                bottomLayoutParams.bottomMargin = -footerView.getMeasuredHeight();
                footerView.setLayoutParams(bottomLayoutParams);
                addView(headerView);
                addView(footerView);
                setPullMode(pullMode);//control visible

                headerView.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @SuppressWarnings("deprecation")
                            @Override
                            public void onGlobalLayout() {
                                headerView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                                topLayoutParams.topMargin = -headerView.getMeasuredHeight();
                                headerView.setLayoutParams(topLayoutParams);
                            }
                        });
                footerView.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @SuppressWarnings("deprecation")
                            @Override
                            public void onGlobalLayout() {
                                footerView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                                bottomLayoutParams.bottomMargin = -footerView.getMeasuredHeight();
                                footerView.setLayoutParams(bottomLayoutParams);
                            }
                        });

                headerView.setOnHeaderProxyListener(new MIPullHeader.OnHeaderProxyListener() {
                    @Override
                    public boolean canRefreshing() {
                        return !isLoading();
                    }

                    @Override
                    public void onRefresh() {
                        if (onPullRefreshListener != null && isEnablePullHeaderToRefresh()) {
                            MLogUtil.d(TAG, "触发 onRefresh");
                            onPullRefreshListener.onRefresh();
                        }
                    }

                    @Override
                    public int getFinalY() {
                        return mPullToRefreshLayoutProxy != null ? mPullToRefreshLayoutProxy.getFinalY() : 0;
                    }

                    @Override
                    public void onHeaderStateChanged(MIPullHeader MIPullHeader, MOverScrollState oldState, MOverScrollState newState, boolean isInDrag) {
                        if (!isInDrag) {
                            MLogUtil.v(TAG, "[onHeaderStateChanged] oldState:" + oldState.name() + ", newState:" + newState.name() + ", isInDrag:" + false);
                            //此处只处理滚动
                            switch (newState) {
                                case HEADER_NORMAL:
                                    MPullToRefreshLayout.this.smoothScrollHeaderToNormal();
                                    break;
                                case HEADER_REFRESH_SUCCESS:
                                    MPullToRefreshLayout.this.smoothScrollHeaderToNormal();
                                    break;
                                case HEADER_REFRESH_FAILURE:
                                case HEADER_REFRESHING:
                                case HEADER_READY_TO_RELEASE:
                                    if (isEnablePullHeaderToRefresh())
                                        MPullToRefreshLayout.this.smoothScrollHeaderToRefreshing();
                                    else
                                        MPullToRefreshLayout.this.smoothScrollHeaderToNormal();
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                });
                footerView.setOnFooterProxyListener(new MIPullFooter.OnFooterProxyListener() {
                    /*@Override
                    public boolean isAnimationNotFinished() {
                        return _pullLayoutProxy.isAnimationNotFinished();
                    }*/

                    @Override
                    public void onLoading() {
                        if (onPullRefreshListener != null && isEnablePullFooterToLoading()) {
                            MLogUtil.d(TAG, "触发 onLoading");
                            onPullRefreshListener.onLoadMore();
                        }
                    }

                    @Override
                    public boolean canLoading() {
                        return !isRefreshing();
                    }

                    @Override
                    public void onFooterStateChanged(MIPullFooter loadMoreView, MOverScrollState oldState, MOverScrollState newState, boolean isInDrag) {
                        if (!isInDrag) {
                            MLogUtil.v(TAG, "[onFooterStateChanged:处理滑动] oldState:" + oldState.name() + ", newState:" + newState.name() + ", isInDrag:" + false);
                            //此处只处理滚动
                            switch (newState) {
                                case FOOTER_NORMAL:
                                case FOOTER_LOAD_SUCCESS:
                                case FOOTER_NO_MORE_DATA:
                                    MPullToRefreshLayout.this.smoothScrollFooterToNormal();
                                    break;
                                case FOOTER_LOAD_FAILURE:
                                case FOOTER_LOADING:
                                case FOOTER_READY_TO_RELEASE:
                                    if (isEnablePullFooterToLoading())
                                        MPullToRefreshLayout.this.smoothScrollFooterToLoading();
                                    else
                                        MPullToRefreshLayout.this.smoothScrollFooterToNormal();
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                });
            }
        }
        super.onFinishInflate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mPullToRefreshLayoutProxy == null && getChildCount() > 0) {
            View childTopView = getChildAt(0);
            if (childTopView instanceof MPullLayout) {
                MPullLayout overScrollLayout = (MPullLayout) childTopView;
                mPullToRefreshLayoutProxy = overScrollLayout.getPullLayoutProxy();
                mPullToRefreshLayoutProxy.setOnComputeScrollListener(onComputeScrollListener);
                MLogUtil.d(MPullToRefreshLayout.TAG, "[onLayout] PullToRefreshLayout = overScrollLayout.getPullLayoutProxy();");
            }
        }
    }

    protected OnOverScrollListener onOverScrollListener;

    public void setOnOverScrollListener(OnOverScrollListener onOverScrollListener) {
        this.onOverScrollListener = onOverScrollListener;
    }

    protected OnComputeScrollListener onComputeScrollListener = new OnComputeScrollListener() {
        @Override
        public void onOverScroll(int currentX, int currentY, boolean isInDrag) {
            {
                //remove repeat onOverScroll
                if (lastX == currentX && lastY == currentY && isLastInDrag == isInDrag) {
                    MLogUtil.w(TAG, "[onOverScroll-重复不处理] currentX:" + currentX + ", currentY:" + currentY
                            + ",isInDrag:" + isInDrag
                            + ",isSmoothingScrollHeaderToRefreshingNow:" + isSmoothingScrollHeaderToRefreshingNow
                            + ",isSmoothingScrollLoadMoreToLoadingNow:" + isSmoothingScrollLoadMoreToLoadingNow
                            + ",isSmoothingScrollHeaderToNormalNow:" + isSmoothingScrollHeaderToNormalNow
                            + ",isSmoothingScrollFooterToNormalNow:" + isSmoothingScrollFooterToNormalNow
                    );
                    return;
                }

                /*if (*//*(*//*isInDrag || currentY == -getHeaderViewHeight()*//*) && lastComputeY >= -getHeaderViewHeight()*//*)
                    isSmoothingScrollHeaderToRefreshingNow = false;
                if (*//*(*//*isInDrag || currentY == getLoadMoreViewHeight()*//*) && lastComputeY <= getHeaderViewHeight()*//*)
                    isSmoothingScrollLoadMoreToLoadingNow = false;
                if ((isInDrag || currentY == 0))
                    isSmoothingScrollHeaderToNormalNow = false;
                if (isInDrag || currentY == 0)
                    isSmoothingScrollFooterToNormalNow = false;*/

                lastX = currentX;
                lastY = currentY;
                isLastInDrag = isInDrag;
            }

            MLogUtil.v(TAG, "[onOverScroll] currentX:" + currentX + ", currentY:" + currentY + ", finalX:" + mPullToRefreshLayoutProxy.getFinalX() + ", finalY:" + mPullToRefreshLayoutProxy.getFinalY()
                    + ",isInDrag:" + isInDrag
                    + ",isSmoothingScrollHeaderToRefreshingNow:" + isSmoothingScrollHeaderToRefreshingNow
                    + ",isSmoothingScrollLoadMoreToLoadingNow:" + isSmoothingScrollLoadMoreToLoadingNow
                    + ",isSmoothingScrollHeaderToNormalNow:" + isSmoothingScrollHeaderToNormalNow
                    + ",isSmoothingScrollFooterToNormalNow:" + isSmoothingScrollFooterToNormalNow
            );
            if (onOverScrollListener != null)
                onOverScrollListener.onOverScroll(currentX, currentY, isInDrag);

            if (headerView != null) {
                headerView.setTranslationY(-currentY);
                headerView.dispatchOverScroll(currentY, isInDrag);
            }
            if (footerView != null) {
                footerView.setTranslationY(-currentY);
                footerView.dispatchOverScroll(currentY, isInDrag);
            }
        }

        @Override
        public void onActionDown() {
            isSmoothingScrollHeaderToRefreshingNow = false;
            isSmoothingScrollHeaderToNormalNow = false;
            isSmoothingScrollLoadMoreToLoadingNow = false;
            isSmoothingScrollFooterToNormalNow = false;
        }

        @Override
        public void onActionUp() {
            isSmoothingScrollHeaderToRefreshingNow = false;
            isSmoothingScrollLoadMoreToLoadingNow = false;
            isSmoothingScrollHeaderToNormalNow = false;
            isSmoothingScrollFooterToNormalNow = false;
        }
    };

    @Override
    public boolean isRefreshing() {
        return headerView != null && headerView.isRefreshing();
    }

    @Override
    public boolean isLoading() {
        return footerView != null && footerView.isLoading();
    }

    //无关紧要
    public void completeHeaderRefreshSuccess() {
        if (headerView != null) {
            MLogUtil.d(TAG, "[completeHeaderRefreshSuccess]");
            headerView.setNewState(MOverScrollState.HEADER_REFRESH_SUCCESS, false);
        }
        resetFooterView();
    }

    public void completeHeaderRefreshFailure() {
        if (headerView != null) {
            MLogUtil.d(TAG, "[completeHeaderRefreshFailure]");
            headerView.setNewState(MOverScrollState.HEADER_REFRESH_FAILURE, false);
        }
        resetFooterView();
    }

    public void completeFooterLoadSuccess() {
        if (footerView != null) {
            MLogUtil.d(TAG, "[completeFooterLoadSuccess]");
            footerView.setNewState(MOverScrollState.FOOTER_LOAD_SUCCESS, false);
        }
    }

    public void completeFooterLoadNoMoreData() {
        if (footerView != null) {
            MLogUtil.d(TAG, "[completeFooterLoadNoMoreData]");
            footerView.setNewState(MOverScrollState.FOOTER_NO_MORE_DATA, false);
        }
    }

    public void completeFooterLoadFailure() {
        if (footerView != null) {
            MLogUtil.d(TAG, "[completeFooterLoadFailure]");
            footerView.setNewState(MOverScrollState.FOOTER_LOAD_FAILURE, false);
        }
    }

    /*
        auto resetFooterView in completeHeader**
        当没有更多数据的时候,调用此方法,可以继续加载更多
    */
    public void resetFooterView() {
        if (footerView != null) {
            MLogUtil.d(TAG, "[resetFooterView]");
            footerView.setNewState(MOverScrollState.FOOTER_NORMAL, false);
        }
    }

    public void setOnPullRefreshListener(OnPullRefreshListener onPullRefreshListener) {
        this.onPullRefreshListener = onPullRefreshListener;
    }

    protected boolean isSmoothingScrollHeaderToRefreshingNow = false;
    protected boolean isSmoothingScrollHeaderToNormalNow = false;
    protected boolean isSmoothingScrollFooterToNormalNow = false;
    protected boolean isSmoothingScrollLoadMoreToLoadingNow = false;

    @Override
    public void smoothScrollHeaderToRefreshing() {
        if (mPullToRefreshLayoutProxy != null && lastY != -getHeaderViewHeight() && !isSmoothingScrollHeaderToRefreshingNow && !mPullToRefreshLayoutProxy.isInDrag()) {
            MLogUtil.v(TAG, "[smoothScrollHeaderToRefreshing] true");
            mPullToRefreshLayoutProxy.smoothScrollHeaderToRefreshing();
            isSmoothingScrollHeaderToRefreshingNow = true;
            isSmoothingScrollFooterToNormalNow = false;
        } else {
            MLogUtil.v(TAG, "[smoothScrollHeaderToRefreshing] false, no need todo this");
        }
    }

    @Override
    public void smoothScrollFooterToNormal() {
        if (mPullToRefreshLayoutProxy != null) {
            if (/*lastY != 0 &&*/ !isSmoothingScrollFooterToNormalNow && !isSmoothingScrollHeaderToNormalNow && !mPullToRefreshLayoutProxy.isInDrag()) {
                MLogUtil.d(TAG, "[smoothScrollFooterToNormal true] smoothingNow?" + isSmoothingScrollFooterToNormalNow + ", isInDrag:" + mPullToRefreshLayoutProxy.isInDrag() + ",lastComputeY != 0?" + (lastY != 0) + ",isSmoothingScrollHeaderToNormalNow:" + isSmoothingScrollHeaderToNormalNow);
                mPullToRefreshLayoutProxy.smoothScrollFooterToNormal();
                isSmoothingScrollFooterToNormalNow = true;
            } else {
                MLogUtil.d(TAG, "[smoothScrollFooterToNormal false] smoothingNow?" + isSmoothingScrollFooterToNormalNow + ", no need todo this: isInDrag:" + mPullToRefreshLayoutProxy.isInDrag() + ",lastComputeY != 0?" + (lastY != 0) + ",isSmoothingScrollHeaderToNormalNow:" + isSmoothingScrollHeaderToNormalNow);
            }
        }
    }

    @Override
    public void smoothScrollHeaderToNormal() {
        if (mPullToRefreshLayoutProxy != null) {
            if (/*lastY != 0 &&*/ !isSmoothingScrollHeaderToNormalNow && !isSmoothingScrollFooterToNormalNow && !mPullToRefreshLayoutProxy.isInDrag()) {
                MLogUtil.d(TAG, "[smoothScrollHeaderToNormal true] smoothingNow?" + isSmoothingScrollHeaderToNormalNow + ", isInDrag:" + mPullToRefreshLayoutProxy.isInDrag() + ",lastComputeY != 0?" + (lastY != 0) + ",isSmoothingScrollFooterToNormalNow:" + isSmoothingScrollFooterToNormalNow);
                mPullToRefreshLayoutProxy.smoothScrollHeaderToNormal();
                isSmoothingScrollHeaderToNormalNow = true;
            } else {
                MLogUtil.d(TAG, "[smoothScrollHeaderToNormal false] smoothingNow?" + isSmoothingScrollHeaderToNormalNow + ", no need todo this: isInDrag:" + mPullToRefreshLayoutProxy.isInDrag() + ",lastComputeY != 0?" + (lastY != 0) + ",isSmoothingScrollFooterToNormalNow:" + isSmoothingScrollFooterToNormalNow);
            }
        }
    }

    @Override
    public void smoothScrollFooterToLoading() {
        if (mPullToRefreshLayoutProxy != null && lastY != getLoadMoreViewHeight() && !isSmoothingScrollLoadMoreToLoadingNow && !mPullToRefreshLayoutProxy.isInDrag()) {
            MLogUtil.d(TAG, "[smoothScrollFooterToLoading] true");
            mPullToRefreshLayoutProxy.smoothScrollFooterToLoading();
            isSmoothingScrollLoadMoreToLoadingNow = true;
        } else {
            MLogUtil.d(TAG, "[smoothScrollFooterToLoading] false, no need todo this");
        }
    }

    public boolean isAutoLoadMore() {
        return isAutoLoadMore;
    }

    public void setAutoLoadMore(boolean autoLoadMore) {
        isAutoLoadMore = autoLoadMore;
    }

    @Override
    public int getHeaderViewHeight() {
        //MLogUtil.d(TAG, "[getHeaderViewHeight]:" + (headerView == null ? 0 : headerView.getMeasuredHeight()));
        return headerView == null ? 0 : headerView.getMeasuredHeight();
    }

    @Override
    public int getLoadMoreViewHeight() {
        //MLogUtil.d(TAG, "[getLoadMoreViewHeight]:" + (footerView == null ? 0 : footerView.getMeasuredHeight()));
        return footerView == null ? 0 : footerView.getMeasuredHeight();
    }

    @Override
    public int getDurationHeaderToRefreshing() {
        return durationHeaderToRefreshing;
    }

    public void setDurationHeaderToRefreshing(int durationHeaderToRefreshing) {
        this.durationHeaderToRefreshing = durationHeaderToRefreshing;
    }

    @Override
    public int getDurationHeaderToNormal() {
        return durationHeaderToNormal;
    }

    public void setDurationHeaderToNormal(int durationHeaderToNormal) {
        this.durationHeaderToNormal = durationHeaderToNormal;
    }

    @Override
    public int getDurationFooterToLoading() {
        return durationFooterToLoading;
    }

    public void setDurationFooterToLoading(int durationFooterToLoading) {
        this.durationFooterToLoading = durationFooterToLoading;
    }

    @Override
    public int getDurationFooterToNormal() {
        return durationFooterToNormal;
    }

    public void setDurationFooterToNormal(int durationFooterToNormal) {
        this.durationFooterToNormal = durationFooterToNormal;
    }

    public boolean isEnableTouchWhileRefreshingOrLoading() {
        return enableTouchWhileRefreshingOrLoading;
    }

    public void setEnableTouchWhileRefreshingOrLoading(boolean enableTouchWhileRefreshingOrLoading) {
        this.enableTouchWhileRefreshingOrLoading = enableTouchWhileRefreshingOrLoading;
    }

    public int getPullMode() {
        return pullMode;
    }

    public boolean isEnablePullHeaderToRefresh() {
        return pullMode == MODE_BOTH || pullMode == MODE_HEADER;
    }

    public boolean isEnablePullFooterToLoading() {
        return pullMode == MODE_BOTH || pullMode == MODE_FOOTER;
    }

    public void setPullMode(int pullMode) {
        this.pullMode = pullMode;
        if (headerView != null)
            headerView.setVisibility(isEnablePullHeaderToRefresh() ? VISIBLE : INVISIBLE);
        if (footerView != null)
            footerView.setVisibility(isEnablePullFooterToLoading() ? VISIBLE : INVISIBLE);
    }

    //==============================================================================================
    //static
    //==============================================================================================
    public static int getGlobalDurationHeaderToRefreshing() {
        return globalDurationHeaderToRefreshing;
    }

    public static void setGlobalDurationHeaderToRefreshing(int globalDurationHeaderToRefreshing) {
        MPullToRefreshLayout.globalDurationHeaderToRefreshing = globalDurationHeaderToRefreshing;
    }

    public static int getGlobalDurationHeaderToNormal() {
        return globalDurationHeaderToNormal;
    }

    public static void setGlobalDurationHeaderToNormal(int globalDurationHeaderToNormal) {
        MPullToRefreshLayout.globalDurationHeaderToNormal = globalDurationHeaderToNormal;
    }

    public static int getGlobalDurationFooterToLoading() {
        return globalDurationFooterToLoading;
    }

    public static void setGlobalDurationFooterToLoading(int globalDurationFooterToLoading) {
        MPullToRefreshLayout.globalDurationFooterToLoading = globalDurationFooterToLoading;
    }

    public static int getGlobalDurationFooterToNormal() {
        return globalDurationFooterToNormal;
    }

    public static void setGlobalDurationFooterToNormal(int globalDurationFooterToNormal) {
        MPullToRefreshLayout.globalDurationFooterToNormal = globalDurationFooterToNormal;
    }

    public static int getGlobalPullBackgroundColor() {
        return globalPullBackgroundColor;
    }

    public static void setGlobalPullBackgroundColor(@ColorInt int globalPullBackgroundColor) {
        MPullToRefreshLayout.globalPullBackgroundColor = globalPullBackgroundColor;
    }

    public static int getGlobalPullTextColor() {
        return globalPullTextColor;
    }

    public static void setGlobalPullTextColor(@ColorInt int globalPullTextColor) {
        MPullToRefreshLayout.globalPullTextColor = globalPullTextColor;
    }
}