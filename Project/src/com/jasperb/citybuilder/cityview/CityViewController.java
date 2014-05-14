/**
 * 
 */
package com.jasperb.citybuilder.cityview;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.jasperb.citybuilder.Constant;
import com.jasperb.citybuilder.SharedState;
import com.jasperb.citybuilder.CityModel.ObjectSlice;
import com.jasperb.citybuilder.Constant.BRUSH_TYPES;
import com.jasperb.citybuilder.Constant.CITY_VIEW_MODES;
import com.jasperb.citybuilder.Constant.OBJECTS;
import com.jasperb.citybuilder.Constant.OBJECT_TOOLS;
import com.jasperb.citybuilder.Constant.TERRAIN_TOOLS;
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

    private SharedState mState = null;

    private ScaleGestureDetector mScaleDetector = null;
    private GestureDetector mPanDetector = null;
    private boolean mInputClick = false;
    private int mLastRow = -1, mLastCol = -1;

    /**
     * Initialize and allocate the necessary components of the view, except those related to the drawing thread
     */
    public void init(Context context, SharedState state) {
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
                mState.UIS_InputActive = true;
                mInputClick = true;
                mLastRow = -1;
                mLastCol = -1;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                mState.UIS_InputActive = false;
            }
        }
        // Let the GestureDetectors interpret this event
        boolean result = mPanDetector.onTouchEvent(event);
        mScaleDetector.onTouchEvent(event);
        if (!result && event.getAction() == MotionEvent.ACTION_UP && mInputClick) {
            if (mState.UIS_Mode == CITY_VIEW_MODES.EDIT_TERRAIN) {
                if (mState.UIS_Tool == TERRAIN_TOOLS.BRUSH) {
                    paintWithBrush(event, false);
                } else if (mState.UIS_Tool == TERRAIN_TOOLS.SELECT) {
                    synchronized (mState) {
                        int posX = (int) event.getX() - mState.getOriginX();
                        int posY = (int) event.getY() - mState.getOriginY();
                        int row = (int) mState.realToIsoRowUpscaling(posX, posY);
                        int col = (int) mState.realToIsoColUpscaling(posX, posY);
                        if (mState.isTileValid(row, col)) {
                            if (row == mState.UIS_FirstSelectedRow && col == mState.UIS_FirstSelectedCol) {
                                mState.UIS_SelectingFirstTile = true;
                            } else if (row == mState.UIS_SecondSelectedRow && col == mState.UIS_SecondSelectedCol) {
                                mState.UIS_SelectingFirstTile = false;
                            } else if (row == mState.UIS_FirstSelectedRow && col == mState.UIS_SecondSelectedCol) {
                                int temp = mState.UIS_FirstSelectedRow;
                                mState.UIS_FirstSelectedRow = mState.UIS_SecondSelectedRow;
                                mState.UIS_SecondSelectedRow = temp;
                                mState.UIS_SelectingFirstTile = false;
                            } else if (row == mState.UIS_SecondSelectedRow && col == mState.UIS_FirstSelectedCol) {
                                int temp = mState.UIS_FirstSelectedRow;
                                mState.UIS_FirstSelectedRow = mState.UIS_SecondSelectedRow;
                                mState.UIS_SecondSelectedRow = temp;
                                mState.UIS_SelectingFirstTile = true;
                            } else {
                                if (mState.UIS_SelectingFirstTile) {
                                    mState.UIS_FirstSelectedRow = row;
                                    mState.UIS_FirstSelectedCol = col;
                                    if (mState.UIS_SecondSelectedRow == -1) {
                                        mState.UIS_SelectingFirstTile = false;
                                        mState.notifyOverlay();
                                    }
                                } else {
                                    mState.UIS_SecondSelectedRow = row;
                                    mState.UIS_SecondSelectedCol = col;
                                }
                            }
                        }
                    }
                } else if (mState.UIS_Tool == TERRAIN_TOOLS.EYEDROPPER) {
                    synchronized (mState) {
                        int posX = (int) event.getX() - mState.getOriginX();
                        int posY = (int) event.getY() - mState.getOriginY();
                        int row = (int) mState.realToIsoRowUpscaling(posX, posY);
                        int col = (int) mState.realToIsoColUpscaling(posX, posY);
                        if (mState.isTileValid(row, col)) {
                            mState.UIS_SelectedTerrainType = mState.UIS_CityModel.getTerrain(row, col);
                            mState.UIS_Tool = mState.UIS_PreviousTool;
                            mState.notifyOverlay();
                        }
                    }
                }
            } else if (mState.UIS_Mode == CITY_VIEW_MODES.EDIT_OBJECTS) {
                if (mState.UIS_Tool == OBJECT_TOOLS.NEW) {
                    synchronized (mState) {
                        int posX = (int) event.getX() - mState.getOriginX();
                        int posY = (int) event.getY() - mState.getOriginY();
                        int row = (int) mState.realToIsoRowUpscaling(posX, posY);
                        int col = (int) mState.realToIsoColUpscaling(posX, posY);
                        if (mState.isTileValid(row, col)) {
                            row = row + 1 - OBJECTS.objectNumRows[mState.UIS_SelectedObjectType];
                            col = col + 1 - OBJECTS.objectNumColumns[mState.UIS_SelectedObjectType];
                            if (mState.isTileValid(row, col)) {
                                mState.UIS_DestRow = row;
                                mState.UIS_DestCol = col;
                                mState.notifyOverlay();
                            }
                        }
                    }
                } else if (mState.UIS_Tool == OBJECT_TOOLS.SELECT) {
                    if (mState.UIS_SelectedObjectID == -1) {
                        synchronized (mState) {
                            int posX = (int) event.getX() - mState.getOriginX();
                            int posY = (int) event.getY() - mState.getOriginY();
                            int row = (int) mState.realToIsoRowUpscaling(posX, posY);
                            int col = (int) mState.realToIsoColUpscaling(posX, posY);
                            if (mState.isTileValid(row, col)) {
                                int id = mState.UIS_CityModel.getObjectID(row, col);
                                if (id != -1) {
                                    mState.UIS_SelectedObjectID = id;
                                    ObjectSlice slice = mState.UIS_CityModel.getObjectSlice(id);
                                    mState.removeObject(id, true);
                                    mState.UIS_SelectedObjectType = slice.type;
                                    mState.UIS_DestRow = slice.row + 1 - OBJECTS.objectNumRows[slice.type];
                                    mState.UIS_DestCol = slice.col;
                                    mState.UIS_OrigRow = mState.UIS_DestRow;
                                    mState.UIS_OrigCol = mState.UIS_DestCol;
                                    mState.notifyOverlay();
                                }
                            }
                        }
                    } else {
                        synchronized (mState) {
                            int posX = (int) event.getX() - mState.getOriginX();
                            int posY = (int) event.getY() - mState.getOriginY();
                            int row = (int) mState.realToIsoRowUpscaling(posX, posY);
                            int col = (int) mState.realToIsoColUpscaling(posX, posY);
                            if (mState.isTileValid(row, col)) {
                                row = row + 1 - OBJECTS.objectNumRows[mState.UIS_SelectedObjectType];
                                col = col + 1 - OBJECTS.objectNumColumns[mState.UIS_SelectedObjectType];
                                if (mState.isTileValid(row, col)) {
                                    mState.UIS_DestRow = row;
                                    mState.UIS_DestCol = col;
                                }
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
     * 
     * @param event
     */
    private void paintWithBrush(MotionEvent event, boolean drag) {
        synchronized (mState) {
            int posX = (int) event.getX() - mState.getOriginX();
            int posY = (int) event.getY() - mState.getOriginY();
            int row = (int) mState.realToIsoRowUpscaling(posX, posY);
            int col = (int) mState.realToIsoColUpscaling(posX, posY);
            if (mState.isTileValid(row, col) && (row != mLastRow || col != mLastCol)) {
                mLastRow = row;
                mLastCol = col;
                if (mState.UIS_BrushType == BRUSH_TYPES.SQUARE1X1) {
                    mState.addTerrainEdit(new TerrainEdit(row, col, mState.UIS_SelectedTerrainType, mState.NS_DrawWithBlending));
                } else if (mState.UIS_BrushType == BRUSH_TYPES.SQUARE3X3) {
                    int minRow = row - 1;
                    if (minRow < 0)
                        minRow = 0;
                    int minCol = col - 1;
                    if (minCol < 0)
                        minCol = 0;
                    int maxRow = row + 1;
                    if (maxRow > mState.UIS_CityModel.getHeight())
                        maxRow = 0;
                    int maxCol = col + 1;
                    if (maxCol > mState.UIS_CityModel.getWidth())
                        maxCol = 0;
                    mState.addTerrainEdit(new TerrainEdit(minRow, minCol, maxRow, maxCol, mState.UIS_SelectedTerrainType,
                            mState.NS_DrawWithBlending));
                } else if (mState.UIS_BrushType == BRUSH_TYPES.SQUARE5X5) {
                    int minRow = row - 2;
                    if (minRow < 0)
                        minRow = 0;
                    int minCol = col - 2;
                    if (minCol < 0)
                        minCol = 0;
                    int maxRow = row + 2;
                    if (maxRow > mState.UIS_CityModel.getHeight())
                        maxRow = 0;
                    int maxCol = col + 2;
                    if (maxCol > mState.UIS_CityModel.getWidth())
                        maxCol = 0;
                    mState.addTerrainEdit(new TerrainEdit(minRow, minCol, maxRow, maxCol, mState.UIS_SelectedTerrainType,
                            mState.NS_DrawWithBlending));
                }
            }
        }
    }

    /**
     * Constrains the focus to within the extended boundaries permitted
     */
    private void constrainFocus() {
        if (mState.TS_FocusRow < -Constant.FOCUS_EXTENDED_BOUNDARY) {
            mState.TS_FocusRow = -Constant.FOCUS_EXTENDED_BOUNDARY;
        } else if (mState.TS_FocusRow > mState.UIS_CityModel.getHeight() + Constant.FOCUS_EXTENDED_BOUNDARY) {
            mState.TS_FocusRow = mState.UIS_CityModel.getHeight() + Constant.FOCUS_EXTENDED_BOUNDARY;
        }
        if (mState.TS_FocusCol < -Constant.FOCUS_EXTENDED_BOUNDARY) {
            mState.TS_FocusCol = -Constant.FOCUS_EXTENDED_BOUNDARY;
        } else if (mState.TS_FocusCol > mState.UIS_CityModel.getWidth() + Constant.FOCUS_EXTENDED_BOUNDARY) {
            mState.TS_FocusCol = mState.UIS_CityModel.getWidth() + Constant.FOCUS_EXTENDED_BOUNDARY;
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
                if (mState.UIS_Mode == CITY_VIEW_MODES.EDIT_TERRAIN && mState.UIS_Tool == TERRAIN_TOOLS.BRUSH) {
//                  Log.d(TAG,"SCROLL: " + e1.getX() + " : " + e1.getY() + " --- " + e2.getX() + " : " + e2.getY() + " --- " + distanceX + " : " + distanceY);
                    paintWithBrush(e2, true);
                } else {
                    synchronized (mState) {
                        mState.TS_FocusRow += mState.realToIsoRowUpscaling(Math.round(distanceX), Math.round(distanceY));
                        mState.TS_FocusCol += mState.realToIsoColUpscaling(Math.round(distanceX), Math.round(distanceY));
                        constrainFocus();
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!(mState.UIS_Mode == CITY_VIEW_MODES.EDIT_TERRAIN && mState.UIS_Tool == TERRAIN_TOOLS.BRUSH)) {
                mInputClick = false;

                int minRow = 0;
                int minCol = 0;
                int maxRow = mState.UIS_CityModel.getHeight() * Constant.TILE_WIDTH - 1;
                int maxCol = mState.UIS_CityModel.getWidth() * Constant.TILE_WIDTH - 1;
                synchronized (mState) {
                    mState.TS_Scroller.fling(
                            Math.round(mState.TS_FocusRow * Constant.TILE_WIDTH),
                            Math.round(mState.TS_FocusCol * Constant.TILE_WIDTH),
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
