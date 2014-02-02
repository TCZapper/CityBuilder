/**
 * 
 */
package com.jasperb.citybuilder;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.jasperb.citybuilder.util.Constant;
import com.jasperb.citybuilder.util.Constant.BRUSH_TYPES;
import com.jasperb.citybuilder.util.Constant.CITY_VIEW_MODES;
import com.jasperb.citybuilder.util.Constant.TERRAIN;
import com.jasperb.citybuilder.util.Constant.TERRAIN_TOOLS;
import com.jasperb.citybuilder.util.TileBitmaps;
import com.jasperb.citybuilder.view.CityViewState;

/**
 * @author Jasper
 * 
 */
public class OverlayController {
    /**
     * String used for identifying this class.
     */
    public static final String TAG = "OverlayCtrl";

    private CityViewState mState = null;

    private MainViewClickListener mClickListener = null;
    private MainViewTouchListener mTouchListener = null;

    //Main View buttons and layouts
    public ImageView mGridButton, mTerrainButton;
    public ImageView mPaintButton, mSelectButton, mTileSyleIcon;
    public ImageView mBrushSquare1x1, mBrushSquare3x3, mBrushSquare5x5;
    public ImageView mLeftButton, mUpButton, mDownButton, mRightButton;
    public FrameLayout mTileStyleButton;
    public RelativeLayout mMoveButtons;
    public LinearLayout mTerrainTools, mBrushTools;

    /**
     * Initialize and allocate the necessary components of the view, except those related to the drawing thread
     */
    public void init(Context context, CityViewState state) {
        mState = state;

        mClickListener = new MainViewClickListener();
        mGridButton.setOnClickListener(mClickListener);
        mTerrainButton.setOnClickListener(mClickListener);
        mTileStyleButton.setOnClickListener(mClickListener);
        mPaintButton.setOnClickListener(mClickListener);
        mSelectButton.setOnClickListener(mClickListener);
        mBrushSquare1x1.setOnClickListener(mClickListener);
        mBrushSquare3x3.setOnClickListener(mClickListener);
        mBrushSquare5x5.setOnClickListener(mClickListener);

        mTouchListener = new MainViewTouchListener();
        mLeftButton.setOnTouchListener(mTouchListener);
        mUpButton.setOnTouchListener(mTouchListener);
        mDownButton.setOnTouchListener(mTouchListener);
        mRightButton.setOnTouchListener(mTouchListener);
    }

    /**
     * Cleanup the components of the view allocated by init()
     */
    public void cleanup() {
        mClickListener = null;
        mTouchListener = null;
    }

    private class MainViewClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (v.equals(mGridButton)) {
                synchronized (mState) {
                    mState.mDrawGridLines = !mState.mDrawGridLines;
                }
            } else if (v.equals(mTerrainButton)) {
                if (mState.mMode == CITY_VIEW_MODES.EDIT_TERRAIN) {
                    synchronized (mState) {
                        mState.mMode = CITY_VIEW_MODES.VIEW;
                    }
                    mTerrainTools.setVisibility(View.GONE);
                    mMoveButtons.setVisibility(View.GONE);
                } else {
                    synchronized (mState) {
                        mState.mMode = CITY_VIEW_MODES.EDIT_TERRAIN;
                    }
                    mTerrainTools.setVisibility(View.VISIBLE);
                    if (mState.mTool == TERRAIN_TOOLS.BRUSH)
                        mMoveButtons.setVisibility(View.VISIBLE);
                }
            } else if (v.equals(mTileStyleButton)) {
                int terrain = mState.mTerrainTypeSelected + 1;
                if (terrain == TERRAIN.count) {
                    terrain = 0;
                }
                synchronized (mState) {
                    mState.mTerrainTypeSelected = terrain;
                }
                mTileSyleIcon.setImageBitmap(TileBitmaps.getFullBitmap(terrain));
            } else if (v.equals(mPaintButton)) {
                synchronized (mState) {
                    mState.mTool = TERRAIN_TOOLS.BRUSH;
                    mMoveButtons.setVisibility(View.VISIBLE);
                }
            } else if (v.equals(mSelectButton)) {
                synchronized (mState) {
                    mState.mTool = TERRAIN_TOOLS.SELECT;
                    mMoveButtons.setVisibility(View.GONE);
                    mState.resetFirstSelectedTile();
                    mState.resetSecondSelectedTile();
                }
            } else if (v.equals(mBrushSquare1x1)) {
                synchronized (mState) {
                    mState.mBrushType = BRUSH_TYPES.SQUARE1X1;
                }
            } else if (v.equals(mBrushSquare3x3)) {
                synchronized (mState) {
                    mState.mBrushType = BRUSH_TYPES.SQUARE3X3;
                }
            } else if (v.equals(mBrushSquare5x5)) {
                synchronized (mState) {
                    mState.mBrushType = BRUSH_TYPES.SQUARE5X5;
                }
            }
        }
    };

    private class MainViewTouchListener implements OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (v.equals(mLeftButton)) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    synchronized (mState) {
                        float min = Math.min(mState.mCityModel.getHeight() - mState.mFocusRow, mState.mFocusCol);
                        int duration = (int) min * Constant.MOVE_BUTTON_DURATION;
                        mState.mScroller.startScroll((int) mState.mFocusRow * Constant.TILE_WIDTH, (int) mState.mFocusCol
                                * Constant.TILE_WIDTH,
                                (int) (min) * Constant.TILE_WIDTH, (int) (-min) * Constant.TILE_WIDTH,
                                duration);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    synchronized (mState) {
                        mState.forceStopScroller();
                    }
                }
            } else if (v.equals(mUpButton)) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    synchronized (mState) {
                        float min = Math.min(mState.mFocusRow, mState.mFocusCol);
                        int duration = (int) min * Constant.MOVE_BUTTON_DURATION;
                        mState.mScroller.startScroll((int) mState.mFocusRow * Constant.TILE_WIDTH, (int) mState.mFocusCol
                                * Constant.TILE_WIDTH,
                                (int) (-min) * Constant.TILE_WIDTH, (int) (-min) * Constant.TILE_WIDTH,
                                duration);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    synchronized (mState) {
                        mState.forceStopScroller();
                    }
                }
            } else if (v.equals(mDownButton)) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    synchronized (mState) {
                        float min = Math.min(mState.mCityModel.getHeight() - mState.mFocusRow, mState.mCityModel.getWidth()
                                - mState.mFocusCol);
                        int duration = (int) min * Constant.MOVE_BUTTON_DURATION;
                        mState.mScroller.startScroll((int) mState.mFocusRow * Constant.TILE_WIDTH, (int) mState.mFocusCol
                                * Constant.TILE_WIDTH,
                                (int) (min) * Constant.TILE_WIDTH, (int) (min) * Constant.TILE_WIDTH,
                                duration);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    synchronized (mState) {
                        mState.forceStopScroller();
                    }
                }
            } else if (v.equals(mRightButton)) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    synchronized (mState) {
                        float min = Math.min(mState.mFocusRow, mState.mCityModel.getWidth() - mState.mFocusCol);
                        int duration = (int) min * Constant.MOVE_BUTTON_DURATION;
                        mState.mScroller.startScroll((int) mState.mFocusRow * Constant.TILE_WIDTH, (int) mState.mFocusCol
                                * Constant.TILE_WIDTH,
                                (int) (-min) * Constant.TILE_WIDTH, (int) (min) * Constant.TILE_WIDTH,
                                duration);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    synchronized (mState) {
                        mState.forceStopScroller();
                    }
                }
            }
            return false;
        }
    };
}
