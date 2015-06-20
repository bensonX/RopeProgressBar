package com.deange.ropeprogressview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ProgressBar;

public class RopeProgressView extends ProgressBar {

    private final Paint mBubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLinesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float m1Dip;

    private int mPrimaryColor;
    private int mSecondaryColor;
    private float mSlack;

    private final Path mBubble = new Path();
    private final Path mTriangle = new Path();

    public RopeProgressView(final Context context) {
        this(context, null);
    }

    public RopeProgressView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RopeProgressView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        m1Dip = getResources().getDisplayMetrics().density;

        float width = dips(8);
        float slack = dips(32);

        int primaryColor = 0xFF009688;
        int secondaryColor = 0xFFDADADA;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final TypedValue out = new TypedValue();

            context.getTheme().resolveAttribute(R.attr.colorControlActivated, out, true);
            primaryColor = out.data;
            context.getTheme().resolveAttribute(R.attr.colorControlHighlight, out, true);
            secondaryColor = out.data;
        }

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.RopeProgressView, defStyleAttr, 0);

        if (a != null) {
            primaryColor = a.getColor(R.styleable.RopeProgressView_primaryColor, primaryColor);
            secondaryColor = a.getColor(R.styleable.RopeProgressView_secondaryColor, secondaryColor);
            slack = a.getDimension(R.styleable.RopeProgressView_slack, slack);
            width = a.getDimension(R.styleable.RopeProgressView_strokeWidth, width);

            a.recycle();
        }

        mPrimaryColor = primaryColor;
        mSecondaryColor = secondaryColor;
        mSlack = slack;

        mLinesPaint.setStrokeWidth(width);
        mLinesPaint.setStyle(Paint.Style.STROKE);
        mLinesPaint.setStrokeCap(Paint.Cap.ROUND);

        mBubblePaint.setColor(Color.WHITE);
        mBubblePaint.setStyle(Paint.Style.FILL);
        mBubblePaint.setPathEffect(new CornerPathEffect(dips(2)));

        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(dips(16));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTypeface(Typeface.create("sans-serif-condensed-light", 0));

        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setIndeterminate(false);
        setBackgroundDrawable(null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setBackgroundDrawable(final Drawable background) {
        super.setBackgroundDrawable(null);
    }

    @Override
    protected synchronized void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {

        final int bubbleHeight = (int) Math.ceil(getBubbleVerticalDisplacement());

        final float strokeWidth = getStrokeWidth();
        final int dw = (int) Math.ceil(getPaddingLeft() + getPaddingRight() + strokeWidth);
        final int dh = (int) Math.ceil(getPaddingTop() + getPaddingBottom() + strokeWidth + mSlack);

        setMeasuredDimension(
                resolveSizeAndState(dw, widthMeasureSpec, 0),
                resolveSizeAndState(dh + bubbleHeight, heightMeasureSpec, 0));

        makeBubble();
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @Override
    protected synchronized void onDraw(final Canvas canvas) {

        final float radius = getStrokeWidth() / 2;

        final float bubbleDisplacement = getBubbleVerticalDisplacement();
        final float top = getPaddingTop() + radius + bubbleDisplacement;
        final float left = getPaddingLeft() + radius;
        final float end = getWidth() - getPaddingRight() - radius;

        final float max = getMax();
        final float offset = (max == 0) ? 0 : (getProgress() / max);
        final float slackHeight = perp(offset) * getSlack();
        final float progressEnd = lerp(left, end, offset);

        mLinesPaint.setColor(mSecondaryColor);
        canvas.drawLine(progressEnd, top + slackHeight, end, top, mLinesPaint);

        mLinesPaint.setColor(mPrimaryColor);
        if (progressEnd == left) {
            // Draw the highlghted part as small as possible
            mLinesPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(left, top, radius, mLinesPaint);
            mLinesPaint.setStyle(Paint.Style.STROKE);

        } else {
            canvas.drawLine(left, top, progressEnd, top + slackHeight, mLinesPaint);
        }

        final float bubbleWidth = getBubbleWidth();
        final float bubbleHeight = getBubbleHeight();

        final float bubbleLeft = Math.min(
                getWidth() - bubbleWidth, Math.max(
                        0, progressEnd - (bubbleWidth / 2)));

        final float bubbleTop = slackHeight;
        final float textX = bubbleLeft + bubbleWidth / 2;
        final float textY = bubbleTop + bubbleHeight - dips(5);

        final float triangleLeft = Math.min(
                getWidth() - getTriangleWidth(), Math.max(
                        0, progressEnd - (getTriangleWidth() / 2)));
        final float triangleTop = bubbleTop + bubbleHeight;

        mBubble.offset(bubbleLeft, bubbleTop);
        mTriangle.offset(triangleLeft, triangleTop);

        final String progress = String.valueOf(getProgress());
        canvas.drawPath(mBubble, mBubblePaint);
        canvas.drawPath(mTriangle, mBubblePaint);
        canvas.drawText(progress, textX, textY, mTextPaint);

        // Return points back to normal location
        mBubble.offset(-bubbleLeft, -bubbleTop);
        mTriangle.offset(-triangleLeft, -triangleTop);
    }

    private float getBubbleVerticalDisplacement() {
        return getBubbleMargin() + getBubbleHeight() + getTriangleHeight();
    }

    private float getBubbleMargin() {
        return dips(4);
    }

    private float getBubbleWidth() {
        //noinspection ReplaceAllDot
        final String maxString = String.valueOf(getMax()).replaceAll(".", "8");
        final float maxSize = mTextPaint.measureText(maxString);

        return maxSize + /* padding */ dips(16);
    }

    private float getBubbleHeight() {
        return getBubbleWidth() / 2;
    }

    private float getTriangleWidth() {
        return dips(12);
    }

    private float getTriangleHeight() {
        return dips(6);
    }

    public void makeBubble() {

        final float bubbleWidth = getBubbleWidth();
        final float bubbleHeight = getBubbleHeight();

        final float triangleWidth = getTriangleWidth();
        final float triangleHeight = getTriangleHeight();
        final float triangleTop = 0;
        final float triangleLeft = 0;

        mTriangle.reset();
        mTriangle.moveTo(triangleLeft, triangleTop);
        mTriangle.lineTo(triangleLeft + triangleWidth, triangleTop);
        mTriangle.lineTo(triangleLeft + triangleWidth / 2f, triangleHeight);
        mTriangle.lineTo(triangleLeft, triangleTop);

        mBubble.reset();
        mBubble.moveTo(0, 0);
        mBubble.addRect(0, 0, bubbleWidth, bubbleHeight, Path.Direction.CW);
    }

    public void setPrimaryColor(final int color) {
        mPrimaryColor = color;

        invalidate();
    }

    public int getPrimaryColor() {
        return mPrimaryColor;
    }

    public void setSecondaryColor(final int color) {
        mSecondaryColor = color;

        invalidate();
    }

    public int getSecondaryColor() {
        return mSecondaryColor;
    }

    public void setSlack(final float slack) {
        mSlack = slack;

        requestLayout();
        invalidate();
    }

    public float getSlack() {
        return mSlack;
    }

    public void setStrokeWidth(final float width) {
        mLinesPaint.setStrokeWidth(width);

        requestLayout();
        invalidate();
    }

    public float getStrokeWidth() {
        return mLinesPaint.getStrokeWidth();
    }

    private float perp(float t) {
        // eh, could be more mathematically accurate to use a catenary function,
        // but the max difference between the two is only 0.005
        return (float) (-Math.pow(2 * t - 1, 2) + 1);
    }

    private float lerp(float v0, float v1, float t) {
        return (t == 1) ? v1 : (v0 + t * (v1 - v0));
    }

    private float dips(final float dips) {
        return dips * m1Dip;
    }

}
