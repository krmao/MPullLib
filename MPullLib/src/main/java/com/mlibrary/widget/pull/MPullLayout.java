package com.mlibrary.widget.pull;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Scroller;

@SuppressWarnings("unused")
public class MPullLayout extends RelativeLayout {
    protected String TAG = getClass().getSimpleName();

    protected MPullToRefreshLayoutProxy pullLayoutProxy;//代理
    protected float baseOverScrollLength;
    protected View childTopView;
    protected Scroller scroller;
    protected float oldX;
    protected float oldY;
    protected float actionDownX;
    protected float actionDownY;
    protected int dealtX;
    protected int dealtY;

    protected boolean isInDrag = false;
    protected boolean isAnimationNotFinished = false;

    /*protected boolean canChildScrollHorizontally = true;
    protected boolean canChildScrollVertical = true;*/

    //拦截重复数值设置 start =========================================================================
    protected boolean isLastInDrag = false;
    protected int lastComputeX = 0;
    protected int lastComputeY = 0;
    //拦截重复数值设置 end ===========================================================================

    //检测是否正在越界滚动,如果不是则ACTION_DOWN 不要拦截,否则,无法股东子view,顾设置此值
    protected boolean isOverScrollingBottomNow = false;
    protected boolean isOverScrollingRightNow = false;
    protected boolean isOverScrollingLeftNow = false;
    protected boolean isOverScrollingTopNow = false;

    //可外部配置的开关 start =========================================================================
    //xml layout config start ======================================================================
    /*<attr name="overScrollMode" format="enum">
        <enum name="both" value="0" />
        <enum name="header" value="1" />
        <enum name="footer" value="2" />
        <enum name="none" value="3" />
    </attr>*/
    public static final int MODE_BOTH = 0;
    public static final int MODE_HEADER = 1;
    public static final int MODE_FOOTER = 2;
    public static final int MODE_NONE = 3;
    protected int overScrollMode = MODE_BOTH;
    protected int orientation = LinearLayout.VERTICAL;

    @Override
    public int getOverScrollMode() {
        return overScrollMode;
    }

    @Override
    public void setOverScrollMode(int overScrollMode) {
        this.overScrollMode = overScrollMode;
    }

    //可外部配置的开关 end ===========================================================================

    protected static int globalDurationOverScrollingToNormal = 800;
    protected int durationOverScrollingToNormal = globalDurationOverScrollingToNormal;

    public void setDebugAble(boolean isDebug) {
        MLogUtil.setDebugAble(isDebug);
    }

    public MPullLayout(Context context) {
        this(context, null);
    }

