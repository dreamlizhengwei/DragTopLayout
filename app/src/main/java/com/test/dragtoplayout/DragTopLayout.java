package com.test.dragtoplayout;

import android.content.Context;
import android.graphics.PointF;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.Scroller;

public class DragTopLayout extends FrameLayout {

    public DragTopLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public DragTopLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragTopLayout(Context context) {
        this(context, null);
    }

    private void init() {
        mScroller = new Scroller(getContext().getApplicationContext());
    }

    /**
     * 当前头部可见还是滚出屏幕，true为滚出屏幕
     */
    private boolean mCollapsed = false;
    /**
     * 头部高度
     */
    private int mHeadHeight;

    /**
     * 滚动辅助
     */
    private Scroller mScroller;

    /**
     * 速度计算,每次用完要recycle
     */
    private VelocityTracker velocityTracker;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        mHeadHeight = getChildAt(0).getMeasuredHeight();

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        View v1 = getChildAt(0);
        v1.layout(0, 0, v1.getMeasuredWidth(), v1.getMeasuredHeight());

        View v2 = getChildAt(1);
        v2.layout(0, v1.getMeasuredHeight(), getMeasuredWidth(),
                getMeasuredHeight() + mHeadHeight);
    }


    /**
     * 按下点，滑动过程中的上一个点
     */
    private PointF mDownPoint = new PointF();

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownPoint.set(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                float y = event.getY();
                // Y方向需要scrollBy的距离，正值表示需要向上滚动
                float scrollByDelt = -y + mDownPoint.y;
                // 假如按照计算的距离偏移后的偏移量，即getScrollY()的值
                float realDelt = getScrollY() + scrollByDelt;
                if (realDelt < 0) { // 表示按照实际的手指滑动距离向下移动的话太大，则直接计算出刚好头部显示出来的realDelt
                    scrollByDelt = 0 - getScrollY();
                } else if (realDelt > mHeadHeight) { // 同样表示实际距离太大，计算出合适的距离
                    scrollByDelt = mHeadHeight - getScrollY();
                }
                scrollBy(0, (int) scrollByDelt);
                mDownPoint.set(event.getX(), y);
                break;
            case MotionEvent.ACTION_UP:
                mDownPoint.set(0, 0);
                checkPosition();
                break;
        }
        return true;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        Log.e("lzw", "onInterceptTouchEvent canChildScrollUp " + canChildScrollUp());
        if (canChildScrollUp()) { // 能向上滚动，一定是滚动view处理事件
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownPoint.set(event.getX(), event.getY());
                return false;
            case MotionEvent.ACTION_MOVE:
                // 横向滚动大于纵向，也不响应
                if (Math.abs(event.getX() - mDownPoint.x) > Math.abs(event.getY()
                        - mDownPoint.y)) {
                    mDownPoint.set(event.getX(), event.getY());
                    return super.onInterceptTouchEvent(event);
                }
                // 在向下移动
                if (event.getY() > mDownPoint.y) {
                    mDownPoint.set(event.getX(), event.getY());
                    if (mCollapsed) { // 头部不可见了，向下滚动需要拦截
                        return true;
                    } else {
                        return super.onInterceptTouchEvent(event);
                    }
                }
                // 在向上移动
                if (event.getY() < mDownPoint.y) {
                    mDownPoint.set(event.getX(), event.getY());
                    if (mCollapsed) { // 头部滚出屏幕，不拦截
                        return super.onInterceptTouchEvent(event);
                    } else {
                        return true;
                    }
                }
                mDownPoint.set(event.getX(), event.getY());
            case MotionEvent.ACTION_UP:
                // 检查头部是否移除去
                mCollapsed = getScrollY() >= mHeadHeight;
                mDownPoint.set(event.getX(), event.getY());
                return super.onInterceptTouchEvent(event);
        }
        return true;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((android.os.Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView)
                || (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     *         scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView
                        .getChildAt(0).getTop() < absListView
                        .getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTarget, -1)
                        || mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    /**
     * 检查是否需要关闭或者打开
     */
    private void checkPosition() {
        // 移出去的大小,不能直接在if里面用，否则返回值不正确
        int opOffset = getScrollY();
        if (opOffset < (mHeadHeight / 2)) {
            open();
        } else {
            close();
        }
    }

    /**
     * 向上移动，隐藏头部
     */
    private void close() {
        mCollapsed = true;
        mScroller.startScroll(0, getScrollY(), 0, mHeadHeight - getScrollY());
        invalidate();
    }

    /**
     * 向下移动,头部出现
     */
    private void open() {
        mCollapsed = false;
        mScroller.startScroll(0, getScrollY(), 0, -getScrollY());
        invalidate();
    }

    @Override
    public void computeScroll() {
        // 返回值为boolean，true说明滚动尚未完成，false说明滚动已经完成。
        if (mScroller.computeScrollOffset()) {
            // 移动位置
            scrollTo(0, mScroller.getCurrY());
            invalidate();
        }
    }

    /**
     * 可滚动view
     */
    private View mTarget;

    /**
     * 设置可滚动view
     */
    public void setTargetView(View v) {
        mTarget = v;
    }

}
