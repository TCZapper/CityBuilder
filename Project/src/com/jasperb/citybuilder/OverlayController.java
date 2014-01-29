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
    public ImageView mGridButton, mTerrainButton, mPaintButton, mSelectButton, mTileSyleIcon;
    public ImageView mLeftButton, mUpButton, mDownButton, mRightButton;
    public FrameLayout mTileStyleButton;
    public RelativeLayout mMoveButtons;
    public LinearLayout mTerrainTools;

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
                }
            }
        }
    };

    private class MainViewTouchListener implements OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (v.equals(mLeftButton)) {
                synchronized (mState) {
                    mState.mFocusRow++;
                    mState.mFocusCol--;
                }
            } else if (v.equals(mUpButton)) {
                synchronized (mState) {
                    mState.mFocusRow--;
                    mState.mFocusCol--;
                }
            } else if (v.equals(mDownButton)) {
                synchronized (mState) {
                    mState.mFocusRow++;
                    mState.mFocusCol++;
                }
            } else if (v.equals(mRightButton)) {
                synchronized (mState) {
                    mState.mFocusRow--;
                    mState.mFocusCol++;
                }
            }
            return false;
        }
    };
}
