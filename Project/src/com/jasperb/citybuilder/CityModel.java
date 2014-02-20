/**
 * 
 */
package com.jasperb.citybuilder;

import com.jasperb.citybuilder.util.Constant;
import com.jasperb.citybuilder.util.Constant.TERRAIN;
import com.jasperb.citybuilder.util.Constant.TERRAIN_MODS;

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
    private byte[][] mTerrainModMap;

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

        // Draw thread processes by column, so keep data in a single column close
        mTerrainMap = new byte[mWidth][mHeight];
        // Try to be clever with memory by keeping all of the terrain mods within a few pages of memory
        mTerrainModMap = new byte[mWidth][mHeight * Constant.MAX_NUMBER_OF_TERRAIN_MODS];

        for (int col = 0; col < mWidth; col++) {
            for (int row = 0; row < mHeight; row++) {
                if ((row + col) % 8 < 4 || row % 20 == 0) {
                    mTerrainMap[col][row] = TERRAIN.GRASS;
                } else {
                    mTerrainMap[col][row] = TERRAIN.DIRT;
                }
                for (int i = 0; i < Constant.MAX_NUMBER_OF_TERRAIN_MODS; i++) {
                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + i] = TERRAIN_MODS.NONE;
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
            return mTerrainMap[col][row];
        }
    }

    public void setTerrain(int startRow, int startCol, int endRow, int endCol, int terrain) {
        for (int col = startCol; col <= endCol; col++)
            for (int row = startRow; row <= endRow; row++)
                mTerrainMap[col][row] = (byte) terrain;
    }

    public void setTerrain(int row, int col, int terrain) {
        mTerrainMap[col][row] = (byte) terrain;
    }
}
