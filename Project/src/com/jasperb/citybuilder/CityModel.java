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
    private TERRAIN[][] mTerrainMap;

    private final Object mModelLock = new Object();

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

        mTerrainMap = new TERRAIN[mHeight][mWidth];
        for (int row = 0; row < mHeight; row++) {
            for (int col = 0; col < mWidth; col++) {
                if ((row + col) % 2 == 0 || row % 2 == 0) {
                    mTerrainMap[row][col] = TERRAIN.GRASS;
                } else {
                    mTerrainMap[row][col] = TERRAIN.DIRT;
                }
            }
        }
        mTerrainMap[mHeight - 1][mWidth - 1] = TERRAIN.DIRT;
    }

    /**
     * Gets the type of TERRAIN located at the specified row and column.
     * 
     * @param row
     * @param col
     * @return the type of tile at the specified location
     */
    public TERRAIN getTerrain(int row, int col) {
        synchronized (mModelLock) {
            return mTerrainMap[row][col];
        }
    }
}
