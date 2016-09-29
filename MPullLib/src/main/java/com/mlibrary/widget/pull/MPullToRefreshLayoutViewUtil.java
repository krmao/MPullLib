package com.mlibrary.widget.pull;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.AbsListView;

import com.mlibrary.widget.pull.header.MIPullHandler;

@SuppressWarnings({"deprecation", "unused"})
public class MPullToRefreshLayoutViewUtil {
    public static void initAttrsBackground(TypedArray typedArray, View view, int index, @ColorInt int defaultColor) {
        try {
            if (defaultColor != -1)
                view.setBackgroundColor(defaultColor);

            Drawable drawable = typedArray.getDrawable(index);
            if (drawable != null) {
                view.setBackgroundDrawable(drawable);
            } else {
                int color = typedArray.getColor(index, Integer.MAX_VALUE);
                if (color != Integer.MAX_VALUE) {
                    view.setBackgroundColor(color);
                } else {
                    String colorStr = typedArray.getString(index);
                    if (!TextUtils.isEmpty(colorStr)) {
                        try {
                            view.setBackgroundColor(Color.parseColor(colorStr));
                        } catch (Exception ignored) {
                        }
                    } else {
                        int resId = typedArray.getResourceId(index, -1);
                        if (resId != -1) {
                            view.setBackgroundResource(resId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initAttrsTextColor(TypedArray typedArray, MIPullHandler pullHandler, int index, @ColorInt int defaultColor) {
        try {
            if (defaultColor != -1) {
                pullHandler.getTitleTextView().setTextColor(defaultColor);
                pullHandler.getTimeTextView().setTextColor(defaultColor);
                pullHandler.getResultTextView().setTextColor(defaultColor);
            }

            int color = typedArray.getColor(index, Integer.MAX_VALUE);
            if (color != Integer.MAX_VALUE) {
                pullHandler.getTitleTextView().setTextColor(color);
                pullHandler.getTimeTextView().setTextColor(color);
                pullHandler.getResultTextView().setTextColor(color);
            } else {
                String colorStr = typedArray.getString(index);
                if (!TextUtils.isEmpty(colorStr)) {
                    try {
                        pullHandler.getTitleTextView().setTextColor(Color.parseColor(colorStr));
                        pullHandler.getTimeTextView().setTextColor(Color.parseColor(colorStr));
                        pullHandler.getResultTextView().setTextColor(Color.parseColor(colorStr));
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean canScrollDownToUp(View view) {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (view instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) view;
                return absListView.getChildCount() > 0
                        && (absListView.getLastVisiblePosition() < absListView.getChildCount() - 1
                        || absListView.getChildAt(absListView.getChildCount() - 1).getBottom() > absListView.getHeight() - absListView.getPaddingBottom());
            }
        }
        return ViewCompat.canScrollVertically(view, 1);
    }

    public static boolean canScrollUpToDown(View view) {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (view instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) view;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            }
        }
        return ViewCompat.canScrollVertically(view, -1);
    }

    public static boolean canScrollLeftToRight(View view) {
        return ViewCompat.canScrollHorizontally(view, -1);
    }

    public static boolean canScrollRightToLeft(View view) {
        return ViewCompat.canScrollHorizontally(view, 1);
    }

}
