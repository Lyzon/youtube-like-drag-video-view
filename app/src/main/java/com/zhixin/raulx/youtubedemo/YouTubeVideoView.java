package com.zhixin.raulx.youtubedemo;

/**
 * Created by laoyongzhi on 2017/4/15.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class YouTubeVideoView extends LinearLayout {

    interface Callback {
        void onVideoViewHide();

        void onVideoClick();
    }

    //单击以及消失时的回调
    private Callback mCallback;

    // 可拖动的videoView 和下方的详情View
    private View mVideoView;
    private View mDetailView;
    // video类的包装类，用于属性动画
    private VideoViewWrapper mVideoWrapper;

    //滑动区间,取值为是videoView最小化时距离屏幕顶端的高度
    private float allScrollY;

    //1f为初始状态，0.5f或0.25f(横屏时)为最小状态
    private float nowStateScale;
    //最小的缩放比例
    private float MIN_RATIO = 0.5f;
    //播放器比例
    private static final float VIDEO_RATIO = 16f / 9f;

    //是否是第一次Measure，用于获取播放器初始宽高
    private boolean isFirstMeasure = true;

    //VideoView初始宽高
    private int originalWidth;
    private int originalHeight;

    //最小时距离屏幕右边以及下边的 DP值 初始化时会转化为PX
    private static final int MARGIN_DP = 12;
    private int marginPx;

    //是否可以横滑删除
    private boolean canHide;

    public YouTubeVideoView(Context context) {
        this(context, null);
    }

    public YouTubeVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public YouTubeVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 2)
            throw new RuntimeException("YouTubeVideoView only need 2 child views");

        mVideoView = getChildAt(0);
        mDetailView = getChildAt(1);

        mVideoView.setOnTouchListener(new VideoTouchListener());
        //初始化包装类
        mVideoWrapper = new VideoViewWrapper(mVideoView);

        marginPx = MARGIN_DP * (getContext().getResources().getDisplayMetrics().densityDpi / 160);

        //当前缩放比例
        nowStateScale = 1f;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (isFirstMeasure) {
            //初始化宽高
            originalWidth = mVideoView.getContext().getResources().getDisplayMetrics().widthPixels;
            originalHeight = (int) (originalWidth / VIDEO_RATIO);
            ViewGroup.LayoutParams lp = mVideoView.getLayoutParams();
            lp.width = originalWidth;
            lp.height = originalHeight;
            mVideoView.setLayoutParams(lp);

            if (mVideoView.getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                MIN_RATIO = 0.25f;
            //滑动区间,取值为是videoView最小化时距离屏幕顶端的高度 也就是最小化时的marginTop
            allScrollY = this.getMeasuredHeight() - MIN_RATIO * originalHeight - marginPx;
            isFirstMeasure = false;
        }
    }

    private class VideoTouchListener implements View.OnTouchListener {
        //保存上一个滑动事件手指的坐标
        private int mLastY;
        private int mLastX;
        //刚触摸时手指的坐标
        private int mDownY;
        private int mDownX;

        private int dy;//和上一次滑动的差值 设置为全局变量是因为 UP里也要使用

        private boolean isClick;

        private VelocityTracker tracker;

        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            int y = (int) ev.getRawY();
            int x = (int) ev.getRawX();
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isClick = true;
                    tracker = VelocityTracker.obtain();
                    mDownY = (int) ev.getRawY();
                    mDownX = (int) ev.getRawX();
                    break;
                case MotionEvent.ACTION_MOVE:
                    tracker.addMovement(ev);
                    dy = y - mLastY; //和上一次滑动的差值
                    int dx = x - mLastX;
                    int newMarY = mVideoWrapper.getMargin() + dy; //新的marginTop值
                    int newMarX = mVideoWrapper.getMarginRight() - dx;//新的marginRight值
                    int dDownY = y - mDownY;
                    int dDownX = x - mDownX; // 从点击点开始产生的的差值

                    //如果滑动达到一定距离
                    if (Math.abs(dDownX) > 20 || Math.abs(dDownY) > 20) {
                        isClick = false;
                        if (Math.abs(dDownX) > Math.abs(dDownY) && canHide) {//如果X>Y 且能滑动关闭
                            mVideoWrapper.setMarginRight(newMarX);
                        } else
                            updateVideoView(newMarY); //否则更新大小
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:

                    if (isClick) {
                        if (nowStateScale == 1f) {
                            if (mCallback != null)
                                mCallback.onVideoClick();
                        } else {
                            ViewGroup.LayoutParams params = getLayoutParams();
                            params.width = -1;
                            params.height = -1;
                            setLayoutParams(params);
                            goMax();
                        }
                        break;
                    }

                    tracker.computeCurrentVelocity(100);
                    float yVelocity = Math.abs(tracker.getYVelocity());
                    tracker.clear();
                    tracker.recycle();

                    if (canHide) {
                        if (yVelocity > 20 || Math.abs(mVideoWrapper.getMarginRight()) > MIN_RATIO * originalWidth)
                            dismissView();
                        else
                            goMin();
                    } else
                        confirmState(yVelocity, dy);
                    break;
            }

            mLastY = y;
            mLastX = x;
            return true;
        }
    }

    private void updateVideoView(int m) {

        if (nowStateScale == MIN_RATIO) {
            ViewGroup.LayoutParams params = getLayoutParams();
            params.width = -1;
            params.height = -1;
            setLayoutParams(params);
        }

        canHide = false;

        if (m > allScrollY)
            m = (int) allScrollY;
        if (m < 0)
            m = 0;

        float marginPercent = (allScrollY - m) / allScrollY;
        float videoPercent = MIN_RATIO + (1f - MIN_RATIO) * marginPercent;

        mVideoWrapper.setWidth(originalWidth * videoPercent);
        mVideoWrapper.setHeight(originalHeight * videoPercent);

        mDetailView.setAlpha(marginPercent);
        this.getBackground().setAlpha((int) (marginPercent * 255));

        int mr = (int) ((1f - marginPercent) * marginPx); //VideoView右边和详情View 上方的margin
        mVideoWrapper.setZ(mr / 2);

        mVideoWrapper.setMargin(m);
        mVideoWrapper.setMarginRight(mr);
        mVideoWrapper.setDetailMargin(mr);
    }

    private void dismissView() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(mVideoView, "alpha", 1f, 0);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(INVISIBLE);
                mVideoView.setAlpha(1f);
            }
        });
        anim.setDuration(300).start();

        if (mCallback != null)
            mCallback.onVideoViewHide();
    }

    private void confirmState(float v, int dy) { //dy用于判断是否反方向滑动了

        //如果手指抬起时宽度达到一定值 或者 速度达到一定值 则改变状态
        if (nowStateScale == 1f) {
            if (mVideoView.getWidth() <= originalWidth * 0.75f || (v > 15 && dy > 0)) {
                goMin();
            } else
                goMax();
        } else {
            if (mVideoView.getWidth() >= originalWidth * 0.75f || (v > 15 && dy < 0)) {
                goMax();
            } else
                goMin();
        }
    }

    public void goMax() {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(mVideoWrapper, "width", mVideoWrapper.getWidth(), originalWidth),
                ObjectAnimator.ofFloat(mVideoWrapper, "height", mVideoWrapper.getHeight(), originalHeight),
                ObjectAnimator.ofInt(mVideoWrapper, "margin", mVideoWrapper.getMargin(), 0),
                ObjectAnimator.ofInt(mVideoWrapper, "marginRight", mVideoWrapper.getMarginRight(), 0),
                ObjectAnimator.ofInt(mVideoWrapper, "detailMargin", mVideoWrapper.getDetailMargin(), 0),
                ObjectAnimator.ofFloat(mVideoWrapper, "z", mVideoWrapper.getZ(), 0),
                ObjectAnimator.ofFloat(mDetailView, "alpha", mDetailView.getAlpha(), 1f),
                ObjectAnimator.ofInt(this.getBackground(), "alpha", this.getBackground().getAlpha(), 255)
        );
        set.setDuration(200).start();
        nowStateScale = 1.0f;
        canHide = false;
    }

    public void goMin() {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(mVideoWrapper, "width", mVideoWrapper.getWidth(), originalWidth * MIN_RATIO),
                ObjectAnimator.ofFloat(mVideoWrapper, "height", mVideoWrapper.getHeight(), originalHeight * MIN_RATIO),
                ObjectAnimator.ofInt(mVideoWrapper, "margin", mVideoWrapper.getMargin(), (int) allScrollY),
                ObjectAnimator.ofInt(mVideoWrapper, "marginRight", mVideoWrapper.getMarginRight(), marginPx),
                ObjectAnimator.ofInt(mVideoWrapper, "detailMargin", mVideoWrapper.getDetailMargin(), marginPx),
                ObjectAnimator.ofFloat(mVideoWrapper, "z", mVideoWrapper.getZ(), marginPx / 2),
                ObjectAnimator.ofInt(this.getBackground(), "alpha", this.getBackground().getAlpha(), 0),
                ObjectAnimator.ofFloat(mDetailView, "alpha", mDetailView.getAlpha(), 0)
        );
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                canHide = true;

                ViewGroup.LayoutParams p = getLayoutParams();
                p.width = -2;
                p.height = -2;
                setLayoutParams(p);

                nowStateScale = MIN_RATIO;
            }
        });
        set.setDuration(200).start();
    }

    //获取当前状态
    public float getNowStateScale() {
        return nowStateScale;
    }

    public void show() {
        setVisibility(VISIBLE);
        ViewGroup.LayoutParams params = getLayoutParams();
        params.width = -1;
        params.height = -1;
        setLayoutParams(params);
        goMax();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    private class VideoViewWrapper {
        private View mVideoView;
        private LinearLayout.LayoutParams params;
        private LinearLayout.LayoutParams detailParams;

        private VideoViewWrapper(View target) {
            mVideoView = target;
            params = (LinearLayout.LayoutParams) mVideoView.getLayoutParams();
            detailParams = (LinearLayout.LayoutParams) mDetailView.getLayoutParams();
            params.gravity = Gravity.END;
        }

        private int getWidth() {
            return params.width < 0 ? originalWidth : params.width;
        }

        private int getHeight() {
            return params.height < 0 ? originalHeight : params.height;
        }

        private void setWidth(float width) {
            if (width == originalWidth) {
                params.width = -1;
                params.setMargins(0, 0, 0, 0);
            } else
                params.width = (int) width;

            mVideoView.setLayoutParams(params);
        }

        private void setHeight(float height) {
            params.height = (int) height;
            mVideoView.setLayoutParams(params);
        }

        private void setMargin(int m) {
            params.topMargin = m;
            mVideoView.setLayoutParams(params);
        }

        private int getMargin() {
            return params.topMargin;
        }

        private void setMarginRight(int mr) {
            params.rightMargin = mr;
            mVideoView.setLayoutParams(params);
        }

        private int getMarginRight() {
            return params.rightMargin;
        }

        private void setZ(float z) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                mVideoView.setTranslationZ(z);
        }

        private float getZ() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                return mVideoView.getTranslationZ();
            else
                return 0;
        }

        private void setDetailMargin(int t) {
            detailParams.topMargin = t;
            mDetailView.setLayoutParams(detailParams);
        }

        private int getDetailMargin() {
            return detailParams.topMargin;
        }

    }
}