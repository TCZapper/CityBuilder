package com.jasperb.citybuilder.view;

import com.jasperb.citybuilder.CityModel;
import com.jasperb.citybuilder.util.Constant;

public class CityViewState {
    public float mFocusRow = 0, mFocusCol = 0;
    public int mWidth = 0, mHeight = 0;
    public float mScaleFactor;
    private int mTileWidth;
    private int mTileHeight;
    public CityModel mCityModel = null;
    public boolean mDrawGridLines = true;
    
    public CityViewState() {
        setScaleFactor(Constant.MAXIMUM_SCALE_FACTOR);
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
        int newHeight = Math.round(scaleFactor * Constant.TILE_HEIGHT);
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
}
