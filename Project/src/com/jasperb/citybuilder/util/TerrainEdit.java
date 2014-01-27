/**
 * 
 */
package com.jasperb.citybuilder.util;

import com.jasperb.citybuilder.view.CityViewState;

/**
 * @author Jasper
 * 
 */
public class TerrainEdit {
    int mStartRow;
    int mStartCol;
    int mEndRow;
    int mEndCol;
    int mTerrain;

    public TerrainEdit(int startRow, int startCol, int endRow, int endCol, int terrain) {
        mStartRow = startRow;
        mStartCol = startCol;
        mEndRow = endRow;
        mEndCol = endCol;
        mTerrain = terrain;
    }

    public TerrainEdit(int row, int col, int terrain) {
        mStartRow = row;
        mStartCol = col;
        mEndRow = row;
        mEndCol = col;
        mTerrain = terrain;
    }
    
    public void setTerrain(CityViewState state) {
        if(mStartRow == mEndRow && mStartCol == mEndCol) {
            state.mCityModel.setTerrain(mStartRow, mStartCol, mTerrain);
        } else {
            state.mCityModel.setTerrain(mStartRow, mStartCol, mEndRow, mEndCol, mTerrain);
        }
    }
}
