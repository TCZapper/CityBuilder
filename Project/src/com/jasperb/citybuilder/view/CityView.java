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
import android.widget.OverScroller;

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
    public OverScroller mScroller = null;
    private CityViewState mState = new CityViewState();
    private final Object mStateLock = new Object();
    private DrawThread mDrawThread = null;
    private SurfaceHolder mSurfaceHolder = null;
    private boolean mSurfaceExists = false;
    private boolean mInputActive = false;
    private boolean mAllocated = false;

    public boolean isEverythingAllocated() {
        return mAllocated;
    }

    public void setCityModel(CityModel model) {
        synchronized (mStateLock) {
            mState.mCityModel = model;
        }
    }

    public boolean getDrawGridLines() {
        synchronized (mStateLock) {
            return mState.mDrawGridLines;
        }
    }

    public void setDrawGridLines(boolean drawGridLines) {
        synchronized (mStateLock) {
            mState.mDrawGridLines = drawGridLines;
        }
    }

    public float getFocusRow() {
        synchronized (mStateLock) {
            return mState.mFocusRow;
        }
    }

    public float getFocusCol() {
        synchronized (mStateLock) {
            return mState.mFocusCol;
        }
    }

    public void setFocusCoords(float row, float col) {
        synchronized (mStateLock) {
            mState.mFocusRow = row;
            mState.mFocusCol = col;
        }
    }

    public float getScaleFactor() {
        synchronized (mStateLock) {
            return mState.getScaleFactor();
        }
    }

    public void setScaleFactor(float scaleFactor) {
        synchronized (mStateLock) {
            mState.setScaleFactor(scaleFactor);
        }
    }

    /**
     * Updates the CityView's state and then copies the state into the passed argument
     * 
     * @param to
     *            the object to copy the state into
     */
    protected void updateAndCopyState(CityViewState to) {
        // The purpose of this method is to be used by the draw thread to update the CityView's state and then retrieve that state
        // This lets us easily continuously update the CityView's state and keep the update rate synced with the FPS of the draw thread
        synchronized (mStateLock) {
            if (mScroller == null)// Happens if cleanup was called but the draw thread is still active
                return;

            // Update the focus based off an active scroller
            // Or if the user input is not active and we are out of bounds, create a new scroller to put us in bounds
            if (!mScroller.isFinished()) {
                mScroller.computeScrollOffset();
                mState.mFocusRow = mScroller.getCurrX() / Constant.TILE_WIDTH;
                mState.mFocusCol = mScroller.getCurrY() / Constant.TILE_WIDTH;
            } else if (!mInputActive && !mState.isTileValid(mState.mFocusRow, mState.mFocusCol)) {
                int startRow = Math.round(mState.mFocusRow * Constant.TILE_WIDTH);
                int startCol = Math.round(mState.mFocusCol * Constant.TILE_WIDTH);
                int endRow = startRow;
                int endCol = startCol;

                if (mState.mFocusRow < 0) {
                    endRow = 0;
                } else if (mState.mFocusRow >= mState.mCityModel.getHeight()) {
                    endRow = mState.mCityModel.getHeight() * Constant.TILE_WIDTH - 1;
                }
                if (mState.mFocusCol < 0) {
                    endCol = 0;
                } else if (mState.mFocusCol >= mState.mCityModel.getWidth()) {
                    endCol = mState.mCityModel.getWidth() * Constant.TILE_WIDTH - 1;
                }
                mScroller.startScroll(startRow, startCol, endRow - startRow, endCol - startCol, Constant.FOCUS_CONSTRAIN_TIME);
            }

            to.copyFrom(mState);
        }
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
        synchronized (mStateLock) {
            mScroller = new OverScroller(getContext());
        }
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

        synchronized (mStateLock) {
            mScroller = null;
        }
        mScaleDetector = null;
        mDetector = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        synchronized (mStateLock) {//Keep track of when user is supplying input
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mInputActive = true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                mInputActive = false;
            }
        }
        // Let the GestureDetectors interpret this event
        boolean result = mDetector.onTouchEvent(event);
        mScaleDetector.onTouchEvent(event);
        return result;
    }

    /**
     * Constrains the focus to within the extended boundaries permitted
     */
    private void constrainFocus() {
        if (mState.mFocusRow < -Constant.FOCUS_EXTENDED_BOUNDARY) {
            mState.mFocusRow = -Constant.FOCUS_EXTENDED_BOUNDARY;
        } else if (mState.mFocusRow > mState.mCityModel.getHeight() + Constant.FOCUS_EXTENDED_BOUNDARY) {
            mState.mFocusRow = mState.mCityModel.getHeight() + Constant.FOCUS_EXTENDED_BOUNDARY;
        }
        if (mState.mFocusCol < -Constant.FOCUS_EXTENDED_BOUNDARY) {
            mState.mFocusCol = -Constant.FOCUS_EXTENDED_BOUNDARY;
        } else if (mState.mFocusCol > mState.mCityModel.getWidth() + Constant.FOCUS_EXTENDED_BOUNDARY) {
            mState.mFocusCol = mState.mCityModel.getWidth() + Constant.FOCUS_EXTENDED_BOUNDARY;
        }
    }

    /**
     * The gesture listener, used for handling scroll and fling gestures (for panning)
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            synchronized (mStateLock) {
                if (!mScroller.isFinished()) {
                    mScroller.computeScrollOffset();//compute current offset before forcing finish
                    mState.mFocusRow = mScroller.getCurrX() / Constant.TILE_WIDTH;
                    mState.mFocusCol = mScroller.getCurrY() / Constant.TILE_WIDTH;
                    mScroller.forceFinished(true);
                }
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            synchronized (mStateLock) {
                mState.mFocusRow += mState.realToIsoRowUpscaling(Math.round(distanceX), Math.round(distanceY));
                mState.mFocusCol += mState.realToIsoColUpscaling(Math.round(distanceX), Math.round(distanceY));
                constrainFocus();
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            fling((int) -velocityX, (int) -velocityY);
            return true;
        }

    }

    private boolean fling(int velocityX, int velocityY) {
        int minRow = 0;
        int minCol = 0;
        int maxRow = mState.mCityModel.getHeight() * Constant.TILE_WIDTH - 1;
        int maxCol = mState.mCityModel.getWidth() * Constant.TILE_WIDTH - 1;
        synchronized (mStateLock) {
            mScroller.fling(
                    Math.round(mState.mFocusRow * Constant.TILE_WIDTH),
                    Math.round(mState.mFocusCol * Constant.TILE_WIDTH),
                    Math.round(mState.realToIsoRowUpscaling(velocityX, velocityY) * Constant.TILE_WIDTH),
                    Math.round(mState.realToIsoColUpscaling(velocityX, velocityY) * Constant.TILE_WIDTH),
                    minRow, maxRow, minCol, maxCol,
                    Constant.FOCUS_EXTENDED_BOUNDARY * Constant.TILE_WIDTH, Constant.FOCUS_EXTENDED_BOUNDARY * Constant.TILE_WIDTH);
        }
        return true;
    }

    /**
     * The scale listener, used for handling multi-finger scale gestures.
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            float scaleFactor = mState.getScaleFactor() * scaleGestureDetector.getScaleFactor();

            synchronized (mStateLock) {
                // Don't let the object get too small or too large.
                mState.setScaleFactor(Math.max(Constant.MINIMUM_SCALE_FACTOR, Math.min(scaleFactor, Constant.MAXIMUM_SCALE_FACTOR)));
            }
            return true;
        }

    }

    /**
     * Start the drawing thread if possible
     */
    public void startDrawThread() {
        // In some instances, the surface persists even when the view is stopped. We still stop the draw thread.
        // There's no point starting the thread before the surface is created.
        if (mSurfaceExists && mDrawThread == null) {
            Log.v(TAG, "startDrawThread");

            mDrawThread = new DrawThread(mSurfaceHolder, this);
            mDrawThread.init(getContext());

            mDrawThread.start();
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

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        Log.v(TAG, "SURFACE CHANGED");
        synchronized (mStateLock) {
            mState.mWidth = width;
            mState.mHeight = height;
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "SURFACE CREATED");
        mSurfaceExists = true;
        startDrawThread();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // WARNING: after this method returns, the Surface/Canvas must never be touched again!
        Log.v(TAG, "SURFACE DESTROYED");
        mSurfaceExists = false;
        stopDrawThread();
    }
}
