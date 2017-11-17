package com.dctp.bgylib.view;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dctp.bgylib.R;

import java.util.Timer;
import java.util.TimerTask;


/**
 * 自定义的布局，用来管理三个子控件，其中一个是下拉头，一个是包含内容的pullableView（可以是实现Pullable接口的的任何View），
 * 还有一个上拉头
 */
public class PullToRefreshLayout extends RelativeLayout {
    // 初始状态
    public static final int INIT = 0;
    // 释放刷新
    public static final int RELEASE_TO_REFRESH = 1;
    // 正在刷新
    public static final int REFRESHING = 2;
    // 释放加载
    public static final int RELEASE_TO_LOAD = 3;
    // 正在加载
    public static final int LOADING = 4;
    // 操作完毕
    public static final int DONE = 5;
    // 当前是否最后一页
    public static int currentLastPage = -1;
    // 初始化加载更多状态
    public static final int INIT_LOAD = -1;
    // 当前状态
    private int state = INIT;
    // 刷新回调接口
    private OnRefreshListener mListener;
    // 刷新成功
    public static final int SUCCEED = 0;
    // 刷新失败
    public static final int FAIL = 1;
    // 最后一页
    public static final int LASTPAGE = 2;
    // 按下Y坐标，上一个事件点Y坐标
    private float downY, lastY;
    private float downX;
    // 下拉的距离。注意：pullDownY和pullUpY不可能同时不为0
    public float pullDownY = 0;
    // 上拉的距离
    private float pullUpY = 0;

    // 释放刷新的距离
    private float refreshDist = 200;
    // 释放加载的距离
    private float loadmoreDist = 200;

    private MyTimer timer;
    // 回滚速度
    public float MOVE_SPEED = 20;
    // 第一次执行布局
    private boolean isLayout;
    // 在刷新过程中滑动操作
    private boolean isTouch;
    // 手指滑动距离与下拉头的滑动距离比，中间会随正切函数变化
    private float radio = 2;

    // 下拉箭头的转180°动画
    private RotateAnimation rotateAnimation;
    // 均匀旋转动画
    private RotateAnimation refreshingAnimation;

    // 下拉头
    private View refreshView;
    // 下拉的箭头
    private View pullView;
    // 正在刷新的图标
    private View refreshingView;
    // 刷新结果图标
    private View refreshStateImageView;
    // 刷新结果：成功或失败
    private TextView refreshStateTextView;

    // 上拉头
    private View loadmoreView;
    // 上拉的箭头
    private View pullUpView;
    // 正在加载的图标
    private View loadingView;
    // 加载结果图标
    private View loadStateImageView;
    // 加载结果：成功或失败
    private TextView loadStateTextView;

    // 实现了Pullable接口的View
    private View pullableView;
    // 过滤多点触碰
    private int mEvents;
    // 这两个变量用来控制pull的方向，如果不加控制，当情况满足可上拉又可下拉时没法下拉
    private boolean canPullDown = true;
    private boolean canPullUp = true;

