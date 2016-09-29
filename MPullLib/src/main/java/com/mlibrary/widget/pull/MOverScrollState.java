package com.mlibrary.widget.pull;

public enum MOverScrollState {
    HEADER_NORMAL("下拉刷新"),//准备下来刷新 or 完全释放 重置状态
    HEADER_REFRESHING("正在刷新..."),//正在刷新
    HEADER_REFRESH_SUCCESS("刷新成功"),//刷新成功 尚未完全释放
    HEADER_REFRESH_FAILURE("刷新失败"),//刷新失败 尚未完全释放
    HEADER_READY_TO_RELEASE("释放立即刷新"),//准备松开刷新

    FOOTER_NORMAL("上拉加载更多"),//准备加载更多
    FOOTER_LOADING("正在加载"),//正在加载
    FOOTER_READY_TO_RELEASE("释放立即加载"),//准备释放加载更多
    FOOTER_LOAD_FAILURE("加载失败"),//加载失败
    FOOTER_LOAD_SUCCESS("加载成功"),//加载成功
    FOOTER_NO_MORE_DATA("没有更多数据了");//全部加载完毕

    private String text;
    private boolean isRefreshCompletely = true;
    private boolean isLoadCompletely = true;

    public String getText() {
        return text;
    }

    public boolean isRefreshCompletely() {
        return isRefreshCompletely;
    }

    public void setRefreshCompletely(boolean refreshCompletely) {
        isRefreshCompletely = refreshCompletely;
    }

    public boolean isLoadCompletely() {
        return isLoadCompletely;
    }

    public void setLoadCompletely(boolean loadCompletely) {
        isLoadCompletely = loadCompletely;
    }

    MOverScrollState(String text) {
        this.text = text;
    }
}
