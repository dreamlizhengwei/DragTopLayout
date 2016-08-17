package com.test.dragtoplayout;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author lzw
 */
public class ExpandableTextView extends LinearLayout implements
        View.OnClickListener {

    /**
     * 显示文案的textview
     */
    private TextView mTv;

    /**
     * 折叠展开按钮
     */
    private ImageButton mButton;

    /**
     * 会否初始化过，操作过展开关闭表示初始化过了，onmeasure就不需要在测量了;
     */
    private boolean hasInit = false;

    /**
     * 当前是关闭还是打开状态，true为已关闭
     */
    private boolean mCollapsed = true;

    /**
     * 关闭时textview的高度
     */
    private int mCollapsedHeight;

    /**
     * 打开时textview的高度
     */
    private int mExpandHeight;

    /**
     * 折叠时最大行数，默认2
     */
    private int mMaxCollapsedLines = 2;

    /**
     * 向下箭头，关闭时的箭头
     */
    private Drawable mExpandDrawable;

    /**
     * 向上箭头，展开时的箭头
     */
    private Drawable mCollapseDrawable;

    /**
     * 展开关闭动画时间
     */
    private int mAnimationDuration = 200;

    /**
     * 动画是否在执行
     */
    private boolean mAnimating;

    /**
     * 打开动画
     */
    private ValueAnimator open;
    /**
     * 关闭动画
     */
    private ValueAnimator close;

    /**
     * 动画结束listener
     */
    private AnimatorListener endListener = new AnimatorListener() {

        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            // 动画是否在进行的标志位重置
            mAnimating = false;

            if (mCollapsed) { // 当前标识为true，表示已关闭，动画结束应该是打开状态，所以设置为不限行数
                mTv.setMaxLines(Integer.MAX_VALUE);
            } else { // 限制行数
                mTv.setMaxLines(mMaxCollapsedLines);
            }

            // 改变打开状态标志位
            mCollapsed = !mCollapsed;

        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }
    };

    /**
     * 更新textview高度listener
     */
    private AnimatorUpdateListener updateListener = new AnimatorUpdateListener() {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int height = (Integer) animation.getAnimatedValue();
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mTv
                    .getLayoutParams();
            lp.height = height;
            mTv.setLayoutParams(lp);
        }
    };

    public ExpandableTextView(Context context) {
        this(context, null);
    }

    public ExpandableTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public void setOrientation(int orientation) {
        if (LinearLayout.HORIZONTAL == orientation) {
            throw new IllegalArgumentException(
                    "ExpandableTextView only supports Vertical Orientation.");
        }
        super.setOrientation(orientation);
    }

    @Override
    public void onClick(View view) {
        Log.e("lzw", "expandabletextview onclick");
        hasInit = true;
        if (mAnimating) {
            return;
        }

        initAnimation();

        mButton.setImageDrawable(!mCollapsed ? mExpandDrawable
                : mCollapseDrawable);

        mAnimating = true;

        if (mCollapsed) { // 已关闭，执行打开动画
            open.start();
        } else { // 执行打开动画
            close.start();
        }

    }

    /**
     * 初始化动画，放在点击事件中初始化，这时候textview高度确定，只初始化一次就够了
     */
    private void initAnimation() {
        if (open == null) {
            open = ValueAnimator.ofInt(mCollapsedHeight, mExpandHeight);
            close = ValueAnimator.ofInt(mExpandHeight, mCollapsedHeight);
            open.setDuration(mAnimationDuration);
            close.setDuration(mAnimationDuration);
            open.setInterpolator(new DecelerateInterpolator());
            close.setInterpolator(new DecelerateInterpolator());

            open.addListener(endListener);
            close.addListener(endListener);
            open.addUpdateListener(updateListener);
            close.addUpdateListener(updateListener);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mAnimating;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        findViews();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!hasInit) { // 没测量过，需要测量textview最大最小高度
            // textview先设置最大行数
            mTv.setMaxLines(Integer.MAX_VALUE);
            // 高度为包含内容时才能测量出折叠时的高度
            mTv.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
            // 测量最大高宽
            measureChild(mTv, widthMeasureSpec, heightMeasureSpec);
            mExpandHeight = mTv.getMeasuredHeight();
            Log.e("lzw", "onmeasure mTv " + mTv.getMeasuredHeight() + "  " + mTv.getHeight());

            // mCollapsedHeight = mExpandHeight/mTv.getLineCount();

            // 测量最小高度
            // 超过最大行数，按钮可见，否则不可见，未初始化时按照最大行数来判断
            if (mTv.getLineCount() <= mMaxCollapsedLines) { // 最大行数小于折叠时的最大行数，表示不需要折叠，按钮隐藏
                mButton.setVisibility(View.GONE);
            } else {
                mButton.setVisibility(View.VISIBLE);
                // 还没有初始化过，最大行数也超过折叠时最大行数，测量折叠时的高度
                mTv.setMaxLines(mMaxCollapsedLines);
                measureChild(mTv, widthMeasureSpec, heightMeasureSpec);
                mCollapsedHeight = mTv.getMeasuredHeight();
            }

            if (mButton != null) {
                mButton.setImageDrawable(mCollapsed ? mExpandDrawable
                        : mCollapseDrawable);
            }

            // 为了后续改动最大行数，改变后，保持原状态
            mTv.setMaxLines(mCollapsed ? mMaxCollapsedLines : Integer.MAX_VALUE);
            measureChild(mTv, widthMeasureSpec, heightMeasureSpec);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        }

        Log.e("lzw", "expand onmeasure this " + this.getMeasuredHeight() + "  " + this.getHeight());

    }

    private void init() {
        if (Build.VERSION.SDK_INT <= 21) {
            mExpandDrawable = getContext().getResources().getDrawable(
                    R.drawable.activity_arrow_down);
            mCollapseDrawable = getContext().getResources().getDrawable(
                    R.drawable.activity_arrow_up);
        } else {
            mExpandDrawable = getContext().getDrawable(
                    R.drawable.activity_arrow_down);
            mCollapseDrawable = getContext().getDrawable(
                    R.drawable.activity_arrow_up);
        }

        setOrientation(LinearLayout.VERTICAL);
    }

    /**
     * 初始化按钮和textview
     */
    private void findViews() {
        mTv = (TextView) getChildAt(0);
        mButton = (ImageButton) getChildAt(1);
        mButton.setImageDrawable(mCollapsed ? mExpandDrawable
                : mCollapseDrawable);
        mButton.setOnClickListener(this);
    }

    /**
     * 设置文案
     */
    public void setText(@Nullable CharSequence text) {
        mTv.setText(text);
        hasInit = false;
        open = null;
        close = null;
        requestLayout();
    }

    @Nullable
    public CharSequence getText() {
        if (mTv == null) {
            return "";
        }
        return mTv.getText();
    }

    /**
     * 获取折叠时最大行数
     */
    public int getmMaxCollapsedLines() {
        return mMaxCollapsedLines;
    }

    /**
     * 设置折叠时最大行数
     */
    public void setmMaxCollapsedLines(int mMaxCollapsedLines) {
        this.mMaxCollapsedLines = mMaxCollapsedLines;
        // 重置标识，动画也需要更改
        hasInit = false;
        open = null;
        close = null;
        requestLayout();
    }

    /**
     * 设置未展开时的箭头
     */
    public void setmExpandDrawable(Drawable mExpandDrawable) {
        this.mExpandDrawable = mExpandDrawable;
        if (mButton != null) {
            mButton.setImageDrawable(mCollapsed ? mExpandDrawable
                    : mCollapseDrawable);
        }
    }

    /**
     * 设置展开时的箭头
     */
    public void setmCollapseDrawable(Drawable mCollapseDrawable) {
        this.mCollapseDrawable = mCollapseDrawable;
        if (mButton != null) {
            mButton.setImageDrawable(mCollapsed ? mExpandDrawable
                    : mCollapseDrawable);
        }
    }

}