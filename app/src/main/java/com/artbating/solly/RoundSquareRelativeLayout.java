package com.artbating.solly;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.makeramen.roundedimageview.RoundedImageView;

/**
 * Created by chunghoen on 2017-03-18.
 */

public class RoundSquareRelativeLayout extends RelativeLayout {
    public RoundSquareRelativeLayout(Context context) {
        super(context);
    }

    public RoundSquareRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RoundSquareRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (widthMeasureSpec < 1) {
            super.onMeasure(heightMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        }
    }
}
