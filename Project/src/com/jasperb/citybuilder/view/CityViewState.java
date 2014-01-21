package com.jasperb.citybuilder.view;

import com.jasperb.citybuilder.CityModel;
import com.jasperb.citybuilder.util.Constant;

public class CityViewState {
    public float mFocusRow = 0, mFocusCol = 0;
    public int mWidth = 0, mHeight = 0;
    private float mScaleFactor;
    private int mTileWidth;
    private int mTileHeight;
    public CityModel mCityModel = null;
    public boolean mDrawGridLines = true;
    
    public CityViewState() {
        setScaleFactor(Constant.MINIMUM_SCALE_FACTOR);
    }
    
    public void copy(CityViewState state) {
        mFocusRow = state.mFocusRow;
        mFocusCol = state.mFocusCol;
        mWidth = state.mWidth;
        mHeight = state.mHeight;
        setScaleFactor(state.getScaleFactor());
        mCityModel = state.mCityModel;
        mDrawGridLines = state.mDrawGridLines;
    }
    
    public boolean setScaleFactor(float scaleFactor) {
        mScaleFactor = scaleFactor;
        int newHeight = Math.round(scaleFactor * (Constant.TILE_HEIGHT / 2)) * 2;
        if(newHeight != mTileHeight) {
            mTileHeight = newHeight;
            mTileWidth = mTileHeight * 2;
            return true;
        } else {
            return false;
        }
    }
    
    public float getScaleFactor() {
        return mScaleFactor;
    }
    
    public float getTileWidth() {
        return mTileWidth;
    }
    
    public float getTileHeight() {
        return mTileHeight;
    }
    
    /**
     * Calculate the real X coordinate relative to the real coordinates of the top-most tile using the isometric coordinates
     */
    public int isoToRealXDownscaling(int row, int col) {
        return (mTileWidth / 2) * (col - row);
    }

    /**
     * Calculate the real Y coordinate relative to the real coordinates of the top-most tile using the isometric coordinates
     */
    public int isoToRealYDownscaling(int row, int col) {
        return (mTileHeight / 2) * (col + row);
    }
    
    /**
     * Calculate the real X coordinate relative to the real coordinates of the top-most tile using the isometric coordinates
     */
    public int isoToRealXDownscaling(float row, float col) {
        return Math.round((mTileWidth / 2) * (col - row));
    }

    /**
     * Calculate the real Y coordinate relative to the real coordinates of the top-most tile using the isometric coordinates
     */
    public int isoToRealYDownscaling(float row, float col) {
        return Math.round((mTileHeight / 2) * (col + row));
    }

    /**
     * Calculate the isometric row coordinate using the real coordinates relative to those of the top-most tile
     */
    public float realToIsoRowUpscaling(int x, int y) {
        return (y / (float)mTileHeight) - (x / (float)mTileWidth);
    }

    /**
     * Calculate the isometric column coordinate using the real coordinates relative to those of the top-most tile
     */
    public float realToIsoColUpscaling(int x, int y) {
        return (y / (float)mTileHeight) + (x / (float)mTileWidth);
    }

    /**
     * Returns true if and only if the tile exists in the model
     * 
     * @param model
     *            the city model that knows the dimensions of the city
     * @param row
     *            the row of the tile to test
     * @param col
     *            the column of the tile to test
     */
    public boolean isTileValid(int row, int col) {
        return row >= 0 && col >= 0 && row < mCityModel.getHeight() && col < mCityModel.getWidth();
    }

    /**
     * Return true if and only if a tile with its top corner drawn at real coordinates (x,y) would be visible in the view
     * 
     * @param scaleFactor
     *            the amount to scale the width and height of the tile by
     * @param viewWidth
     *            the width of the view in pixels
     * @param viewHeight
     *            the height of the view in pixels
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
