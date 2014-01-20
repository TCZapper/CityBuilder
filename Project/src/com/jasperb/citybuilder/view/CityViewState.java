package com.jasperb.citybuilder.view;

import com.jasperb.citybuilder.CityModel;
import com.jasperb.citybuilder.util.Constant;

public class CityViewState {
    public float mFocusRow = 0, mFocusCol = 0;
    public int mWidth = 0, mHeight = 0;
    public float mScaleFactor = Constant.MAXIMUM_SCALE_FACTOR;
    public CityModel mCityModel = null;
    public boolean mDrawGridLines = true;
    
    public void copy(CityViewState state) {
        mFocusRow = state.mFocusRow;
        mFocusCol = state.mFocusCol;
        mWidth = state.mWidth;
        mHeight = state.mHeight;
        mScaleFactor = state.mScaleFactor;
        mCityModel = state.mCityModel;
        mDrawGridLines = state.mDrawGridLines;
    }
}
