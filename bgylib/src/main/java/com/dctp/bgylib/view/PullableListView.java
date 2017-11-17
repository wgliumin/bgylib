package com.dctp.bgylib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ListView;

import com.dctp.bgylib.R;


public class PullableListView extends ListView implements Pullable {
    private boolean mCanPullDown = true;
    private boolean mCanPullUp = true;
    private OnListForSearchListener mForSearchListener;

    public PullableListView(Context context) {
        super(context);
    }

    public PullableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PullableListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.IPullable);
        int mode = a.getInt(R.styleable.IPullable_PullMode, 0);
        a.recycle();
        switch (mode) {
            case Pullable.BOTH:// both
                mCanPullDown = true;
                mCanPullUp = true;
                break;
            case Pullable.TOP:// top
                mCanPullDown = true;
                mCanPullUp = false;
                break;
            case Pullable.BOTTOM:// bottom
                mCanPullDown = false;
                mCanPullUp = true;
                break;
            default:
                mCanPullDown = false;
                mCanPullUp = false;
                break;
        }
    }

    public void setPullToRefreshMode(int mode) {
        switch (mode) {
            case 0:// both
                mCanPullDown = true;
                mCanPullUp = true;
                break;
            case 1:// top
                mCanPullDown = true;
                mCanPullUp = false;
                break;
            case 2:// bottom
                mCanPullDown = false;
                mCanPullUp = true;
                break;
            default:
                mCanPullDown = false;
                mCanPullUp = false;
                break;
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mForSearchListener != null) {
            mForSearchListener.onListScrollForSearch(t);
        }
    }

    @Override
    public boolean canPullDown() {
        if (!mCanPullDown) {
            return false;
        }
        if (getCount() == 0) {
            return true;
        } else if (getFirstVisiblePosition() == 0
                && getChildAt(0).getTop() >= 0) {
            return true;
        } else
            return false;
    }

    @Override
    public boolean canPullUp() {
        if (!mCanPullUp) {
            return false;
        }
        if (getCount() == 0) {
            return true;
        } else if (getLastVisiblePosition() == (getCount() - 1)) {
            if (getChildAt(getLastVisiblePosition() - getFirstVisiblePosition()) != null
                    && getChildAt(
                    getLastVisiblePosition()
                            - getFirstVisiblePosition()).getBottom() <= getMeasuredHeight())

                return true;
        }
        return false;
    }

    public void setOnListForSearchListener(
            OnListForSearchListener mForSearchListener) {
        this.mForSearchListener = mForSearchListener;
    }

    public interface OnListForSearchListener {
        public void onListScrollForSearch(int scrollY);
    }
}