    /**
     * 执行自动回滚的handler
     */
    Handler updateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // 回弹速度随下拉距离moveDeltaY增大而增大
            MOVE_SPEED = (float) (8 + 5 * Math.tan(Math.PI / 2
                    / getMeasuredHeight() * (pullDownY + Math.abs(pullUpY))));
            if (!isTouch) {
                // 正在刷新，且没有往上推的话则悬停，显示"正在刷新..."
                if (state == REFRESHING && pullDownY <= refreshDist) {
                    pullDownY = refreshDist;
                    timer.cancel();
                } else if (state == LOADING && -pullUpY <= loadmoreDist) {
                    pullUpY = -loadmoreDist;
                    timer.cancel();
                }

            }
            if (pullDownY > 0)
                pullDownY -= MOVE_SPEED;
            else if (pullUpY < 0)
                pullUpY += MOVE_SPEED;
            if (pullDownY < 0) {
                // 已完成回弹
                pullDownY = 0;
                pullView.clearAnimation();
                // 隐藏下拉头时有可能还在刷新，只有当前状态不是正在刷新时才改变状态
                if (state != REFRESHING && state != LOADING)
                    changeState(INIT);
                timer.cancel();
                requestLayout();
            }
            if (pullUpY > 0) {
                // 已完成回弹
                pullUpY = 0;
                pullUpView.clearAnimation();
                // 隐藏下拉头时有可能还在刷新，只有当前状态不是正在刷新时才改变状态
                if (state != REFRESHING && state != LOADING)
                    changeState(INIT);
                timer.cancel();
            }
            // 刷新布局,会自动调用onLayout
            requestLayout();
        }

    };
    //    private View viewTop;
    private View viewBottom;
    private RotateAnimation rotateAnimation_f;

    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    public PullToRefreshLayout(Context context) {
        super(context);
        initView(context);
    }

    public PullToRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PullToRefreshLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {
        timer = new MyTimer(updateHandler);
        rotateAnimation = (RotateAnimation) AnimationUtils.loadAnimation(
                context, R.anim.reverse_anim);
        rotateAnimation_f = (RotateAnimation) AnimationUtils.loadAnimation(
                context, R.anim.f_reverse_anim);
        refreshingAnimation = (RotateAnimation) AnimationUtils.loadAnimation(
                context, R.anim.rotating);
        // 添加匀速转动动画
        LinearInterpolator lir = new LinearInterpolator();
        rotateAnimation.setInterpolator(lir);
        rotateAnimation_f.setInterpolator(lir);
        refreshingAnimation.setInterpolator(lir);
    }

    private void hide() {
        timer.schedule(5);
    }

    public void refreshFinish() {
        initLoadmore();
        if (pullDownY > 0) {
            //刷新结果停留1秒
            mHandler.sendEmptyMessageDelayed(0, 500);
        } else {
            changeState(DONE);
            hide();
        }
    }

    public void LastPager() {
        setLoadMoreState(LASTPAGE);
    }

    public void initLoadmore() {
        loadStateTextView.setVisibility(GONE);
        setLoadMoreState(INIT_LOAD);
    }

    public void setLoadMoreState(int loadState) {
        currentLastPage = loadState;
        if (loadState == LASTPAGE) {
            viewBottom.setVisibility(GONE);
            loadStateTextView.setVisibility(VISIBLE);
//            loadStateTextView.setText(R.string.show_all_data);
            changeState(DONE);
            hide();
        }
    }

    public void loadmoreFinish() {
        if (pullUpY < 0) {
            //刷新结果停留1秒
            mHandler.sendEmptyMessageDelayed(0, 500);
        } else {
            changeState(DONE);
            hide();
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            changeState(DONE);
            hide();
        }
    };

    private void changeState(int to) {
        state = to;
//        refreshStateTextView.setVisibility(GONE);
        switch (state) {
            case INIT:
                // 下拉布局初始状态
                refreshStateImageView.setVisibility(View.GONE);
                refreshStateTextView.setText(R.string.a_pull_to_refresh);
//                pullView.clearAnimation();
                pullView.setVisibility(View.VISIBLE);
                pullView.startAnimation(rotateAnimation_f);
                if (currentLastPage == 2) {
                    break;
                }
                // 上拉布局初始状态
                loadStateImageView.setVisibility(View.GONE);
                loadStateTextView.setText(R.string.a_pullup_to_load);


                pullUpView.clearAnimation();
                pullUpView.startAnimation(rotateAnimation_f);
                pullUpView.setVisibility(View.VISIBLE);
                viewBottom.setVisibility(View.VISIBLE);
                break;
            case RELEASE_TO_REFRESH:
                // 释放刷新状态
                refreshStateTextView.setText(R.string.a_release_to_refresh);
                pullView.startAnimation(rotateAnimation);
                break;
            case REFRESHING:
                // 正在刷新状态
                pullView.clearAnimation();
                pullView.setVisibility(View.INVISIBLE);
                refreshingView.setVisibility(View.VISIBLE);

                refreshingView.startAnimation(refreshingAnimation);
//                refreshStateTextView.setVisibility(GONE);
                refreshStateTextView.setText("");
                break;
            case RELEASE_TO_LOAD:
                // 释放加载状态
                if (currentLastPage == 2) {
                    break;
                }
                loadStateTextView.setText(R.string.a_release_to_load);
                pullUpView.startAnimation(rotateAnimation);
                break;
            case LOADING:
                // 正在加载状态
                pullUpView.clearAnimation();
                pullUpView.setVisibility(View.INVISIBLE);

                loadingView.setVisibility(View.VISIBLE);
                loadingView.startAnimation(refreshingAnimation);
                loadStateTextView.setText(R.string.a_loading);
                break;
            case DONE:
                // 刷新或加载完毕，停止动画
                if (loadingView != null) {
                    loadingView.clearAnimation();
                    loadingView.setVisibility(View.GONE);
                }
                if (pullUpView != null) {
                    pullUpView.clearAnimation();
                    pullUpView.setVisibility(View.GONE);
                }
                if (refreshingView != null) {
                    refreshingView.clearAnimation();
                    refreshingView.setVisibility(View.GONE);

                }
                break;
        }
    }


    /**
     * 不限制上拉或下拉
     */
    private void releasePull() {
        canPullDown = true;
        canPullUp = true;
    }

    /*
     * （非 Javadoc）由父控件决定是否分发事件，防止事件冲突
     *
     * @see android.view.ViewGroup#dispatchTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                lastY = downY;
                timer.cancel();
                mEvents = 0;
                releasePull();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                // 过滤多点触碰
                mEvents = -1;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mEvents == 0) {
                    if ((pullDownY > 0
                            || (((Pullable) pullableView).canPullDown()
                            && canPullDown && state != LOADING))
                            && Math.abs(ev.getX() - downX) < Math.abs(ev.getY() - downY)) {
                        getParent().requestDisallowInterceptTouchEvent(false);//拦截子View触摸
                        // 可以下拉，正在加载时不能下拉
                        // 对实际滑动距离做缩小，造成用力拉的感觉
                        pullDownY = pullDownY + (ev.getY() - lastY) / radio;
                        if (pullDownY < 0) {
                            pullDownY = 0;
                            canPullDown = false;
                            canPullUp = true;
                        }
                        if (pullDownY > getMeasuredHeight())
                            pullDownY = getMeasuredHeight();
                        if (state == REFRESHING) {
                            // 正在刷新的时候触摸移动
                            isTouch = true;
                        }

                        //下拉放大缩小动画// 根据下拉距离改变比例
//                        pullDownViewAnimation();

                    } else if (pullUpY < 0
                            || (((Pullable) pullableView).canPullUp() && canPullUp && state != REFRESHING)) {
                        // 可以上拉，正在刷新时不能上拉
                        pullUpY = pullUpY + (ev.getY() - lastY) / radio;
                        if (pullUpY > 0) {
                            pullUpY = 0;
                            canPullDown = true;
                            canPullUp = false;
                        }
                        if (pullUpY < -getMeasuredHeight())
                            pullUpY = -getMeasuredHeight();
                        if (state == LOADING) {
                            // 正在加载的时候触摸移动
                            isTouch = true;
                        }
                    } else
                        releasePull();
                } else {
                    mEvents = 0;
                }
                lastY = ev.getY();

                //////////
                requestLayout();
                if (pullDownY > 0) {
                    if (pullDownY <= refreshDist
                            && (state == RELEASE_TO_REFRESH || state == DONE)) {
                        // 如果下拉距离没达到刷新的距离且当前状态是释放刷新，改变状态为下拉刷新
                        changeState(INIT);
                    }
                    if (pullDownY >= refreshDist && state == INIT) {
                        // 如果下拉距离达到刷新的距离且当前状态是初始状态刷新，改变状态为释放刷新
                        changeState(RELEASE_TO_REFRESH);
                    }
                } else if (pullUpY < 0) {
                    // 下面是判断上拉加载的，同上，注意pullUpY是负值
                    if (-pullUpY <= loadmoreDist
                            && (state == RELEASE_TO_LOAD || state == DONE)) {
                        changeState(INIT);
                    }
                    // 上拉操作
                    if (-pullUpY >= loadmoreDist && state == INIT) {
                        changeState(RELEASE_TO_LOAD);
                    }

                }
                // 因为刷新和加载操作不能同时进行，所以pullDownY和pullUpY不会同时不为0，因此这里用(pullDownY +
                // Math.abs(pullUpY))就可以不对当前状态作区分了
                if ((pullDownY + Math.abs(pullUpY)) > 8) {
                    // 防止下拉过程中误触发长按事件和点击事件
                    ev.setAction(MotionEvent.ACTION_CANCEL);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (pullDownY > refreshDist || -pullUpY > loadmoreDist)
                    // 正在刷新时往下拉（正在加载时往上拉），释放后下拉头（上拉头）不隐藏
                    isTouch = false;
                if (state == RELEASE_TO_REFRESH) {
                    changeState(REFRESHING);
                    // 刷新操作
                    if (mListener != null)
                        mListener.onRefresh(this);
                } else if (state == RELEASE_TO_LOAD) {
                    changeState(LOADING);
                    // 加载操作
                    if (mListener != null)
                        mListener.onLoadMore(this);
                }
                hide();
            default:
                break;
        }
        super.dispatchTouchEvent(ev);
        return true;
    }

    //下拉放大缩小动画// 根据下拉距离改变比例
    private void pullDownViewAnimation() {
        radio = (float) (2 + 2 * Math.tan(Math.PI / 2 / getMeasuredHeight()
                * (pullDownY + Math.abs(pullUpY))));
        int change = 0;
        change = AndroidUtils.getPhoneDensityDpiChange(getContext());
        float radioTmep = (float) (2 + 24 * Math.tan(Math.PI / 2 / getMeasuredHeight()
                * (pullDownY + Math.abs(pullUpY))));
        float temp = radioTmep * change;
        int mTemp = (int) temp;
        if (pullDownY < refreshDist) {
            if (mTemp > refreshDist) {
                mTemp = (int) refreshDist;
            }
            setPullViewAn(mTemp);
        }
    }

    private void setPullViewAn(int mTemp) {
        if (null == pullView || null == pullView.getLayoutParams())
            return;
        LayoutParams linearParams = (LayoutParams) pullView.getLayoutParams();
        linearParams.height = mTemp;
        linearParams.width = mTemp;
        pullView.setLayoutParams(linearParams);
    }

    /**
     * 自动刷新
     */
    public void autoRefresh() {
        AutoRefreshAndLoadTask task = new AutoRefreshAndLoadTask(1);
        task.execute(20);
    }

    private class AutoRefreshAndLoadTask extends
            AsyncTask<Integer, Float, String> {

        private final int mIndex;

        public AutoRefreshAndLoadTask(int index) {
            mIndex = index;
        }

        @Override
        protected String doInBackground(Integer... params) {
            if (mIndex == 1) {
                while (pullDownY < 4 / 3 * refreshDist) {
                    pullDownY += MOVE_SPEED;
                    publishProgress(pullDownY);
                    try {
                        Thread.sleep(params[0]);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else if (mIndex == 2) {
                while (pullUpY > 4 / 3 * -loadmoreDist) {
                    pullUpY -= MOVE_SPEED;
                    publishProgress(pullUpY);
                    try {
                        Thread.sleep(params[0]);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (mIndex == 1) {
                changeState(REFRESHING);
                if (mListener != null)
                    mListener.onRefresh(PullToRefreshLayout.this);
            } else if (mIndex == 2) {
                changeState(LOADING);
                if (mListener != null)
                    mListener.onLoadMore(PullToRefreshLayout.this);
            }
            hide();
        }

        @Override
        protected void onProgressUpdate(Float... values) {
            if (mIndex == 1) {
                if (pullDownY > refreshDist)
                    changeState(RELEASE_TO_REFRESH);
            } else if (mIndex == 2) {
                if (pullUpY > -loadmoreDist)
                    changeState(RELEASE_TO_LOAD);
            }
            requestLayout();
        }

    }

    /**
     * 自动加载
     */
    public void autoLoad() {
        AutoRefreshAndLoadTask task = new AutoRefreshAndLoadTask(2);
        task.execute(20);
    }

    private void initView() {
        // 初始化下拉布局
        pullView = refreshView.findViewById(R.id.pull_icon);
        refreshStateTextView = (TextView) refreshView
                .findViewById(R.id.state_tv);
        refreshingView = refreshView.findViewById(R.id.refreshing_icon);
        refreshStateImageView = refreshView.findViewById(R.id.state_iv);
//        viewTop = refreshView.findViewById(R.id.viewTop);

        // 初始化上拉布局
        pullUpView = loadmoreView.findViewById(R.id.pullup_icon);
        loadStateTextView = (TextView) loadmoreView
                .findViewById(R.id.loadstate_tv);
        loadingView = loadmoreView.findViewById(R.id.loading_icon);
        loadStateImageView = loadmoreView.findViewById(R.id.loadstate_iv);
        viewBottom = loadmoreView.findViewById(R.id.viewBottom);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Log.d("Test", "Test");
        if (!isLayout) {
            // 这里是第一次进来的时候做一些初始化
            refreshView = getChildAt(0);
            pullableView = getChildAt(1);
            loadmoreView = getChildAt(2);
            isLayout = true;
            initView();
            refreshDist = ((ViewGroup) refreshView).getChildAt(0)
                    .getMeasuredHeight();
            loadmoreDist = ((ViewGroup) loadmoreView).getChildAt(0)
                    .getMeasuredHeight();
        }
        // 改变子控件的布局，这里直接用(pullDownY + pullUpY)作为偏移量，这样就可以不对当前状态作区分
        refreshView.layout(0,
                (int) (pullDownY + pullUpY) - refreshView.getMeasuredHeight(),
                refreshView.getMeasuredWidth(), (int) (pullDownY + pullUpY));
        pullableView.layout(0, (int) (pullDownY + pullUpY),
                pullableView.getMeasuredWidth(), (int) (pullDownY + pullUpY)
                        + pullableView.getMeasuredHeight());
        loadmoreView.layout(0,
                (int) (pullDownY + pullUpY) + pullableView.getMeasuredHeight(),
                loadmoreView.getMeasuredWidth(),
                (int) (pullDownY + pullUpY) + pullableView.getMeasuredHeight()
                        + loadmoreView.getMeasuredHeight());

    }

    class MyTimer {
        private Handler handler;
        private Timer timer;
        private MyTask mTask;

        public MyTimer(Handler handler) {
            this.handler = handler;
            timer = new Timer();
        }

        public void schedule(long period) {
            if (mTask != null) {
                mTask.cancel();
                mTask = null;
            }
            mTask = new MyTask(handler);
            timer.schedule(mTask, 0, period);
        }

        public void cancel() {
            if (mTask != null) {
                mTask.cancel();
                mTask = null;
            }
        }

        class MyTask extends TimerTask {
            private Handler handler;

            public MyTask(Handler handler) {
                this.handler = handler;
            }

            @Override
            public void run() {
                handler.obtainMessage().sendToTarget();
            }

        }
    }

    /**
     * 刷新加载回调接口
     */
    public interface OnRefreshListener {
        /**
         * 刷新操作
         */
        void onRefresh(PullToRefreshLayout pullToRefreshLayout);

        /**
         * 加载操作
         */
        void onLoadMore(PullToRefreshLayout pullToRefreshLayout);
    }

}
