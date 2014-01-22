/**
 * 
 */
package com.jasperb.citybuilder.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jasperb.citybuilder.CityModel;
import com.jasperb.citybuilder.util.Constant;

/**
 * Custom view for handling rendering the city model. Includes the ability to pan, fling and scale within the view.
 * 
 * @author Jasper
 * 
 */
public class CityView extends SurfaceView implements SurfaceHolder.Callback {

    /**
     * String used for identifying this class.
     */
    public static final String TAG = "CityView";

    private ScaleGestureDetector mScaleDetector = null;
    private GestureDetector mDetector = null;
    private CityViewState mState = new CityViewState();
    private boolean mAllocated = false;
    private DrawThread mDrawThread = null;
    private SurfaceHolder mSurfaceHolder = null;
    private boolean mSurfaceExists = false;

    public boolean isEverythingAllocated() {
        return mAllocated;
    }

    public void setCityModel(CityModel model) {
        mState.mCityModel = model;
    }

    public boolean getDrawGridLines() {
        return mState.mDrawGridLines;
    }

    public void setDrawGridLines(boolean drawGridLines) {
        mState.mDrawGridLines = drawGridLines;
        redraw();
    }

    public float getFocusRow() {
        return mState.mFocusRow;
    }

    public float getFocusCol() {
        return mState.mFocusCol;
    }

    public void setFocusCoords(float row, float col) {
        mState.mFocusRow = row;
        mState.mFocusCol = col;
        redraw();
    }

    public float getScaleFactor() {
        return mState.getScaleFactor();
    }

    public void setScaleFactor(float scaleFactor) {
        mState.setScaleFactor(scaleFactor);
        redraw();
    }

    public void redraw() {
        Log.d(TAG, "REDRAW");
        if (mDrawThread != null)
            mDrawThread.setDrawState(mState);
    }

    /**
     * Class constructor taking a context and an attribute set. This constructor is used by the layout engine to construct a
     * {@link CityView} from a set of XML attributes.
     * 
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs
     *            An attribute set which can contain attributes inherited from {@link android.view.View}.
     */
    public CityView(Context context, AttributeSet attrs) {
        super(context, attrs);
        construct();
    }

    /**
     * Class constructor taking a context. This constructor is used by the layout engine to construct a {@link CityView}.
     * 
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     */
    public CityView(Context context) {
        super(context);
        construct();
    }

    /**
     * Common method for constructors
     */
    private void construct() {
        mSurfaceHolder = getHolder();//Do this here because we hold onto this for the lifetime of the view
        mSurfaceHolder.addCallback(this);
    }

    /**
     * Initialize and allocate the necessary components of the view, except those related to the drawing thread
     */
    public void init() {
        mDetector = new GestureDetector(getContext(), new GestureListener());

        // Turn off long press--this control doesn't use it, and if long press is enabled, you can't scroll for a bit, pause, then scroll
        // some more (the pause is interpreted as a long press, apparently)
        mDetector.setIsLongpressEnabled(false);

        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        mAllocated = true;
    }

    /**
     * Cleanup the components of the view allocated by init()
     */
    public void cleanup() {
        mAllocated = false;

        mScaleDetector = null;
        mDetector = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Let the GestureDetectors interpret this event
        boolean result = mDetector.onTouchEvent(event);
        mScaleDetector.onTouchEvent(event);

        // If the GestureDetectors don't want this event, do some custom processing.
        // This code just tries to detect when the user is done scrolling by looking for ACTION_UP events.
//        if (!result) {
//            if (event.getAction() == MotionEvent.ACTION_UP) {
//                // stopScrolling();
//                result = true;
//            }
//        }
        redraw();
        return result;
    }

    /**
     * The gesture listener, used for handling scroll and fling gestures (for panning)
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {// For some reason, scaling and scroll don't work without this
            mState.setScaleFactor(mState.getScaleFactor() * 0.99f);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mState.mFocusRow += mState.realToIsoRowUpscaling(Math.round(distanceX), Math.round(distanceY));
            mState.mFocusCol += mState.realToIsoColUpscaling(Math.round(distanceX), Math.round(distanceY));

            redraw();
            return true;
        }
    }

    /**
     * The scale listener, used for handling multi-finger scale gestures.
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            float scaleFactor = mState.getScaleFactor() * scaleGestureDetector.getScaleFactor();

            // Don't let the object get too small or too large.
            if (mState.setScaleFactor(Math.max(Constant.MINIMUM_SCALE_FACTOR, Math.min(scaleFactor, Constant.MAXIMUM_SCALE_FACTOR)))) {
                redraw();//redraw if the scale factor affected the tile size
            }
            return true;
        }

    }

    /**
     * Start the drawing thread if possible
     * 
     * @param redraw
     *            if true a redraw event is triggered by this call
     */
    public void startDrawThread(boolean redraw) {
        // In some instances, the surface persists even when the view is stopped. We still stop the draw thread.
        // There's no point starting the thread before the surface is created.
        if (mSurfaceExists && mDrawThread == null) {
            Log.v(TAG, "startDrawThread");

            mDrawThread = new DrawThread(mSurfaceHolder);
            mDrawThread.init(getContext());

            mDrawThread.start();

            if (redraw)
                redraw();
        }
    }

    /**
     * Stop the drawing thread if it exists
     */
    public void stopDrawThread() {
        if (mDrawThread != null) {
            Log.v(TAG, "stopDrawThread");

            mDrawThread.stopThread();//tell thread to finish up

            boolean retry = true;
            while (retry) {
                try {
                    mDrawThread.join();//block current thread until draw thread finishes execution
                    retry = false;
                } catch (InterruptedException e) {}
            }

            mDrawThread = null;
        }
    }

    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        Log.v(TAG, "SURFACE CHANGED");
        mState.mWidth = width;
        mState.mHeight = height;
        redraw();
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "SURFACE CREATED");
        mSurfaceExists = true;
        startDrawThread(false);
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "SURFACE DESTROYED");
        mSurfaceExists = false;
        stopDrawThread();
    }
}
