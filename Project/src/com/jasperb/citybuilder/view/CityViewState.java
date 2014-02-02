package com.jasperb.citybuilder.view;

import java.util.LinkedList;

import android.widget.OverScroller;

import com.jasperb.citybuilder.CityModel;
import com.jasperb.citybuilder.util.Constant;
import com.jasperb.citybuilder.util.Constant.BRUSH_TYPES;
import com.jasperb.citybuilder.util.Constant.CITY_VIEW_MODES;
import com.jasperb.citybuilder.util.Constant.TERRAIN;
import com.jasperb.citybuilder.util.Constant.TERRAIN_TOOLS;
import com.jasperb.citybuilder.util.TerrainEdit;

/**
 * @author Jasper
 * 
 */
public class CityViewState {
    // Thread safe member variables (read from and written to by multiple threads)
    public float mFocusRow = 0, mFocusCol = 0;
    public OverScroller mScroller = null;
    private LinkedList<TerrainEdit> mTerrainEdits = new LinkedList<TerrainEdit>();

    // Thread safe member variables (read from by multiple threads, but only written to by UI thread)
    public int mWidth = 0, mHeight = 0;
    private float mScaleFactor;
    private int mTileWidth;
    private int mTileHeight;
    public CityModel mCityModel = null;
    public boolean mDrawGridLines = true;
    public int mTerrainTypeSelected = TERRAIN.GRASS;
    public int mMode = CITY_VIEW_MODES.VIEW;
    public int mTool = TERRAIN_TOOLS.BRUSH;
    public int mBrushType = BRUSH_TYPES.SQUARE1X1;
    public int mFirstSelectedRow = -1, mFirstSelectedCol = -1, mSecondSelectedRow = -1, mSecondSelectedCol = -1;
    public boolean mSelectingFirstTile = true;
    public boolean mInputActive = false;

    public CityViewState() {
        setScaleFactor(Constant.MAXIMUM_SCALE_FACTOR);
    }

    /**
     * Copy the contents of a CityViewState object into this one.
     * 
     * @param state
     *            the CityViewState object to copy from
     */
    public void copyFrom(CityViewState state) {
        mFocusRow = state.mFocusRow;
        mFocusCol = state.mFocusCol;
        mWidth = state.mWidth;
        mHeight = state.mHeight;
        setScaleFactor(state.getScaleFactor());
        mCityModel = state.mCityModel;
        mDrawGridLines = state.mDrawGridLines;
        mMode = state.mMode;
        mTool = state.mTool;
        mFirstSelectedRow = state.mFirstSelectedRow;
        mFirstSelectedCol = state.mFirstSelectedCol;
        mSecondSelectedRow = state.mSecondSelectedRow;
        mSecondSelectedCol = state.mSecondSelectedCol;
        mSelectingFirstTile = state.mSelectingFirstTile;
    }

    /**
     * Updates the CityView's state and then copies the state into the passed argument
     * 
     * @param to
     *            the object to copy the state into
     */
    protected void updateThenCopyState(CityViewState to) {
        // The purpose of this method is to be used by the draw thread to update the CityView's state and then retrieve that state
        // This lets us easily continuously update the CityView's state and keep the update rate synced with the FPS of the draw thread
        synchronized (this) {
            //Process all of the terrain edits since our last update
            synchronized (mCityModel) {
                for (TerrainEdit edit : mTerrainEdits)
                    edit.setTerrain(mCityModel);
            }
            mTerrainEdits.clear();

            if (mScroller == null)// Happens if cleanup was called but the draw thread is still active
                return;

            // Update the focus based off an active scroller
            // Or if the user input is not active and we are out of bounds, create a new scroller to put us in bounds
            if (!mScroller.isFinished()) {
                mScroller.computeScrollOffset();
                mFocusRow = mScroller.getCurrX() / Constant.TILE_WIDTH;
                mFocusCol = mScroller.getCurrY() / Constant.TILE_WIDTH;
            } else if (!mInputActive && !isTileValid(mFocusRow, mFocusCol)) {
                int startRow = Math.round(mFocusRow * Constant.TILE_WIDTH);
                int startCol = Math.round(mFocusCol * Constant.TILE_WIDTH);
                int endRow = startRow;
                int endCol = startCol;

                if (mFocusRow < 0) {
                    endRow = 0;
                } else if (mFocusRow >= mCityModel.getHeight()) {
                    endRow = mCityModel.getHeight() * Constant.TILE_WIDTH - 1;
                }
                if (mFocusCol < 0) {
                    endCol = 0;
                } else if (mFocusCol >= mCityModel.getWidth()) {
                    endCol = mCityModel.getWidth() * Constant.TILE_WIDTH - 1;
                }
                mScroller.startScroll(startRow, startCol, endRow - startRow, endCol - startCol, Constant.FOCUS_CONSTRAIN_TIME);
            }

            to.copyFrom(this);
        }
    }

    public void addTerrainEdit(TerrainEdit edit) {
        mTerrainEdits.add(edit);
    }
    
    public void addSelectedTerrainEdit() {
        int minRow, maxRow, minCol, maxCol;
        if(mFirstSelectedRow < mSecondSelectedRow) {
            minRow = mFirstSelectedRow;
            maxRow = mSecondSelectedRow;
        } else {
            minRow = mSecondSelectedRow;
            maxRow = mFirstSelectedRow;
        }
        if(mFirstSelectedCol < mSecondSelectedCol) {
            minCol = mFirstSelectedCol;
            maxCol = mSecondSelectedCol;
        } else {
            minCol = mSecondSelectedCol;
            maxCol = mFirstSelectedCol;
        }
        mTerrainEdits.add(new TerrainEdit(minRow, minCol, maxRow, maxCol, mTerrainTypeSelected));
    }

