/**
 * 
 */
package com.jasperb.citybuilder;

import com.jasperb.citybuilder.util.Constant.TERRAIN;

import android.util.Log;

/**
 * @author Jasper
 * 
 */
public class CityModel {
    /**
     * String used for identifying this class.
     */
    public static final String TAG = "CityModel";

    private int mWidth, mHeight;
    private byte[][] mTerrainMap;

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    @SuppressWarnings("unused")
    private CityModel() {}// Prevent constructing without a width and height

    public CityModel(int width, int height) {
        Log.v(TAG, "Create City: " + width + "x" + height);
        mWidth = width;
        mHeight = height;

        mTerrainMap = new byte[mHeight][mWidth];
        for (int row = 0; row < mHeight; row++) {
            for (int col = 0; col < mWidth; col++) {
                if ((row + col) % 8 < 4 || row % 20 == 0) {
                    mTerrainMap[row][col] = TERRAIN.GRASS;
                } else {
                    mTerrainMap[row][col] = TERRAIN.DIRT;
                }
            }
        }
    }

    /**
     * Gets the type of TERRAIN located at the specified row and column.
     * 
     * @param row
     * @param col
     * @return the type of tile at the specified location
     */
    public byte getTerrain(int row, int col) {
        synchronized (this) {
            return mTerrainMap[row][col];
        }
    }

    public void setTerrain(int startRow, int startCol, int endRow, int endCol, int terrain) {
        for(int row = startRow; row <= endRow; row++)
            for(int col = startCol; col <= endCol; col++)
                mTerrainMap[row][col] = (byte) terrain;
    }

    public void setTerrain(int row, int col, int terrain) {
        mTerrainMap[row][col] = (byte) terrain;
    }
}
