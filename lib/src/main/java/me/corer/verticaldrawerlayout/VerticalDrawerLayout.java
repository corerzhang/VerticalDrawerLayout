package me.corer.verticaldrawerlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.v4.view.KeyEventCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 作者： corer.zhang   时间： 16/3/29.
 * 功能：
 * 修改：
 */
public class VerticalDrawerLayout extends ViewGroup {

    @IntDef({STATE_IDLE, STATE_DRAGGING, STATE_SETTLING})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
    }

    /**
     * Indicates that any drawers are in an idle, settled state. No animation is in progress.
     */
    public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;

    /**
     * Indicates that a drawer is currently being dragged by the user.
     */
    public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;
    /**
     * Indicates that a drawer is in the process of settling to a final position.
     */
    public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;

    private static final int MIN_DRAWER_MARGIN = 64; // dp
    private static final int MIN_FLING_VELOCITY = 400; // dips per second

    private int mMinDrawerMargin;
    private float mContentScrimOpacity;

    private static final int DEFAULT_CONTENT_SCRIM_COLOR = 0x99000000;
    private int mContentScrimColor = DEFAULT_CONTENT_SCRIM_COLOR;
    private final Paint mContentScrimPaint = new Paint();

    private boolean mInLayout;
    private boolean mFirstLayout = true;
    private int mDrawerState;

    ViewDragHelper mDragHelper;
    View mContentView;
    View mDrawerView;

    DrawerListener mListener;
    Drawable mShadow;



    public VerticalDrawerLayout(Context context) {
        this(context, null);
    }

    public VerticalDrawerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalDrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final float density = getResources().getDisplayMetrics().density;
        mMinDrawerMargin = (int) (MIN_DRAWER_MARGIN * density + 0.5f);
        final float minVel = MIN_FLING_VELOCITY * density;

        mDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragCallback());
        mDragHelper.setMinVelocity(minVel);
        // So that we can catch the back button
        setFocusableInTouchMode(true);
    }

    public void setDrawerShadow(Drawable shadowDrawable) {
        mShadow = shadowDrawable;
        invalidate();
    }

    public void setDrawerListener(DrawerListener listener) {
        mListener = listener;
    }

    public void closeDrawer() {
        closeDrawerView(mDrawerView);
    }

    public void openDrawerView() {
        openDrawerView(mDrawerView);
    }



    public boolean isDrawerOpen() {
        return ((LayoutParams) mDrawerView.getLayoutParams()).knownOpen;
    }




    @Override
    public void computeScroll() {
        final int childCount = getChildCount();
        float scrimOpacity = 0;
        for (int i = 0; i < childCount; i++) {
            final float onscreen = ((LayoutParams) getChildAt(i).getLayoutParams()).onScreen;
            scrimOpacity = Math.max(scrimOpacity, onscreen);
        }
        mContentScrimOpacity = scrimOpacity;
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public class ViewDragCallback extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return false;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            float offset;
            final int childHeight = changedView.getHeight();


            // This reverses the positioning shown in onLayout.
            offset = 1 - ((float) (-top) / (childHeight));

            setDrawerViewOffset(changedView, offset);

            final View contentView = mContentView;
            final boolean isContentViewVisible = offset == 1.f
                    && changedView.getMeasuredHeight() == getMeasuredHeight();

            contentView.setVisibility(isContentViewVisible ? INVISIBLE : VISIBLE);

            invalidate();
        }

        @Override
        public void onViewDragStateChanged(int state) {
            updateDrawerState(state, mDragHelper.getCapturedView());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int childCount = getChildCount();
        if (childCount != 2) {
            throw new IllegalArgumentException("There must be 2 child in VerticalDrawerLayout");
        }
        mContentView = getChildAt(0);
        mDrawerView = getChildAt(1);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(widthSize, heightSize);

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (isContentView(child)) {
                // Content views get measured at exactly the layout's size.
                final int contentWidthSpec = MeasureSpec.makeMeasureSpec(
                        widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
                final int contentHeightSpec = MeasureSpec.makeMeasureSpec(
                        heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
                child.measure(contentWidthSpec, contentHeightSpec);
            } else {
                final int drawerWidthSpec = getChildMeasureSpec(widthMeasureSpec,
                        lp.leftMargin + lp.rightMargin,
                        lp.width);
                final int drawerHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                        mMinDrawerMargin + lp.topMargin + lp.bottomMargin,
                        lp.height);
                child.measure(drawerWidthSpec, drawerHeightSpec);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout = true;

        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (isContentView(child)) {
                child.layout(lp.leftMargin, lp.topMargin, lp.leftMargin + child.getMeasuredWidth(), lp.topMargin + child.getMeasuredHeight());

                final View drawerView = mDrawerView;
                if (drawerView != null) {
                    final LayoutParams drawerLayoutParams = (LayoutParams) drawerView.getLayoutParams();
                    if (drawerLayoutParams.onScreen == 1.f
                            && drawerView.getMeasuredHeight() == getMeasuredHeight()) {
                        child.setVisibility(INVISIBLE);
                    }
                }
            } else if (lp.onScreen == 0) { // Drawer view - hidden

                child.layout(lp.leftMargin, -child.getMeasuredHeight(), lp.leftMargin + child.getMeasuredWidth(), 0);
            } else { // Drawer view - displayed
                final int top = -child.getMeasuredHeight() + (int) ((child.getMeasuredHeight()) * lp.onScreen);

                child.layout(lp.leftMargin, top, lp.leftMargin + child.getMeasuredWidth(), (int) ((child.getMeasuredHeight()) * lp.onScreen));
            }
        }
        mInLayout = false;
        mFirstLayout = false;

    }

    @Override
    public void requestLayout() {
        if (!mInLayout) {
            super.requestLayout();
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final int width = getWidth();
        int clipTop = 0, clipBottom = getHeight();
        boolean drawingContent = isContentView(child);
        final int restoreCount = canvas.save();

        if (drawingContent) {
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View v = getChildAt(i);
                if (v == child || v.getVisibility() != VISIBLE ||
                        !hasOpaqueBackground(v) || !(v == mDrawerView) ||
                        v.getWidth() < width) {
                    continue;
                }

                final int vBottom = v.getBottom();
                if (vBottom > clipTop) {
                    clipTop = vBottom;
                }
            }
            canvas.clipRect(0, clipTop, getWidth(), clipBottom);
        }
        final boolean result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(restoreCount);

        if (mContentScrimOpacity > 0 && drawingContent) {
            final int baseAlpha = (mContentScrimColor & 0xff000000) >>> 24;
            final int imag = (int) (baseAlpha * mContentScrimOpacity);
            final int color = imag << 24 | (mContentScrimColor & 0xffffff);
            mContentScrimPaint.setColor(color);

            canvas.drawRect(0, clipTop, getWidth(), clipBottom, mContentScrimPaint);
        } else if (mShadow != null) {
            final int shadowHeight = mShadow.getIntrinsicHeight();
            final int childBottom = child.getBottom();
            final int showing = childBottom;
            final int drawerPeekDistance = mDragHelper.getEdgeSize();
            final float alpha = Math.max(0, Math.min((float) showing / drawerPeekDistance, 1.f));
            mShadow.setBounds(child.getLeft(), childBottom - shadowHeight, child.getRight(), getHeight());
            mShadow.setAlpha((int) (0xff * alpha));
            mShadow.draw(canvas);
        }


        return result;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean interceptForDrag = mDragHelper.shouldInterceptTouchEvent(ev);
        boolean interceptForTap = false;

        final int action = MotionEventCompat.getActionMasked(ev);
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();

                final View touchedView = mDragHelper.findTopChildUnder((int) x, (int) y);
                if (isContentView(touchedView)) {
                    if (mContentScrimOpacity > 0) {
                        interceptForTap = true;
                    }
                }

                break;
            }
        }


        return interceptForDrag || interceptForTap;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mDragHelper.processTouchEvent(ev);

        final int action = ev.getAction();
        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {

                break;
            }
            case MotionEvent.ACTION_MOVE: {


                return false;
            }
            case MotionEvent.ACTION_UP: {
                final float x = ev.getX();
                final float y = ev.getY();
                final View touchedView = mDragHelper.findTopChildUnder((int) x, (int) y);

                if (touchedView == null) {
                    return false;
                }

                if (isContentView(touchedView)) {
                    if (mContentScrimOpacity > 0) {
                        closeDrawer();
                    }
                }

                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                break;
            }
        }

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && isDrawerVisible()) {
            KeyEventCompat.startTracking(event);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            boolean isDrawerVisible = isDrawerVisible();
            if (isDrawerVisible) {
                closeDrawer();
            }
            return isDrawerVisible;
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean isDrawerVisible() {
        return ((LayoutParams) mDrawerView.getLayoutParams()).onScreen > 0;
    }

    private boolean isContentView(View view){
        return view==mContentView;
    }


    private void closeDrawerView(View drawerView) {
        if (mFirstLayout) {
            final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
            lp.onScreen = 0.f;
            lp.knownOpen = false;
        } else {
            mDragHelper.smoothSlideViewTo(drawerView, drawerView.getLeft(), -drawerView.getHeight());

        }

        invalidate();
    }


    private void openDrawerView(View drawerView) {


        if (mFirstLayout) {
            final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
            lp.onScreen = 1.f;
            lp.knownOpen = true;
        } else {
            mDragHelper.smoothSlideViewTo(drawerView, drawerView.getLeft(), 0);
        }

        invalidate();
    }

    void updateDrawerState(int activeState, View activeDrawer) {

        final int leftState = mDragHelper.getViewDragState();

        final int state;
        if (leftState == STATE_DRAGGING) {
            state = STATE_DRAGGING;
        } else if (leftState == STATE_SETTLING) {
            state = STATE_SETTLING;
        } else {
            state = STATE_IDLE;
        }

        if (activeDrawer != null && activeState == STATE_IDLE) {
            final LayoutParams lp = (LayoutParams) activeDrawer.getLayoutParams();
            if (lp.onScreen == 0) {
                dispatchOnDrawerClosed(activeDrawer);
            } else if (lp.onScreen == 1) {
                dispatchOnDrawerOpened(activeDrawer);
            }

        }

        if (state != mDrawerState) {
            mDrawerState = state;

            if (mListener != null) {
                mListener.onDrawerStateChanged(state);
            }
        }


    }

    void dispatchOnDrawerClosed(View drawerView) {
        final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (lp.knownOpen) {
            lp.knownOpen = false;
            if (mListener != null) {
                mListener.onDrawerClosed(drawerView);
            }

            updateChildrenImportantForAccessibility(drawerView, false);

            // Only send WINDOW_STATE_CHANGE if the host has window focus. This
            // may change if support for multiple foreground windows (e.g. IME)
            // improves.
            if (hasWindowFocus()) {
                final View rootView = getRootView();
                if (rootView != null) {
                    rootView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                }
            }
        }
    }

    void dispatchOnDrawerOpened(View drawerView) {
        final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (!lp.knownOpen) {
            lp.knownOpen = true;
            if (mListener != null) {
                mListener.onDrawerOpened(drawerView);
            }

            updateChildrenImportantForAccessibility(drawerView, true);

            // Only send WINDOW_STATE_CHANGE if the host has window focus.
            if (hasWindowFocus()) {
                sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
            }

            drawerView.requestFocus();
        }
    }

    private void updateChildrenImportantForAccessibility(View drawerView, boolean isDrawerOpen) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (!isDrawerOpen && !(child == mDrawerView)
                    || isDrawerOpen && child == drawerView) {
                // Drawer is closed and this is a content view or this is an
                // open drawer view, so it should be visible.
                ViewCompat.setImportantForAccessibility(child,
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
            } else {
                ViewCompat.setImportantForAccessibility(child,
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            }
        }
    }


    void dispatchOnDrawerSlide(View drawerView, float slideOffset) {
        if (mListener != null) {
            mListener.onDrawerSlide(drawerView, slideOffset);
        }
    }

    void setDrawerViewOffset(View drawerView, float slideOffset) {
        final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (slideOffset == lp.onScreen) {
            return;
        }

        lp.onScreen = slideOffset;
        dispatchOnDrawerSlide(drawerView, slideOffset);
    }

    float getDrawerViewOffset(View drawerView) {
        return ((LayoutParams) drawerView.getLayoutParams()).onScreen;
    }


    private static boolean hasOpaqueBackground(View v) {
        final Drawable bg = v.getBackground();
        if (bg != null) {
            return bg.getOpacity() == PixelFormat.OPAQUE;
        }
        return false;
    }

    public interface DrawerListener {
        void onDrawerSlide(View drawerView, float slideOffset);

        /**
         * Called when a drawer has settled in a completely open state.
         * The drawer is interactive at this point.
         *
         * @param drawerView Drawer view that is now open
         */
        void onDrawerOpened(View drawerView);

        /**
         * Called when a drawer has settled in a completely closed state.
         *
         * @param drawerView Drawer view that is now closed
         */
        void onDrawerClosed(View drawerView);

        /**
         * Called when the drawer motion state changes. The new state will
         * be one of {@link #STATE_IDLE}, {@link #STATE_DRAGGING} or {@link #STATE_SETTLING}.
         *
         * @param newState The new drawer motion state
         */
        void onDrawerStateChanged(@State int newState);
    }

    public static abstract class SimpleDrawerListener implements DrawerListener {
        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
        }

        @Override
        public void onDrawerOpened(View drawerView) {
        }

        @Override
        public void onDrawerClosed(View drawerView) {
        }

        @Override
        public void onDrawerStateChanged(int newState) {
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams
                ? new LayoutParams((LayoutParams) p)
                : p instanceof ViewGroup.MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    private static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.layout_gravity
    };

    public static class LayoutParams extends MarginLayoutParams {
        public int gravity = Gravity.NO_GRAVITY;
        float onScreen;
        boolean knownOpen;

        /**
         * {@inheritDoc}
         */
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            final TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            this.gravity = a.getInt(0, Gravity.NO_GRAVITY);
            a.recycle();
        }

        /**
         * {@inheritDoc}
         */
        public LayoutParams(int width, int height) {
            super(width, height);
        }

        /**
         * Creates a new set of layout parameters with the specified width, height
         * and weight.
         *
         * @param width   the width, either {@link #MATCH_PARENT},
         *                {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param height  the height, either {@link #MATCH_PARENT},
         *                {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param gravity the gravity
         * @see android.view.Gravity
         */
        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        /**
         * {@inheritDoc}
         */
        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        /**
         * {@inheritDoc}
         */
        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        /**
         * Copy constructor. Clones the width, height, margin values, and
         * gravity of the source.
         *
         * @param source The layout params to copy from.
         */
        public LayoutParams(LayoutParams source) {
            super(source);
            this.gravity = source.gravity;
        }
    }
}
