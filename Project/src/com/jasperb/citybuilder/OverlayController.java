/**
 * 
 */
package com.jasperb.citybuilder;

import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.jasperb.citybuilder.Constant.BRUSH_TYPES;
import com.jasperb.citybuilder.Constant.CITY_VIEW_MODES;
import com.jasperb.citybuilder.Constant.OBJECT_TOOLS;
import com.jasperb.citybuilder.Constant.TERRAIN_TOOLS;
import com.jasperb.citybuilder.dialog.GridViewDialogFragment;
import com.jasperb.citybuilder.util.Observer;
import com.jasperb.citybuilder.util.TileBitmaps;

/**
 * @author Jasper
 * 
 */
public class OverlayController implements Observer {
    /**
     * String used for identifying this class.
     */
    public static final String TAG = "OverlayCtrl";

    private SharedState mState = null;

    private MainViewClickListener mClickListener = null;
    private MainViewTouchListener mTouchListener = null;

    //Main View buttons and layouts
    public ImageView mGridButton, mTerrainButton, mObjectsButton, mMenuButton;
    public ImageView mPaintButton, mSelectTerrainButton, mTileSyleIcon, mBlendButton, mEyedropperButton;
    public ImageView mBrushSquare1x1, mBrushSquare3x3, mBrushSquare5x5;
    public ImageView mLeftButton, mUpButton, mDownButton, mRightButton;
    public ImageView mAcceptButton, mCancelButton, mUndoButton, mRedoButton, mDeleteButton;
    public ImageView mBuildingsButton, mSelectObjectButton;
    public FrameLayout mTileStyleButton;
    public RelativeLayout mMoveButtons;
    public LinearLayout mTerrainTools, mBrushTools, mGeneralTools, mObjectTools;

    /**
     * Initialize and allocate the necessary components of the view, except those related to the drawing thread
     */
    public void init(Context context, SharedState state) {
        mState = state;

        mClickListener = new MainViewClickListener();
        mGridButton.setOnClickListener(mClickListener);
        mTerrainButton.setOnClickListener(mClickListener);
        mObjectsButton.setOnClickListener(mClickListener);
        mMenuButton.setOnClickListener(mClickListener);
        mTileStyleButton.setOnClickListener(mClickListener);
        mPaintButton.setOnClickListener(mClickListener);
        mSelectTerrainButton.setOnClickListener(mClickListener);
        mEyedropperButton.setOnClickListener(mClickListener);
        mBlendButton.setOnClickListener(mClickListener);
        mBrushSquare1x1.setOnClickListener(mClickListener);
        mBrushSquare3x3.setOnClickListener(mClickListener);
        mBrushSquare5x5.setOnClickListener(mClickListener);
        mAcceptButton.setOnClickListener(mClickListener);
        mCancelButton.setOnClickListener(mClickListener);
        mUndoButton.setOnClickListener(mClickListener);
        mRedoButton.setOnClickListener(mClickListener);
        mDeleteButton.setOnClickListener(mClickListener);
        mBuildingsButton.setOnClickListener(mClickListener);
        mSelectObjectButton.setOnClickListener(mClickListener);

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
        //It's not really worth the effort removing the listeners properly
        mClickListener = null;
        mTouchListener = null;
        mState = null;
    }