    /**
     * Changes the scale factor and potentially the tile width and height.
     * 
     * @param scaleFactor
     *            the new value to set the scale factor to
     * @return true if the size of a tile changed
     */
    public boolean setScaleFactor(float scaleFactor) {
        mScaleFactor = scaleFactor;
        int newHeight = Math.round(scaleFactor * (Constant.TILE_HEIGHT / 2)) * 2;
        if (newHeight != mTileHeight) {
            mTileHeight = newHeight;
            mTileWidth = mTileHeight * 2;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the real scale factor, unaffected by the size requirements placed on the tile dimensions.
     */
    public float getScaleFactor() {
        return mScaleFactor;
    }

    /**
     * Get the tile width accounting for the scale factor and maintaining the property that tile height is divisible by 2.
     */
    public int getTileWidth() {
        return mTileWidth;
    }

    /**
     * Get the tile width accounting for the scale factor and maintaining the property that tile height is divisible by 2.
     */
    public int getTileHeight() {
        return mTileHeight;
    }

    /**
     * Calculate the real X coordinate relative to the real coordinates of the top-most tile using the isometric coordinates.
     * Takes row/column unscaled and returns the real X coordinate scaled down by the scale factor.
     */
    public int isoToRealXDownscaling(int row, int col) {
        return (mTileWidth / 2) * (col - row);
    }

    /**
     * Calculate the real Y coordinate relative to the real coordinates of the top-most tile using the isometric coordinates.
     * Takes row/column unscaled and returns the real Y coordinate scaled down by the scale factor.
     */
    public int isoToRealYDownscaling(int row, int col) {
        return (mTileHeight / 2) * (col + row);
    }

    /**
     * Calculate the real X coordinate relative to the real coordinates of the top-most tile using the isometric coordinates.
     * Takes row/column unscaled and returns the real X coordinate scaled down by the scale factor.
     */
    public int isoToRealXDownscaling(float row, float col) {
        return Math.round((mTileWidth / 2) * (col - row));
    }

    /**
     * Calculate the real Y coordinate relative to the real coordinates of the top-most tile using the isometric coordinates.
     * Takes row/column unscaled and returns the real Y coordinate scaled down by the scale factor.
     */
    public int isoToRealYDownscaling(float row, float col) {
        return Math.round((mTileHeight / 2) * (col + row));
    }

    /**
     * Calculate the isometric row coordinate using the real coordinates relative to those of the top-most tile.
     * Takes the real coordinates and scales them up by the scale factor.
     */
    public float realToIsoRowUpscaling(int x, int y) {
        return (y / (float) mTileHeight) - (x / (float) mTileWidth);
    }

    /**
     * Calculate the isometric column coordinate using the real coordinates relative to those of the top-most tile.
     * Takes the real coordinates and scales them up by the scale factor.
     */
    public float realToIsoColUpscaling(int x, int y) {
        return (y / (float) mTileHeight) + (x / (float) mTileWidth);
    }

    /**
     * Returns true if the tile exists in the city model.
     * 
     * @param row
     *            the row of the tile to test
     * @param col
     *            the column of the tile to test
     */
    public boolean isTileValid(int row, int col) {
        return row >= 0 && col >= 0 && row < mCityModel.getHeight() && col < mCityModel.getWidth();
    }

    /**
     * Returns true if the tile exists in the city model.
     * 
     * @param row
     *            the row of the tile to test
     * @param col
     *            the column of the tile to test
     */
    public boolean isTileValid(float row, float col) {
        return row >= 0 && col >= 0 && row < mCityModel.getHeight() && col < mCityModel.getWidth();
    }

    /**
     * Return true if and only if a tile with its top corner drawn at real coordinates (x,y) would be visible in the view.
     * 
     * @param x
     *            the x coordinate for where the tile would be drawn in the view's canvas
     * @param y
     *            the y coordinate for where the tile would be drawn in the view's canvas
     */
    public boolean isTileVisible(int x, int y) {
        return !(y >= mHeight
                || y + mTileHeight < 0
                || x + mTileWidth / 2 < 0
                || x - mTileWidth / 2 >= mWidth);
    }

    public int getOriginX() {
        return mWidth / 2 - isoToRealXDownscaling(mFocusRow, mFocusCol);
    }

    public int getOriginY() {
        return mHeight / 2 - isoToRealYDownscaling(mFocusRow, mFocusCol);
    }

    public void forceStopScroller() {
        if (!mScroller.isFinished()) {
            mScroller.computeScrollOffset();//compute current offset before forcing finish
            mFocusRow = mScroller.getCurrX() / Constant.TILE_WIDTH;
            mFocusCol = mScroller.getCurrY() / Constant.TILE_WIDTH;
            mScroller.forceFinished(true);
        }
    }
    
    public void resetFirstSelectedTile() {
        mFirstSelectedRow = -1;
        mFirstSelectedCol = -1;
    }
    
    public void resetSecondSelectedTile() {
        mSecondSelectedRow = -1;
        mSecondSelectedCol = -1;
    }
    
    public void resetSelectTool() {
        resetFirstSelectedTile();
        resetSecondSelectedTile();
        mSelectingFirstTile = true;
    }
}
