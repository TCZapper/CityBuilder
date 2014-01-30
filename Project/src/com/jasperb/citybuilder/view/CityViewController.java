/**
 * 
 */
package com.jasperb.citybuilder.view;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.jasperb.citybuilder.util.Constant;
import com.jasperb.citybuilder.util.Constant.CITY_VIEW_MODES;
import com.jasperb.citybuilder.util.Constant.TERRAIN_TOOLS;
import com.jasperb.citybuilder.util.TerrainEdit;

/**
 * @author Jasper
 * 
 */
public class CityViewController {
    /**
     * String used for identifying this class.
     */
    public static final String TAG = "CityCtrl";

    private CityViewState mState = null;

    private ScaleGestureDetector mScaleDetector = null;
    private GestureDetector mPanDetector = null;
    private boolean mInputClick = false;

    /**
     * Initialize and allocate the necessary components of the view, except those related to the drawing thread
     */
    public void init(Context context, CityViewState state) {
        mState = state;

        mPanDetector = new GestureDetector(context, new GestureListener());

        // Turn off long press--this control doesn't use it, and if long press is enabled, you can't scroll for a bit, pause, then scroll
        // some more (the pause is interpreted as a long press, apparently)
        mPanDetector.setIsLongpressEnabled(false);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    /**
     * Cleanup the components of the view allocated by init()
     */
    public void cleanup() {
        mScaleDetector = null;
        mPanDetector = null;
    }

    public boolean onTouchEvent(MotionEvent event) {
        synchronized (mState) {//Keep track of when user is supplying input
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mState.mInputActive = true;
                mInputClick = true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                mState.mInputActive = false;
            }
        }
        // Let the GestureDetectors interpret this event
        boolean result = mPanDetector.onTouchEvent(event);
        mScaleDetector.onTouchEvent(event);
        if (!result) {
            if (event.getAction() == MotionEvent.ACTION_UP && mInputClick) {
                Log.d(TAG, "RELEASE");
                if (mState.mMode == CITY_VIEW_MODES.EDIT_TERRAIN && mState.mTool == TERRAIN_TOOLS.BRUSH) {
                    synchronized (mState) {
                        int posX = (int) event.getX() - mState.getOriginX();
                        int posY = (int) event.getY() - mState.getOriginY();
                        int row = (int) mState.realToIsoRowUpscaling(posX, posY);
                        int col = (int) mState.realToIsoColUpscaling(posX, posY);
                        if (mState.isTileValid(row, col)) {
                            mState.addTerrainEdit(new TerrainEdit(row, col, mState.mTerrainTypeSelected));
                        }
                    }
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
                mState.forceStopScroller();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            //e1 defines start of the scroll, e2 is the destination of the scroll
            if (e2.getPointerCount() == 1) {
                mInputClick = false;

                if (mState.mMode == CITY_VIEW_MODES.VIEW ||
                        (mState.mMode == CITY_VIEW_MODES.EDIT_TERRAIN && mState.mTool == TERRAIN_TOOLS.SELECT)) {
                    synchronized (mState) {
                        mState.mFocusRow += mState.realToIsoRowUpscaling(Math.round(distanceX), Math.round(distanceY));
                        mState.mFocusCol += mState.realToIsoColUpscaling(Math.round(distanceX), Math.round(distanceY));
                        constrainFocus();
                    }
                } else {
//                    Log.d(TAG,"SCROLL: " + e1.getX() + " : " + e1.getY() + " --- " + e2.getX() + " : " + e2.getY() + " --- " + distanceX + " : " + distanceY);
                    synchronized (mState) {
                        int posX = (int) e2.getX() - mState.getOriginX();
                        int posY = (int) e2.getY() - mState.getOriginY();
                        int row = (int) mState.realToIsoRowUpscaling(posX, posY);
                        int col = (int) mState.realToIsoColUpscaling(posX, posY);
                        if (mState.isTileValid(row, col)) {
                            mState.addTerrainEdit(new TerrainEdit(row, col, mState.mTerrainTypeSelected));
                        }
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
                mInputClick = false;

                int minRow = 0;
                int minCol = 0;
                int maxRow = mState.mCityModel.getHeight() * Constant.TILE_WIDTH - 1;
                int maxCol = mState.mCityModel.getWidth() * Constant.TILE_WIDTH - 1;
                synchronized (mState) {
                    mState.mScroller.fling(
                            Math.round(mState.mFocusRow * Constant.TILE_WIDTH),
                            Math.round(mState.mFocusCol * Constant.TILE_WIDTH),
                            Math.round(mState.realToIsoRowUpscaling((int) -velocityX, (int) -velocityY) * Constant.TILE_WIDTH),
                            Math.round(mState.realToIsoColUpscaling((int) -velocityX, (int) -velocityY) * Constant.TILE_WIDTH),
                            minRow, maxRow, minCol, maxCol,
                            Constant.FOCUS_EXTENDED_BOUNDARY * Constant.TILE_WIDTH, Constant.FOCUS_EXTENDED_BOUNDARY * Constant.TILE_WIDTH);
                }
            }
            return true;
        }

    }

    /**
     * The scale listener, used for handling multi-finger scale gestures.
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            mInputClick = false;
            float scaleFactor = mState.getScaleFactor() * scaleGestureDetector.getScaleFactor();
            // Don't let the object get too small or too large.
            synchronized (mState) {
                mState.setScaleFactor(Math.max(Constant.MINIMUM_SCALE_FACTOR, Math.min(scaleFactor, Constant.MAXIMUM_SCALE_FACTOR)));
            }
            return true;
        }
    }
}
