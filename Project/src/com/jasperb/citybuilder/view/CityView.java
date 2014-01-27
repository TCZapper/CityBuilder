/**
 * 
 */
package com.jasperb.citybuilder.view;

import java.util.LinkedList;

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
import com.jasperb.citybuilder.util.Constant.CITY_VIEW_MODES;
import com.jasperb.citybuilder.util.Constant.TERRAIN;
import com.jasperb.citybuilder.util.Constant.TERRAIN_TOOLS;
import com.jasperb.citybuilder.util.TerrainEdit;

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
    private DrawThread mDrawThread = null;
    private SurfaceHolder mSurfaceHolder = null;
    private boolean mSurfaceExists = false;
    private boolean mInputActive = false;
    private boolean mInputClick = false;
    private boolean mAllocated = false;
    private int mTerrainTypeSelected = TERRAIN.GRASS;
    private LinkedList<TerrainEdit> mTerrainEdits = new LinkedList<TerrainEdit>();

    public boolean isEverythingAllocated() {
        return mAllocated;
    }

    public void setCityModel(CityModel model) {
        synchronized (mState) {
            mState.mCityModel = model;
        }
    }

    public int getTerrainTypeSelected() {
        return mTerrainTypeSelected;
    }

    public void setTerrainTypeSelected(int terrain) {
        mTerrainTypeSelected = terrain;
    }

    public boolean getDrawGridLines() {
        synchronized (mState) {
            return mState.mDrawGridLines;
        }
    }

    public void setDrawGridLines(boolean drawGridLines) {
        synchronized (mState) {
            mState.mDrawGridLines = drawGridLines;
        }
    }

    public int getMode() {
        synchronized (mState) {
            return mState.mMode;
        }
    }

    public void setMode(int mode) {
        synchronized (mState) {
            mState.mMode = mode;
        }
    }

    public int getTool() {
        synchronized (mState) {
            return mState.mTool;
        }
    }

    public void setTool(int tool) {
        synchronized (mState) {
            mState.mTool = tool;
        }
    }

    public float getFocusRow() {
        synchronized (mState) {
            return mState.mFocusRow;
        }
    }

    public float getFocusCol() {
        synchronized (mState) {
            return mState.mFocusCol;
        }
    }

    public void setFocusCoords(float row, float col) {
        synchronized (mState) {
            mState.mFocusRow = row;
            mState.mFocusCol = col;
        }
    }

    public float getScaleFactor() {
        synchronized (mState) {
            return mState.getScaleFactor();
        }
    }

    public void setScaleFactor(float scaleFactor) {
        synchronized (mState) {
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
        synchronized (mTerrainEdits) {
            for (TerrainEdit edit : mTerrainEdits)
                edit.setTerrain(mState);
        }

        // The purpose of this method is to be used by the draw thread to update the CityView's state and then retrieve that state
        // This lets us easily continuously update the CityView's state and keep the update rate synced with the FPS of the draw thread
        synchronized (mState) {
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
        synchronized (mState) {
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

        synchronized (mState) {
            mScroller = null;
        }
        mScaleDetector = null;
        mDetector = null;
    }

    public void addTerrainEdit(TerrainEdit edit) {
        synchronized (mTerrainEdits) {
            mTerrainEdits.add(edit);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        synchronized (mState) {//Keep track of when user is supplying input
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mInputActive = true;
                mInputClick = true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                mInputActive = false;
            }
        }
        // Let the GestureDetectors interpret this event
        boolean result = mDetector.onTouchEvent(event);
        mScaleDetector.onTouchEvent(event);
        if (!result) {
            if (event.getAction() == MotionEvent.ACTION_UP && mInputClick) {
                Log.d(TAG, "RELEASE");
                if (mState.mMode == CITY_VIEW_MODES.EDIT_TERRAIN && mState.mTool == TERRAIN_TOOLS.BRUSH) {
                    int posX = (int) event.getX() - mState.getOriginX();
                    int posY = (int) event.getY() - mState.getOriginY();
                    int row = (int) mState.realToIsoRowUpscaling(posX, posY);
                    int col = (int) mState.realToIsoColUpscaling(posX, posY);
                    if (mState.isTileValid(row, col))
                        addTerrainEdit(new TerrainEdit(row, col, mTerrainTypeSelected));
                }
            }
        }
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
            synchronized (mState) {
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
            //e1 defines start of the scroll, e2 is the destination of the scroll
            if (e2.getPointerCount() == 1) {
                mInputClick = false;
                synchronized (mState) {
                    if (mState.mMode == CITY_VIEW_MODES.VIEW || mState.mMode == CITY_VIEW_MODES.EDIT_TERRAIN && mState.mTool == TERRAIN_TOOLS.SELECT) {
                        mState.mFocusRow += mState.realToIsoRowUpscaling(Math.round(distanceX), Math.round(distanceY));
                        mState.mFocusCol += mState.realToIsoColUpscaling(Math.round(distanceX), Math.round(distanceY));
                        constrainFocus();
                    } else {
//                    Log.d(TAG,"SCROLL: " + e1.getX() + " : " + e1.getY() + " --- " + e2.getX() + " : " + e2.getY() + " --- " + distanceX + " : " + distanceY);
                        int posX = (int) e2.getX() - mState.getOriginX();
                        int posY = (int) e2.getY() - mState.getOriginY();
                        int row = (int) mState.realToIsoRowUpscaling(posX, posY);
                        int col = (int) mState.realToIsoColUpscaling(posX, posY);
                        if (mState.isTileValid(row, col))
                            addTerrainEdit(new TerrainEdit(row, col, mTerrainTypeSelected));
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!(mState.mMode == CITY_VIEW_MODES.EDIT_TERRAIN && mState.mTool == TERRAIN_TOOLS.BRUSH)) {
                fling((int) -velocityX, (int) -velocityY);
            }
            return true;
        }

    }

    private boolean fling(int velocityX, int velocityY) {
        mInputClick = false;

        int minRow = 0;
        int minCol = 0;
        int maxRow = mState.mCityModel.getHeight() * Constant.TILE_WIDTH - 1;
        int maxCol = mState.mCityModel.getWidth() * Constant.TILE_WIDTH - 1;
        synchronized (mState) {
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
            mInputClick = false;
            synchronized (mState) {
                float scaleFactor = mState.getScaleFactor() * scaleGestureDetector.getScaleFactor();
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
        synchronized (mState) {
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
