package com.jasperb.citybuilder.view;

import com.jasperb.citybuilder.CityModel;
import com.jasperb.citybuilder.util.Constant;

/**
 * @author Jasper
 * 
 */
public class CityViewState {
    public float mFocusRow = 0, mFocusCol = 0;
    public int mWidth = 0, mHeight = 0;
    private float mScaleFactor;
    private int mTileWidth;
    private int mTileHeight;
    public CityModel mCityModel = null;
    public boolean mDrawGridLines = true;

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
}
