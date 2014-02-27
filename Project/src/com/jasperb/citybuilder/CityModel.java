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
    private boolean[][] mBlend;

    /**
     * @return width of the model in tiles
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * @return height of the model in tiles
     */
    public int getHeight() {
        return mHeight;
    }

    @SuppressWarnings("unused")
    private CityModel() {}// Prevent constructing without a width and height

    public CityModel(int width, int height) {
        Log.v(TAG, "Create City: " + width + "x" + height);
        mWidth = width;
        mHeight = height;

        // Draw thread processes by column, so keep data in a single column close in memory
        mTerrainMap = new byte[mWidth][mHeight];
        // Try to be clever with memory by keeping all of the terrain mods close in memory
        mTerrainModMap = new byte[mWidth][mHeight * Constant.MAX_NUMBER_OF_TERRAIN_MODS];
        mBlend = new boolean[mWidth][mHeight];

        for (int col = 0; col < mWidth; col++) {
            for (int row = 0; row < mHeight; row++) {
                mTerrainMap[col][row] = (byte) (Math.random() * TERRAIN.count);
                for (int i = 0; i < Constant.MAX_NUMBER_OF_TERRAIN_MODS; i++) {
                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + i] = TERRAIN_MODS.NONE;
                }
                mBlend[col][row] = false;
            }
        }
        for (int col = 0; col < mWidth; col++) {
            for (int row = 0; row < mHeight; row++) {
                determineTerrainMods(row, col);
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
        return mTerrainMap[col][row];
    }

    public byte getMod(int row, int col, int index) {
        return mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + index];
    }

    /**
     * Fill a rectangular region as one terrain type.
     * 
     * @param startRow
     *            minimum row defining the region
     * @param startCol
     *            minimum column defining the region
     * @param endRow
     *            maximum row defining the region
     * @param endCol
     *            maximum column defining the region
     * @param terrain
     *            type of terrain to fill the region with
     */
    public void setTerrain(int startRow, int startCol, int endRow, int endCol, int terrain, boolean blend) {
        for (int col = startCol; col <= endCol; col++) {
            for (int row = startRow; row <= endRow; row++) {
                mTerrainMap[col][row] = (byte) terrain;
                mBlend[col][row] = blend;
            }
        }
        for (int col = Math.max(startCol - 1, 0); col <= Math.min(endCol + 1, mWidth - 1); col++)
            for (int row = Math.max(startRow - 1, 0); row <= Math.min(endRow + 1, mHeight - 1); row++)
                determineTerrainMods(row, col);
    }

    /**
     * Set a tile as one terrain type.
     * 
     * @param row
     * @param col
     * @param terrain
     */
    public void setTerrain(int row, int col, int terrain, boolean blend) {
        mTerrainMap[col][row] = (byte) terrain;
        mBlend[col][row] = blend;
        for (int c = Math.max(col - 1, 0); c <= Math.min(col + 1, mWidth - 1); c++) {
            for (int r = Math.max(row - 1, 0); r <= Math.min(row + 1, mHeight - 1); r++) {
                determineTerrainMods(r, c);
            }
        }
    }

    public void determineTerrainMods(int row, int col) {
        int terrain = mTerrainMap[col][row];
        int modIndex = 0;
        int blendTerrain;
        if (mBlend[col][row]) {
            if (TERRAIN_MODS.isRoundableTerrain(terrain)) {
                if (col - 1 >= 0) {
                    blendTerrain = TERRAIN.getBaseType(mTerrainMap[col - 1][row]);
                    if (TERRAIN_MODS.hasRoundingMods(blendTerrain) && blendTerrain != TERRAIN.getBaseType(terrain)) {
                        if (row - 1 >= 0 && TERRAIN.getBaseType(mTerrainMap[col - 1][row - 1]) == blendTerrain
                                && TERRAIN.getBaseType(mTerrainMap[col][row - 1]) == blendTerrain) {
                            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] =
                                    (byte) (TERRAIN_MODS.getRoundedType(blendTerrain) + TERRAIN_MODS.TOP_LEFT);
                            modIndex++;
                        }
                        if (row + 1 < mHeight && TERRAIN.getBaseType(mTerrainMap[col - 1][row + 1]) == blendTerrain
                                && TERRAIN.getBaseType(mTerrainMap[col][row + 1]) == blendTerrain) {
                            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] =
                                    (byte) (TERRAIN_MODS.getRoundedType(blendTerrain) + TERRAIN_MODS.BOTTOM_LEFT);
                            modIndex++;
                        }
                    }
                }
                if (col + 1 < mWidth) {
                    blendTerrain = TERRAIN.getBaseType(mTerrainMap[col + 1][row]);
                    if (TERRAIN_MODS.hasRoundingMods(blendTerrain) && blendTerrain != TERRAIN.getBaseType(terrain)) {
                        if (row - 1 >= 0 && TERRAIN.getBaseType(mTerrainMap[col + 1][row - 1]) == blendTerrain
                                && TERRAIN.getBaseType(mTerrainMap[col][row - 1]) == blendTerrain) {
                            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] =
                                    (byte) (TERRAIN_MODS.getRoundedType(blendTerrain) + TERRAIN_MODS.TOP_RIGHT);
                            modIndex++;
                        }
                        if (row + 1 < mHeight && TERRAIN.getBaseType(mTerrainMap[col + 1][row + 1]) == blendTerrain
                                && TERRAIN.getBaseType(mTerrainMap[col][row + 1]) == blendTerrain) {
                            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] =
                                    (byte) (TERRAIN_MODS.getRoundedType(blendTerrain) + TERRAIN_MODS.BOTTOM_RIGHT);
                            modIndex++;
                        }
                    }
                }
            }
        }
        if (modIndex < Constant.MAX_NUMBER_OF_TERRAIN_MODS)
            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.NONE;
    }
}