    public MPullLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MPullLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //xml layout config start ==================================================================
        //typedArray 只有在构造函数里面才有值,怪事,所以预先初始化 headerView and footerView 因为 background是多样性的,既可以是色值,也可以是图片,直接复制比较方便
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MPullLayout);
        overScrollMode = typedArray.getInt(R.styleable.MPullLayout_overScrollMode, overScrollMode);
        orientation = typedArray.getInt(R.styleable.MPullLayout_orientation, orientation);
        durationOverScrollingToNormal = typedArray.getInt(R.styleable.MPullLayout_durationOverScrollingToNormal, durationOverScrollingToNormal);
        typedArray.recycle();
        //xml layout config end ====================================================================
        scroller = new Scroller(getContext(), new OvershootInterpolator(0.75f));
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeGlobalOnLayoutListener(this);
                //checkCanChildScroll();
                //baseOverScrollLength = canChildScrollVertical ? getMeasuredHeight() : getMeasuredWidth();
                baseOverScrollLength = getMeasuredHeight();
            }
        });
    }

    //检测可以滚动的方向,以及获取真实的宽高
    /*protected void checkCanChildScroll() {
        if (childTopView instanceof AbsListView || childTopView instanceof ScrollView || childTopView instanceof WebView) {
            canChildScrollHorizontally = false;
            canChildScrollVertical = true;
        } else if (childTopView instanceof RecyclerView) {
            RecyclerView.LayoutManager layoutManager = ((RecyclerView) childTopView).getLayoutManager();
            int orientation = -1;
            if (layoutManager instanceof StaggeredGridLayoutManager)
                orientation = ((StaggeredGridLayoutManager) layoutManager).getOrientation();
            else if (layoutManager instanceof LinearLayoutManager)
                orientation = ((LinearLayoutManager) layoutManager).getOrientation();
            canChildScrollHorizontally = RecyclerView.HORIZONTAL == orientation;
            canChildScrollVertical = RecyclerView.VERTICAL == orientation;
        } else if (childTopView instanceof HorizontalScrollView) {
            canChildScrollHorizontally = true;
            canChildScrollVertical = false;
        } else if (childTopView instanceof ViewPager) {
            canChildScrollHorizontally = false;
            canChildScrollVertical = false;
        } else {
            canChildScrollHorizontally = false;
            canChildScrollVertical = true;
        }
    }*/

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (childTopView == null && getChildCount() > 0) {
            childTopView = getChildAt(getChildCount() - 1);
            childTopView.setOverScrollMode(OVER_SCROLL_NEVER);
        }

        if (pullLayoutProxy == null && getParent() != null && getParent() instanceof IPullToRefresh) {
            pullLayoutProxy = new MPullToRefreshLayoutProxy(this);
            pullLayoutProxy.setPullLayout((IPullToRefresh) getParent());
            MLogUtil.d(TAG, "[MOverScrollLayout:onLayout] pullLayoutProxy.setPullLayout((IPullToRefresh) getParent());");
        }
    }

    /*
    dispatchTouchEvent()返回true，后续事件（ACTION_MOVE、ACTION_UP）会再传递，
    如果返回false，dispatchTouchEvent()就接收不到ACTION_UP、ACTION_MOVE
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (childTopView != null && overScrollMode != MODE_NONE) {
            //在刷新的时候设置是否可以继续滚动
            if (pullLayoutProxy != null && !pullLayoutProxy.isEnableScrollOnRefreshing())
                if (pullLayoutProxy.isRefreshing() || pullLayoutProxy.isLoading())
                    return false;

            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldX = oldY = 0;
                    MLogUtil.d(TAG, "ACTION_POINTER_DOWN");
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    MLogUtil.d(TAG, "ACTION_POINTER_UP");
                    oldX = oldY = 0;
                    break;
                case MotionEvent.ACTION_DOWN:
                    MLogUtil.w(TAG, "ACTION_DOWN");
                    setInDrag(true);

                    if (pullLayoutProxy != null)
                        pullLayoutProxy.onActionDown();

                    actionDownX = motionEvent.getX();
                    actionDownY = motionEvent.getY();
                    oldX = motionEvent.getX();
                    dealtX = scroller.getCurrX();
                    oldY = motionEvent.getY();
                    dealtY = scroller.getCurrY();

                    if ((orientation == LinearLayout.VERTICAL && dealtY != 0) || (orientation == LinearLayout.HORIZONTAL && dealtX != 0))
                        forceFinished();

                    MLogUtil.d(TAG, "ACTION_DOWN:"
                            + ",\ngetCurrentY:" + getCurrentY()
                            + ",\ngetFinalY:" + getFinalY()
                            + ",\ndealtY:" + dealtY
                            + ",\ndealtX:" + dealtX
                            + ",\noldX:" + oldX
                            + ",\noldY:" + oldY
                            + ",\noverScrollMode:" + (overScrollMode == MODE_BOTH ? "MODE_BOTH" : (overScrollMode == MODE_NONE ? "MODE_NONE" :
                            (overScrollMode == MODE_HEADER ? "MODE_HEADER" : (overScrollMode == MODE_FOOTER ? "MODE_FOOTER" : "未知"))))
                            + ",\norientation:" + (orientation == LinearLayout.VERTICAL ? "VERTICAL" : "HORIZONTAL")
                            + "\nmotionEvent.getY():" + motionEvent.getY()
                    );

                    //如果符合越界拉动条件,则拦截自己处理
                    if (overScrollMode != MODE_NONE) {
                        if (orientation == LinearLayout.VERTICAL) {
                            if (isOverScrollingBottomNow() || isOverScrollingTopNow()) {
                                MLogUtil.w(TAG, "ACTION_DOWN:正在垂直越界 垂直  滚动 return true");
                                return true;
                            }
                        } else {
                            if (isOverScrollingLeftNow() || isOverScrollingRightNow()) {
                                MLogUtil.w(TAG, "ACTION_DOWN:正在水平越界 水平  滚动 return true");
                                return true;
                            }
                        }
                    }
                    MLogUtil.v(TAG, "ACTION_DOWN: return false,overScrollMode:" + overScrollMode + ",orientation:" + orientation);
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (orientation == LinearLayout.VERTICAL && oldY == 0 || orientation == LinearLayout.HORIZONTAL && oldX == 0) {//当第二个手指按下时 oldY=oldX=0,导致dealtY重大偏移(相差的值正好是oldY 与新的 oldY的差值),此时return ,下一次使用新的oldY计算偏移,就不会出现跳跃式的现象!
                        MLogUtil.w(TAG, "ACTION_MOVE: oldY==0 ||  oldX==0 return true");
                        oldX = motionEvent.getX();
                        oldY = motionEvent.getY();
                        return true;
                    }
                    boolean isVerticalOffsetBigger = Math.abs(motionEvent.getY() - actionDownY) > Math.abs(motionEvent.getX() - actionDownX);
                    if (orientation == LinearLayout.VERTICAL && !isVerticalOffsetBigger || orientation == LinearLayout.HORIZONTAL && isVerticalOffsetBigger) {
                        setOverScrollingTopNow(false);
                        setOverScrollingBottomNow(false);
                        MLogUtil.v(TAG, "ACTION_MOVE,事件冲突,拦截,isVerticalOffsetBigger:" + isVerticalOffsetBigger +
                                "\nactionDownY:" + actionDownY +
                                "\nactionDownX:" + actionDownX +
                                "\noldX:" + oldX +
                                "\ncurX:" + motionEvent.getX() +
                                "\noldY:" + oldY +
                                "\ncurY:" + motionEvent.getY() +
                                "\nMath.abs(motionEvent.getX() - actionDownX):" + (Math.abs(motionEvent.getX() - actionDownX)) +
                                "\nMath.abs(motionEvent.getY() - actionDownY):" + (Math.abs(motionEvent.getY() - actionDownY))
                        );
                        /*if (childTopView != null) {//loading 结束按住不放无法继续滚动的bug
                            MotionEvent tmpEvent0 = MotionEvent.obtain(motionEvent);
                            tmpEvent0.setAction(MotionEvent.ACTION_CANCEL);
                            childTopView.onTouchEvent(tmpEvent0);
                            tmpEvent0.recycle();
                        }*/
                        return super.dispatchTouchEvent(motionEvent);
                    }

                    int tmpDealtX = (int) (dealtX + getDealt(oldX - motionEvent.getX(), dealtX));
                    int tmpDealtY = (int) (dealtY + getDealt(oldY - motionEvent.getY(), dealtY));
                    boolean canPullLayoutPullUpToDown = tmpDealtY < 0 && !canChildScrollUpToDown();
                    boolean canPullLayoutPullDownToUp = tmpDealtY > 0 && !canChildScrollDownToUp();
                    MLogUtil.w(TAG, "ACTION_MOVE:"
                            + "motionEvent.getY():" + motionEvent.getY()
                            + ",\ngetCurrentY:" + getCurrentY()
                            + ",\ncanChildScrollUpToDown():" + canChildScrollUpToDown()
                            + ",\ncanChildScrollDownToUp():" + canChildScrollDownToUp()
                            + ",\ncanPullLayoutPullUpToDown:" + canPullLayoutPullUpToDown
                            + ",\ncanPullLayoutPullDownToUp:" + canPullLayoutPullDownToUp
                            + ",\noverScrollMode:" + (overScrollMode == MODE_BOTH ? "MODE_BOTH" : (overScrollMode == MODE_NONE ? "MODE_NONE" :
                            (overScrollMode == MODE_HEADER ? "MODE_HEADER" : (overScrollMode == MODE_FOOTER ? "MODE_FOOTER" : "未知"))))
                            + ",\norientation:" + (orientation == LinearLayout.VERTICAL ? "VERTICAL" : "HORIZONTAL")
                            + ",\ntmpDealtY:" + tmpDealtY
                    );
                    if (overScrollMode != MODE_NONE) {
                        //子view自己无法再滚动,则触发越界滚动
                        if (canPullLayoutPullUpToDown || canPullLayoutPullDownToUp) {
                            dealtX = tmpDealtX;
                            dealtY = tmpDealtY;
                            oldY = motionEvent.getY();
                            if (childTopView != null) {//loading 结束按住不放无法继续滚动的bug,一并解决 拖拽过程中触发 longClick event
                                MotionEvent tmpEvent = MotionEvent.obtain(motionEvent);
                                tmpEvent.setAction(MotionEvent.ACTION_CANCEL);
                                childTopView.dispatchTouchEvent(tmpEvent);
                                tmpEvent.recycle();
                            }
                            if (orientation == LinearLayout.VERTICAL)
                                smoothScrollTo(0, dealtY, 0);//当手指松开,快速点击时候,出现跳帧现象,因为ActionUp 会回到0点, finalY 已经是0了,那么执行滑动,会出现跳帧
                            else
                                smoothScrollTo(dealtX, 0, 0);

                            if ((isOverScrollingTopNow() && dealtY == 0 && !isOverScrollingBottomNow()) || (isOverScrollingBottomNow() && dealtY == 0 && !isOverScrollingTopNow())) {
                                oldY = 0;
                                setOverScrollingTopNow(false);
                                setOverScrollingBottomNow(false);
                                return (!canChildScrollUpToDown() && !canChildScrollDownToUp()) || super.dispatchTouchEvent(resetVertical(motionEvent));
                            }
                            return true;
                        } else {
                            if (getFinalY() != 0) {
                                smoothScrollByFinalXY(0, 0, 0);
                                MLogUtil.v(TAG, "ACTION_MOVE-selfScroll:getFinalY() != 0 smoothScrollByFinalXY");
                                return true;
                            }
                            if (getCurrentY() != 0) {
                                smoothScrollByCurrentXY(0, 0, 0);
                                MLogUtil.v(TAG, "ACTION_MOVE-selfScroll:getCurrentY() != 0 smoothScrollByCurrentXY");
                                return true;
                            }
                            oldX = oldY = dealtX = dealtY = 0;//重置越界滚动
                            setOverScrollingTopNow(false);
                            setOverScrollingBottomNow(false);
                            //setOverScrollingLeftNow(false);
                            //setOverScrollingRightNow(false);
                            MLogUtil.v(TAG, "ACTION_MOVE-selfScroll: oldY==" + oldY + ",getCurrentY:" + getCurrentY() + ",getFinalY:" + getFinalY());
                            //motionEvent.setAction(MotionEvent.ACTION_CANCEL);//强制退出本次Action序列 设置此值,则必须重新按下才行
                            if (childTopView != null) {//loading 结束按住不放无法继续滚动的bug
                                MotionEvent tmpEvent0 = MotionEvent.obtain(motionEvent);
                                tmpEvent0.setAction(MotionEvent.ACTION_MOVE);
                                /*todo
                                Process: com.mtemplate, PID: 8129
                                                 java.lang.IndexOutOfBoundsException: Inconsistency detected. Invalid view holder adapter positionViewHolder{453caf88 position=9 id=-1, oldPos=-1, pLpos:-1 no parent}
                                                     at android.support.v7.widget.RecyclerView$Recycler.validateViewHolderForOffsetPosition(RecyclerView.java:4801)
                                                     at android.support.v7.widget.RecyclerView$Recycler.getViewForPosition(RecyclerView.java:4932)
                                                     at android.support.v7.widget.RecyclerView$Recycler.getViewForPosition(RecyclerView.java:4913)
                                                     at android.support.v7.widget.LinearLayoutManager$LayoutState.next(LinearLayoutManager.java:2029)
                                                     at android.support.v7.widget.LinearLayoutManager.layoutChunk(LinearLayoutManager.java:1414)
                                                     at android.support.v7.widget.LinearLayoutManager.fill(LinearLayoutManager.java:1377)
                                                     at android.support.v7.widget.LinearLayoutManager.scrollBy(LinearLayoutManager.java:1193)
                                                     at android.support.v7.widget.LinearLayoutManager.scrollVerticallyBy(LinearLayoutManager.java:1043)
                                                     at android.support.v7.widget.RecyclerView.scrollByInternal(RecyclerView.java:1552)
                                                     at android.support.v7.widget.RecyclerView.onTouchEvent(RecyclerView.java:2649)
                                                     at android.view.View.dispatchTouchEvent(View.java:7723)
                                                     at android.view.ViewGroup.dispatchTransformedTouchEvent(ViewGroup.java:2212)
                                                     at android.view.ViewGroup.dispatchTouchEvent(ViewGroup.java:1945)
                                                     at com.mlibrary.widget.pull.MPullLayout.dispatchTouchEvent(MPullLayout.java:302)*/
                                childTopView.dispatchTouchEvent(tmpEvent0);//
                                tmpEvent0.recycle();
                            }
                            return false;
                        }
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    MLogUtil.w(TAG, "ACTION_UP");
                    dealtY = dealtX = 0;
                    if (pullLayoutProxy != null) {
                        pullLayoutProxy.onActionUp();
                    }
                    if (Math.abs(scroller.getCurrY()) > Math.abs(scroller.getFinalY()) && scroller.getFinalY() == 0)
                        forceFinished();
                    else
                        abortAnimation();
                    setInDrag(false);
                    if (!isEnabledPullRefresh()) {
                        smoothScrollTo(0, 0, getDurationOverScrollingToNormal());
                    } else {
                        //这么写只因为非常快速的滑动,currentY < headerViewHeight 而实际已经大于它,
                        // 调用 computeScroll 使用的是 currentY,所以存在bug,直接滚动到normal 没有刷新了,所以这里强制调用 onOverScroll,以及 返回的currentY出现跳帧现象,顾传递比较大的值
                        if (pullLayoutProxy != null)
                            pullLayoutProxy.onComputeScroll(
                                    Math.abs(getCurrentX()) > Math.abs(getFinalX()) ? getCurrentX() : getFinalX(),
                                    Math.abs(getCurrentY()) > Math.abs(getFinalY()) ? getCurrentY() : getFinalY());
//                        invalidate();
                    }
                    if (isOverScrollingNow()) {//免得越界滑动过程中,触发点击事件
                        MLogUtil.d(TAG, "isOverScrollingNow==true:return true");
                        return true;
                    }
                    break;
            }
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    //是否正在越界滚动,非正常状态
    protected boolean isOverScrollingNow() {
        return getCurrentX() != 0 || getFinalX() != 0 || getCurrentY() != 0 || getFinalY() != 0;
    }

    protected int animationFinishedNum = 0;

    @Override
    public void computeScroll() {
        isAnimationNotFinished = scroller.computeScrollOffset();
        int currentX = scroller.getCurrX();
        int currentY = scroller.getCurrY();
        if (isAnimationNotFinished) {
            animationFinishedNum = 0;
            postInvalidate();
            //重复 return
            if (lastComputeX == currentX && lastComputeY == currentY && isLastInDrag == isInDrag) {
                MLogUtil.v(TAG, "[computeScroll 动画未结束 重复] currentX:" + currentX + ", currentY:" + currentY + ", finalX:" + getFinalX() + ", finalY:" + getFinalY());
                return;
            }
            MLogUtil.v(TAG, "[computeScroll 动画未结束] currentX:" + currentX + ", currentY:" + currentY + ", finalX:" + getFinalX() + ", finalY:" + getFinalY());
            scrollTo(currentX, currentY);
        } else {
            if (lastComputeX == currentX && lastComputeY == currentY && isLastInDrag == isInDrag) {
                MLogUtil.v(TAG, "[computeScroll 动画已结束 重复] currentX:" + currentX + ", currentY:" + currentY + ", finalX:" + getFinalX() + ", finalY:" + getFinalY());
                animationFinishedNum++;
                if (animationFinishedNum <= 3)
                    postInvalidate();
                return;
            }
            animationFinishedNum = 0;
            postInvalidate();
            setOverScrollingTopNow(false);
            setOverScrollingBottomNow(false);
            if (isInDrag() && (getCurrentY() != 0 || getFinalY() != 0)) {
                setOverScrollingTopNow(true);
                setOverScrollingBottomNow(true);
            }
            MLogUtil.v(TAG, "[computeScroll 动画已结束] currentX:" + currentX + ", currentY:" + currentY + ", finalX:" + getFinalX() + ", finalY:" + getFinalY());
        }
        lastComputeX = currentX;
        lastComputeY = currentY;
        isLastInDrag = isInDrag;

        //其它处理
        if (pullLayoutProxy != null)
            pullLayoutProxy.onComputeScroll(currentX, currentY);
    }

    //process scroller start =======================================================================
    //x y 停留在 终点,并计算后刷新一次
    public void abortAnimation() {
        MLogUtil.d(TAG, "==>>>>> abortAnimation getCurrentY:" + getCurrentY() + ",getFinalY:" + getFinalY());
        scroller.abortAnimation();
        computeScroll();//强制刷新
        MLogUtil.d(TAG, "==<<<<< abortAnimation getCurrentY:" + getCurrentY() + ",getFinalY:" + getFinalY());
    }

    public void abortNormalAnimation() {
        scroller.abortAnimation();
    }

    //x y 停留在 当前位置,并计算后刷新一次
    public void forceFinished() {
        MLogUtil.d(TAG, "==>>>>> forceFinished getCurrentY:" + getCurrentY() + ",getFinalY:" + getFinalY());
        scroller.forceFinished(true);
        computeScroll();//强制刷新
        MLogUtil.d(TAG, "==<<<<< forceFinished getCurrentY:" + getCurrentY() + ",getFinalY:" + getFinalY());
    }

    public void smoothScrollTo(int destX, int destY, int duration) {
        if (Math.abs(getCurrentY()) < Math.abs(getFinalY()))//优化处理,防止出现跨度很大的跳帧现象
            smoothScrollByFinalXY(destX, destY, duration);
        else
            smoothScrollByCurrentXY(destX, destY, duration);
    }

    protected void smoothScrollByCurrentXY(int destX, int destY, int duration) {
        forceFinished();//强制停止动画
        int dx = destX - scroller.getCurrX();
        int dy = destY - scroller.getCurrY();
        if (dx == 0 && dy == 0) {
            return;
        }
        MLogUtil.v(TAG, "[smoothScrollByCurrentXY true  " + Thread.currentThread().getName() + "] getCurrX:" + scroller.getCurrX() + "getFinalX:" + scroller.getFinalX() + ", getCurrY:" + scroller.getCurrY() + ", getFinalY:" + scroller.getFinalY() + ", dx:" + dx + ", dy:" + dy + ", duration:" + duration);
        scroller.startScroll(scroller.getCurrX(), scroller.getCurrY(), dx, dy, duration);
        invalidate();//不然有时候静止不动
    }

    protected void smoothScrollByFinalXY(int destX, int destY, int duration) {
        forceFinished();//强制停止动画
        int dx = destX - scroller.getFinalX();
        int dy = destY - scroller.getFinalY();
        if (dx == 0 && dy == 0) {
            return;
        }
        MLogUtil.v(TAG, "[smoothScrollByFinalXY true  " + Thread.currentThread().getName() + "] getCurrX:" + scroller.getCurrX() + "getFinalX:" + scroller.getFinalX() + ", getCurrY:" + scroller.getCurrY() + ", getFinalY:" + scroller.getFinalY() + ", dx:" + dx + ", dy:" + dy + ", duration:" + duration);
        scroller.startScroll(scroller.getFinalX(), scroller.getFinalY(), dx, dy, duration);
        invalidate();
    }

    //process scroller end && getter setter start ==================================================
    public MPullToRefreshLayoutProxy getPullLayoutProxy() {
        return pullLayoutProxy;
    }

    public boolean isEnabledPullRefresh() {
        return pullLayoutProxy != null;
    }

    public boolean isAnimationNotFinished() {
        return isAnimationNotFinished;
    }

    public int getCurrentY() {
        return scroller.getCurrY();
    }

    public int getCurrentX() {
        return scroller.getCurrX();
    }

    public int getFinalY() {
        return scroller.getFinalY();
    }

    public int getFinalX() {
        return scroller.getFinalX();
    }

    public boolean isInDrag() {
        return isInDrag;
    }

    public void setInDrag(boolean inDrag) {
        isInDrag = inDrag;
        forceFinished();
    }

    //获取拉升处理后的位移(达到越拉越难拉动的效果)
    protected float getDealt(float dealt, float distance) {
        if (dealt * distance < 0)
            return dealt;
        //x 为0的时候 y 一直为0, 所以当x==0的时候,给一个0.1的最小值
        float x = (float) Math.min(Math.max(Math.abs(distance), 0.1) / Math.abs(baseOverScrollLength), 1);
        float y = Math.min(new AccelerateInterpolator(0.15f).getInterpolation(x), 1);
        return dealt * (1 - y);
    }

    protected MotionEvent resetVertical(MotionEvent event) {
        oldY = dealtY = 0;
        event.setAction(MotionEvent.ACTION_DOWN);
        super.dispatchTouchEvent(event);
        event.setAction(MotionEvent.ACTION_MOVE);
        return event;
    }

    public void setOverScrollingRightNow(boolean overScrollingRightNow) {
        isOverScrollingRightNow = overScrollingRightNow;
    }

    public void setOverScrollingLeftNow(boolean overScrollingLeftNow) {
        isOverScrollingLeftNow = overScrollingLeftNow;
    }

    public void setOverScrollingTopNow(boolean overScrollingTopNow) {
        isOverScrollingTopNow = overScrollingTopNow;
        MLogUtil.d(TAG, "setOverScrollingTopNow:" + overScrollingTopNow);
    }

    public void setOverScrollingBottomNow(boolean overScrollingBottomNow) {
        isOverScrollingBottomNow = overScrollingBottomNow;
        MLogUtil.d(TAG, "setOverScrollingBottomNow:" + overScrollingBottomNow);
    }

    public boolean isOverScrollingTopNow() {
        return isOverScrollingTopNow;
    }

    public boolean isOverScrollingLeftNow() {
        return isOverScrollingLeftNow;
    }

    public boolean isOverScrollingRightNow() {
        return isOverScrollingRightNow;
    }

    public boolean isOverScrollingBottomNow() {
        return isOverScrollingBottomNow;
    }

    public boolean canChildScrollLeftToRight() {
        return MPullToRefreshLayoutViewUtil.canScrollLeftToRight(childTopView);
    }

    public boolean canChildScrollRightToLeft() {
        return MPullToRefreshLayoutViewUtil.canScrollRightToLeft(childTopView);
    }


    public boolean canChildScrollUpToDown() {
        return MPullToRefreshLayoutViewUtil.canScrollUpToDown(childTopView);
    }

    public boolean canChildScrollDownToUp() {
        return MPullToRefreshLayoutViewUtil.canScrollDownToUp(childTopView);
    }

    public int getDurationOverScrollingToNormal() {
        return durationOverScrollingToNormal;
    }

    public void setDurationOverScrollingToNormal(int durationOverScrollingToNormal) {
        this.durationOverScrollingToNormal = durationOverScrollingToNormal;
    }

    //getter setter end ============================================================================

    public static int getGlobalDurationOverScrollingToNormal() {
        return globalDurationOverScrollingToNormal;
    }

    public static void setGlobalDurationOverScrollingToNormal(int globalDurationOverScrollingToNormal) {
        MPullLayout.globalDurationOverScrollingToNormal = globalDurationOverScrollingToNormal;
    }
}