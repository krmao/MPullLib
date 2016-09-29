package com.mlibrary.widget.pull.header;

import android.widget.TextView;

public interface MIPullHandler {
    /**
     * 下拉刷新
     */
    TextView getTitleTextView();

    /**
     * 最近更新: 13:56
     */
    TextView getTimeTextView();

    /**
     * 刷新成功
     */
    TextView getResultTextView();
}