    /**
     * Handles click events on the overlay
     */
    private class MainViewClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            int initMode = mState.mMode;
            int initTool = mState.mTool;
            if (v.equals(mGridButton)) {
                synchronized (mState) {
                    mState.mDrawGridLines = !mState.mDrawGridLines;
                }
            } else if (v.equals(mTerrainButton)) {
                synchronized (mState) {
                    if (mState.mMode == CITY_VIEW_MODES.EDIT_TERRAIN) {
                        mState.mMode = CITY_VIEW_MODES.VIEW;
                    } else {
                        mState.mMode = CITY_VIEW_MODES.EDIT_TERRAIN;
                        mState.resetSelectTool();
                    }
                }
            } else if (v.equals(mObjectsButton)) {
                synchronized (mState) {
                    if (mState.mMode == CITY_VIEW_MODES.EDIT_OBJECTS) {
                        mState.mMode = CITY_VIEW_MODES.VIEW;
                    } else {
                        mState.mMode = CITY_VIEW_MODES.EDIT_OBJECTS;
                        mState.mTool = OBJECT_TOOLS.SELECT;
                        mState.resetSelectTool();
                    }
                }
            } else if (v.equals(mMenuButton)) {

            } else if (v.equals(mSelectObjectButton)) {
                synchronized (mState) {
                    mState.mTool = OBJECT_TOOLS.SELECT;
                    mState.mSelectedObjectID = -1;
                    mState.mDestRow = -1;
                    mState.mDestCol = -1;
                }
            } else if (v.equals(mBuildingsButton)) {
                synchronized (mState) {
                    mState.mTool = OBJECT_TOOLS.NEW;
                    mState.mDestRow = -1;
                    mState.mDestCol = -1;
                }
                openDialog(GridViewDialogFragment.TYPE_BUILDINGS);
            } else if (v.equals(mTileStyleButton)) {
                openDialog(GridViewDialogFragment.TYPE_TILES);
            } else if (v.equals(mPaintButton)) {
                synchronized (mState) {
                    mState.mTool = TERRAIN_TOOLS.BRUSH;
                }
            } else if (v.equals(mSelectTerrainButton)) {
                synchronized (mState) {
                    mState.mTool = TERRAIN_TOOLS.SELECT;
                    mState.resetSelectTool();
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
            } else if (v.equals(mAcceptButton)) {
                synchronized (mState) {
                    if (mState.mMode == CITY_VIEW_MODES.EDIT_TERRAIN && mState.mTool == TERRAIN_TOOLS.SELECT) {
                        mState.addSelectedTerrainEdit();
                        mState.resetSelectTool();
                    } else if (mState.mMode == CITY_VIEW_MODES.EDIT_OBJECTS) {
                        if (mState.mTool == OBJECT_TOOLS.NEW) {
                            int objID = mState.addObject(mState.mDestRow, mState.mDestCol, mState.mSelectedObjectType);
                            if (objID >= 0) {
                                mState.mDestCol = -1;
                                mState.mDestRow = -1;
                            } else {
                                Toast.makeText(mState.mActivity, "Error: Placement Failed. Code: " + objID, Toast.LENGTH_LONG).show();
                            }
                        } else if (mState.mTool == OBJECT_TOOLS.SELECT) {
                            int objID = mState.addObject(mState.mDestRow, mState.mDestCol, mState.mSelectedObjectType,
                                    mState.mSelectedObjectID);
                            if (objID >= 0) {
                                mState.mSelectedObjectID = -1;
                                mState.mDestCol = -1;
                                mState.mDestRow = -1;
                            }
                        }
                    }
                }
            } else if (v.equals(mCancelButton)) {
                synchronized (mState) {
                    if (mState.mMode == CITY_VIEW_MODES.EDIT_TERRAIN && mState.mTool == TERRAIN_TOOLS.SELECT) {
                        mState.resetSelectTool();
                    } else if (mState.mMode == CITY_VIEW_MODES.EDIT_OBJECTS) {
                        if (mState.mTool == OBJECT_TOOLS.SELECT) {
                            mState.cancelMoveObject();
                        }
                    }
                }
            } else if (v.equals(mUndoButton)) {

            } else if (v.equals(mRedoButton)) {

            } else if (v.equals(mDeleteButton)) {
                synchronized (mState) {
                    mState.mCityModel.freeObjectID(mState.mSelectedObjectID);
                    mState.mSelectedObjectID = -1;
                    mState.mDestCol = -1;
                    mState.mDestRow = -1;
                }
            } else if (v.equals(mBlendButton)) {
                mState.mDrawWithBlending = !mState.mDrawWithBlending;
            } else if (v.equals(mEyedropperButton)) {
                synchronized (mState) {
                    if (mState.mTool != TERRAIN_TOOLS.EYEDROPPER) {
                        mState.mPreviousTool = mState.mTool;
                        mState.mTool = TERRAIN_TOOLS.EYEDROPPER;
                    }
                }
            }
            if (initMode != mState.mMode || initTool != mState.mTool) {
                if (initMode == CITY_VIEW_MODES.EDIT_OBJECTS && initTool == OBJECT_TOOLS.SELECT && mState.mSelectedObjectID != -1) {
                    mState.cancelMoveObject();
                }
            }
            update();
        }
    };

    /**
     * Handles touch events on the overlay
     */
    private class MainViewTouchListener implements OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // The directional buttons allow panning when the standard scrolling gesture is used for something else.
            // We use an accelerating scroller to pan the view progressively the longer the user holds down a button.
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
                        mState.mScroller.startScroll((int) mState.mFocusRow * Constant.TILE_WIDTH,
                                (int) mState.mFocusCol * Constant.TILE_WIDTH,
                                (int) -min * Constant.TILE_WIDTH,
                                (int) min * Constant.TILE_WIDTH,
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

    /**
     * Overlay component visibility is dictated by the associated city view state.
     * When the state is altered in such a way that the overlay visibility needs updating, this method is called.
     */
    public void update() {
        mUndoButton.setVisibility(View.GONE);
        mRedoButton.setVisibility(View.GONE);

        switch (mState.mMode) {
        case CITY_VIEW_MODES.VIEW:
            mTerrainTools.setVisibility(View.GONE);
            mObjectTools.setVisibility(View.GONE);
            mGeneralTools.setVisibility(View.GONE);
            mBrushTools.setVisibility(View.GONE);
            mMoveButtons.setVisibility(View.GONE);
            break;
        case CITY_VIEW_MODES.EDIT_TERRAIN:
            mTerrainTools.setVisibility(View.VISIBLE);
            mObjectTools.setVisibility(View.GONE);
            mTileSyleIcon.setImageBitmap(TileBitmaps.getFullTileBitmap(mState.mSelectedTerrainType));
            mBlendButton.setSelected(mState.mDrawWithBlending);
            switch (mState.mTool) {
            case TERRAIN_TOOLS.BRUSH:
                mMoveButtons.setVisibility(View.VISIBLE);
                mGeneralTools.setVisibility(View.GONE);
                mBrushTools.setVisibility(View.VISIBLE);
                break;
            case TERRAIN_TOOLS.SELECT:
                mMoveButtons.setVisibility(View.GONE);
                mGeneralTools.setVisibility(View.VISIBLE);
                mBrushTools.setVisibility(View.GONE);
                if (mState.mFirstSelectedRow != -1) {
                    mAcceptButton.setVisibility(View.VISIBLE);
                    mCancelButton.setVisibility(View.VISIBLE);
                } else {
                    mAcceptButton.setVisibility(View.GONE);
                    mCancelButton.setVisibility(View.GONE);
                }
                mDeleteButton.setVisibility(View.GONE);
                break;
            case TERRAIN_TOOLS.EYEDROPPER:
                mMoveButtons.setVisibility(View.GONE);
                mGeneralTools.setVisibility(View.GONE);
                mBrushTools.setVisibility(View.GONE);
                break;
            }
            break;
        case CITY_VIEW_MODES.EDIT_OBJECTS:
            mTerrainTools.setVisibility(View.GONE);
            mObjectTools.setVisibility(View.VISIBLE);
            mGeneralTools.setVisibility(View.VISIBLE);
            mBrushTools.setVisibility(View.GONE);
            mMoveButtons.setVisibility(View.GONE);

            if (mState.mDestRow != -1 && mState.mDestCol != -1) {
                mAcceptButton.setVisibility(View.VISIBLE);
                if (mState.mTool == OBJECT_TOOLS.NEW) {
                    mCancelButton.setVisibility(View.GONE);
                    mDeleteButton.setVisibility(View.GONE);
                } else if (mState.mTool == OBJECT_TOOLS.SELECT) {
                    mCancelButton.setVisibility(View.VISIBLE);
                    mDeleteButton.setVisibility(View.VISIBLE);
                }
            } else {
                mAcceptButton.setVisibility(View.GONE);
                mCancelButton.setVisibility(View.GONE);
                mDeleteButton.setVisibility(View.GONE);
            }
            
            
            break;
        }
        mGridButton.setSelected(mState.mDrawGridLines);
    }

    private void openDialog(int type) {
        GridViewDialogFragment newFragment = new GridViewDialogFragment();
        Bundle b = new Bundle();
        b.putInt(GridViewDialogFragment.TYPE, type);
        newFragment.setArguments(b);
        newFragment.show(mState.mActivity.getFragmentManager(), "TileDialog");
    }
}
