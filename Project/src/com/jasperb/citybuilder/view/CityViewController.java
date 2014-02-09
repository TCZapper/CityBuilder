/**
 * 
 */
package com.jasperb.citybuilder.view;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.jasperb.citybuilder.util.Constant;
import com.jasperb.citybuilder.util.Constant.BRUSH_TYPES;
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
        mState = null;
    }

    /**
     * Touch events from the city view get passed to this
     */
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
        if (!result && event.getAction() == MotionEvent.ACTION_UP && mInputClick) {
            if (mState.mMode == CITY_VIEW_MODES.EDIT_TERRAIN) {
                if (mState.mTool == TERRAIN_TOOLS.BRUSH) {
                    paintWithBrush(event);
                } else if (mState.mTool == TERRAIN_TOOLS.SELECT) {
                    synchronized (mState) {
                        int posX = (int) event.getX() - mState.getOriginX();
                        int posY = (int) event.getY() - mState.getOriginY();
                        int row = (int) mState.realToIsoRowUpscaling(posX, posY);
                        int col = (int) mState.realToIsoColUpscaling(posX, posY);
                        if (row == mState.mFirstSelectedRow && col == mState.mFirstSelectedCol) {
                            mState.mSelectingFirstTile = true;
                        } else if (row == mState.mSecondSelectedRow && col == mState.mSecondSelectedCol) {
                            mState.mSelectingFirstTile = false;
                        } else if (row == mState.mFirstSelectedRow && col == mState.mSecondSelectedCol) {
                            int temp = mState.mFirstSelectedRow;
                            mState.mFirstSelectedRow = mState.mSecondSelectedRow;
                            mState.mSecondSelectedRow = temp;
                            mState.mSelectingFirstTile = false;
                        } else if (row == mState.mSecondSelectedRow && col == mState.mFirstSelectedCol) {
                            int temp = mState.mFirstSelectedRow;
                            mState.mFirstSelectedRow = mState.mSecondSelectedRow;
                            mState.mSecondSelectedRow = temp;
                            mState.mSelectingFirstTile = true;
                        } else {
                            if (mState.mSelectingFirstTile) {
                                mState.mFirstSelectedRow = row;
                                mState.mFirstSelectedCol = col;
                                if (mState.mSecondSelectedRow == -1) {
                                    mState.mSelectingFirstTile = false;
                                    mState.notifyOverlay();
                                }
                            } else {
                                mState.mSecondSelectedRow = row;
                                mState.mSecondSelectedCol = col;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Process a touch event as though the user were painting with a brush
     * @param event
     */
    private void paintWithBrush(MotionEvent event) {
        synchronized (mState) {
            int posX = (int) event.getX() - mState.getOriginX();
            int posY = (int) event.getY() - mState.getOriginY();
            int row = (int) mState.realToIsoRowUpscaling(posX, posY);
            int col = (int) mState.realToIsoColUpscaling(posX, posY);
            if (mState.isTileValid(row, col)) {
                if (mState.mBrushType == BRUSH_TYPES.SQUARE1X1) {
                    mState.addTerrainEdit(new TerrainEdit(row, col, mState.mTerrainTypeSelected));
                } else if (mState.mBrushType == BRUSH_TYPES.SQUARE3X3) {
                    int minRow = row - 1;
                    if (minRow < 0)
                        minRow = 0;
                    int minCol = col - 1;
                    if (minCol < 0)
                        minCol = 0;
                    int maxRow = row + 1;
                    if (maxRow > mState.mCityModel.getHeight())
                        maxRow = 0;
                    int maxCol = col + 1;
                    if (maxCol > mState.mCityModel.getWidth())
                        maxCol = 0;
                    mState.addTerrainEdit(new TerrainEdit(minRow, minCol, maxRow, maxCol, mState.mTerrainTypeSelected));
                } else if (mState.mBrushType == BRUSH_TYPES.SQUARE5X5) {
                    int minRow = row - 2;
                    if (minRow < 0)
                        minRow = 0;
                    int minCol = col - 2;
                    if (minCol < 0)
                        minCol = 0;
                    int maxRow = row + 2;
                    if (maxRow > mState.mCityModel.getHeight())
                        maxRow = 0;
                    int maxCol = col + 2;
                    if (maxCol > mState.mCityModel.getWidth())
                        maxCol = 0;
                    mState.addTerrainEdit(new TerrainEdit(minRow, minCol, maxRow, maxCol, mState.mTerrainTypeSelected));
                }
            }
        }
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
                    paintWithBrush(e2);
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
