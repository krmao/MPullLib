package com.mlibrary.widget.pull;

public interface IPullToRefresh {

    void smoothScrollHeaderToRefreshing();

    void smoothScrollFooterToLoading();

    void smoothScrollHeaderToNormal();

    void smoothScrollFooterToNormal();

    boolean isRefreshing();

    boolean isLoading();

    boolean isEnableTouchWhileRefreshingOrLoading();

    int getHeaderViewHeight();

    int getLoadMoreViewHeight();

    int getDurationFooterToNormal();

    int getDurationHeaderToNormal();

    int getDurationFooterToLoading();

    int getDurationHeaderToRefreshing();

}