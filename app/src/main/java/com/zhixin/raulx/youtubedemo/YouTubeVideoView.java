package com.zhixin.raulx.youtubedemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
/**
 * Created by laoyongzhi on 2017/4/15.
 */

public class YouTubeVideoView extends LinearLayout {


    //TODO
    public interface Callback{
        void onVideoViewHide();
        void onVideoClick();
    }

    //单机以及消失时的回调
    private Callback mCallback;

    private final static String TAG = "YouTubeVideoView";

    // 可拖动的videoView 和下方的详情View
    private View mVideoView;
    private View mDetailView;
    // video类的包装类，用于属性动画
    private VideoViewWrapper mVideoWrapper;

    //滑动区间,取值为是videoView最小化时距离屏幕顶端的高度
    private float allScrollHeight;

    //1f为初始状态，0.5f为最小状态
    private float nowStateScale;

    //是否是第一次Layout，用于获取播放器初始宽高
    private boolean isFirstLayout = true;

    //VideoView初始宽高
    private int originalWidth;
    private int originalHeight;

    //最小的缩放比例
    private static final float MIN_RATIO = 0.5f;

    //最小时距离屏幕右边以及下边的 DP值 初始化时会转化为PX
    private static final int MARGIN_DP = 12;
    private int marginPx;

