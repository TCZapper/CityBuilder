/**
 * 
 */
package com.jasperb.citybuilder.util;

import com.jasperb.citybuilder.CityModel;

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
    boolean mBlend;

    public TerrainEdit(int startRow, int startCol, int endRow, int endCol, int terrain, boolean blend) {
        mStartRow = startRow;
        mStartCol = startCol;
        mEndRow = endRow;
        mEndCol = endCol;
        mTerrain = terrain;
        mBlend = blend;
    }

    public TerrainEdit(int row, int col, int terrain, boolean blend) {
        mStartRow = row;
        mStartCol = col;
        mEndRow = row;
        mEndCol = col;
        mTerrain = terrain;
        mBlend = blend;
    }
    
    public void setTerrain(CityModel model) {
        if(mStartRow == mEndRow && mStartCol == mEndCol) {
            model.setTerrain(mStartRow, mStartCol, mTerrain, mBlend);
        } else {
            model.setTerrain(mStartRow, mStartCol, mEndRow, mEndCol, mTerrain, mBlend);
        }
    }
}