    //是否可以横滑删除
    private boolean canDismiss;

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
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (isFirstLayout) {
            originalWidth = mVideoView.getMeasuredWidth();
            originalHeight = mVideoView.getMeasuredHeight();
            //滑动区间,取值为是videoView最小化时距离屏幕顶端的高度 也就是最小化时的marginTop
            allScrollHeight = this.getHeight() - MIN_RATIO * originalHeight - marginPx;
            isFirstLayout = false;
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

        private float percent;
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
                    int newMarY = mVideoWrapper.getMargin() + dy;
                    int newMarX = mVideoWrapper.getMarginRight() - dx;
                    int dDownY = y - mDownY;
                    int dDownX = x - mDownX; // 从点击点开始产生的的差值

                    //由差值算出应改变的 宽高百分比
                    percent = dDownY / allScrollHeight * 0.5f;

                    //如果滑动达到一定距离
                    if (Math.abs(dDownX) > 20 || Math.abs(dDownY) > 20) {
                        isClick = false ;
                        if (Math.abs(dDownX) > Math.abs(dDownY) && canDismiss) {//如果X>Y 且能滑动关闭
                            mVideoWrapper.setMarginRight(newMarX);
                        } else
                            updateVideoView(percent, newMarY); //否则更新大小
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:

                    if(isClick){

                        if(nowStateScale == 1f ){
                            if(mCallback != null)
                                mCallback.onVideoClick();
                        } else{
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

                    if (canDismiss) {
                        if (yVelocity > 20 || Math.abs(mVideoWrapper.getMarginRight()) > MIN_RATIO * originalWidth)
                            dismissView();
                        else
                            goMin();
                    } else
                        confirmState(yVelocity,dy);
                    break;
            }

            mLastY = y;
            mLastX = x;
            return true;
        }
    }

    private void dismissView(){
        ObjectAnimator anim = ObjectAnimator.ofFloat(mVideoView, "alpha", 1f, 0);
        anim.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                setVisibility(INVISIBLE);
                mVideoView.setAlpha(1f);
            }
        });
        anim.setDuration(300).start();

        if(mCallback != null)
            mCallback.onVideoViewHide();
    }


    private void confirmState(float v ,int dy) { //dy用于判断是否反方向滑动了

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
        ObjectAnimator.ofFloat(mVideoWrapper, "width", mVideoWrapper.getWidth(), originalWidth).setDuration(200).start();
        ObjectAnimator.ofFloat(mVideoWrapper, "height", mVideoWrapper.getHeight(), originalHeight).setDuration(200).start();
        ObjectAnimator.ofInt(mVideoWrapper, "margin", mVideoWrapper.getMargin(), 0).setDuration(200).start();
        ObjectAnimator.ofInt(mVideoWrapper, "marginRight", mVideoWrapper.getMarginRight(), 0).setDuration(200).start();
        ObjectAnimator.ofInt(mVideoWrapper, "detailMargin", mVideoWrapper.getDetailMargin(), 0).setDuration(200).start();
        ObjectAnimator.ofFloat(mVideoWrapper, "z", mVideoWrapper.getZ(), 0).setDuration(200).start();
        ObjectAnimator.ofFloat(mDetailView, "alpha", mDetailView.getAlpha(), 1f).setDuration(200).start();
        ObjectAnimator.ofInt(this.getBackground(),"alpha",this.getBackground().getAlpha(),255).setDuration(200).start();
        nowStateScale = 1.0f;
        canDismiss = false;
    }

    public void goMin() {
        ObjectAnimator.ofFloat(mVideoWrapper, "width", mVideoWrapper.getWidth(), originalWidth * MIN_RATIO).setDuration(200).start();
        ObjectAnimator.ofFloat(mVideoWrapper, "height", mVideoWrapper.getHeight(), originalHeight * MIN_RATIO).setDuration(200).start();
        ObjectAnimator.ofInt(mVideoWrapper, "margin", mVideoWrapper.getMargin(), (int) allScrollHeight).setDuration(200).start();
        ObjectAnimator.ofInt(mVideoWrapper, "marginRight", mVideoWrapper.getMarginRight(), marginPx).setDuration(200).start();
        ObjectAnimator.ofInt(mVideoWrapper, "detailMargin", mVideoWrapper.getDetailMargin(), marginPx).setDuration(200).start();
        ObjectAnimator.ofFloat(mVideoWrapper, "z", mVideoWrapper.getZ(), marginPx / 2).setDuration(200).start();
        ObjectAnimator.ofInt(this.getBackground(),"alpha",this.getBackground().getAlpha(),0).setDuration(200).start();
        ObjectAnimator anim = ObjectAnimator.ofFloat(mDetailView, "alpha", mDetailView.getAlpha(), 0).setDuration(200);
        anim.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                canDismiss = true;

                ViewGroup.LayoutParams p = getLayoutParams();
                p.width = -2;
                p.height = -2;
                setLayoutParams(p);

                nowStateScale = MIN_RATIO;
            }
        });
        anim.start();
    }

    //获取当前状态
    public float getNowStateScale(){
        return nowStateScale;
    }

    public void show(){
        setVisibility(VISIBLE);
        ViewGroup.LayoutParams params = getLayoutParams();
        params.width = -1;
        params.height = -1;
        setLayoutParams(params);
        goMax();
    }

    private void updateVideoView(float p, int m) {

        if(nowStateScale == 0.5f) {
            ViewGroup.LayoutParams params = getLayoutParams();
            params.width = -1;
            params.height = -1;
            setLayoutParams(params);
        }

        canDismiss = false;

        float w = originalWidth * (nowStateScale - p);
        float h = originalHeight * (nowStateScale - p);

        //height 只能是最大值  最小值之间
        if (h > originalHeight) {
            h = originalHeight;
            m = 0;
        } else if (h < MIN_RATIO * originalHeight) {
            h = MIN_RATIO * originalHeight;
            m = (int) allScrollHeight;
        }

        if (w > originalWidth) {
            w = originalWidth;
            m = 0;
        } else if (w < MIN_RATIO * originalWidth) {
            w = MIN_RATIO * originalWidth;
            m = (int) allScrollHeight;
        }

        mVideoWrapper.setHeight(h);
        mVideoWrapper.setWidth(w);

        float totalPercent = (allScrollHeight - m) / allScrollHeight;

        Log.d(TAG,">>>>p "+ String.valueOf(nowStateScale - p) +"         totalp"+totalPercent);

        mDetailView.setAlpha(totalPercent);
        this.getBackground().setAlpha((int)(totalPercent * 255));

        int mr = (int) ((1f - totalPercent) * marginPx); //VideoView右边和详情View 上方的margin
        mVideoWrapper.setZ(mr / 2);

        mVideoWrapper.setMargin(m);
        mVideoWrapper.setMarginRight(mr);
        mVideoWrapper.setDetailMargin(mr);

    }

    public void setCallback(Callback callback){
        mCallback = callback;
    }


    private class VideoViewWrapper {
        private View mVideoView;
        private LinearLayout.LayoutParams params;
        private LinearLayout.LayoutParams detailParams;

        public VideoViewWrapper(View target) {
            mVideoView = target;
            params = (LinearLayout.LayoutParams) mVideoView.getLayoutParams();
            detailParams = (LinearLayout.LayoutParams) mDetailView.getLayoutParams();
            params.gravity = Gravity.END;
        }

        public int getWidth() {
            return params.width < 0 ? originalWidth : params.width;
        }

        public int getHeight() {
            return params.height < 0 ? originalHeight : params.height;
        }

        public void setWidth(float width) {
            if (width == originalWidth) {
                params.width = -1;
                params.setMargins(0, 0, 0, 0);
            } else
                params.width = (int) width;

            mVideoView.setLayoutParams(params);
        }

        public void setHeight(float height) {
            params.height = (int) height;
            mVideoView.setLayoutParams(params);
        }

        public void setMargin(int m) {
            params.topMargin = m;
            mVideoView.setLayoutParams(params);
        }

        public int getMargin() {
            return params.topMargin;
        }

        public void setMarginRight(int mr) {
            params.rightMargin = mr;
            mVideoView.setLayoutParams(params);
        }

        public int getMarginRight() {
            return params.rightMargin;
        }

        public void setZ(float z) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                mVideoView.setTranslationZ(z);
        }

        public float getZ() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                return mVideoView.getTranslationZ();
            else
                return 0;
        }

        public void setDetailMargin(int t) {
            detailParams.topMargin = t;
            mDetailView.setLayoutParams(detailParams);
        }

        public int getDetailMargin() {
            return detailParams.topMargin;
        }

    }
}