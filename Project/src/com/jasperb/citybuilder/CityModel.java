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
                mTerrainMap[col][row] = TERRAIN.GRASS;
                //mTerrainMap[col][row] = (byte) (Math.random() * (TERRAIN.count + 2));
                if (mTerrainMap[col][row] >= TERRAIN.count) {
                    if (row == 0) {
                        mTerrainMap[col][row] = (byte) (Math.random() * TERRAIN.count);
                    } else {
                        mTerrainMap[col][row] = mTerrainMap[col][row - 1];
                    }
                }
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS] = TERRAIN_MODS.NONE;
                mBlend[col][row] = true;
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

    private void determineTerrainMods(int row, int col) {
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

        if (terrain == TERRAIN.PAVED_LINE && setPavedLineMod(row, col, modIndex)) {
            Log.d(TAG,"PAVED: " + mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex]);
            modIndex++;
        }

        if (modIndex < Constant.MAX_NUMBER_OF_TERRAIN_MODS)
            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.NONE;
        else
            assert (modIndex == Constant.MAX_NUMBER_OF_TERRAIN_MODS);
    }

    private boolean setPavedLineMod(int row, int col, int modIndex) {
        if (col - 1 >= 0 && mTerrainMap[col - 1][row] == TERRAIN.PAVED_LINE) {
//            if (mBlend[col][row] && col + 1 < mWidth) {
//                if (row - 1 >= 0 && mTerrainMap[col + 1][row - 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.HORIZONTAL * 4 + TERRAIN_MODS.TOP_RIGHT;
//                    return true;
//                } else if (row + 1 < mHeight && mTerrainMap[col + 1][row + 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.HORIZONTAL * 4 + TERRAIN_MODS.BOTTOM_RIGHT;
//                    return true;
//                }
//            }
            if (row - 1 >= 0 && mTerrainMap[col][row - 1] == TERRAIN.PAVED_LINE) {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.ROUNDED_PAVED_LINE
                        + TERRAIN_MODS.TOP_LEFT;
            } else if (row + 1 < mHeight && mTerrainMap[col][row + 1] == TERRAIN.PAVED_LINE) {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.ROUNDED_PAVED_LINE
                        + TERRAIN_MODS.BOTTOM_LEFT;
            } else {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.STRAIGHT_PAVED_LINE
                        + TERRAIN_MODS.HORIZONTAL;
            }
        } else if (col + 1 < mWidth && mTerrainMap[col + 1][row] == TERRAIN.PAVED_LINE) {
//            if (mBlend[col][row] && col - 1 >= 0) {
//                if (row - 1 >= 0 && mTerrainMap[col - 1][row - 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.HORIZONTAL * 4 + TERRAIN_MODS.TOP_LEFT;
//                    return true;
//                } else if (row + 1 < mHeight && mTerrainMap[col - 1][row + 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.HORIZONTAL * 4 + TERRAIN_MODS.BOTTOM_LEFT;
//                    return true;
//                }
//            }
            if (row - 1 >= 0 && mTerrainMap[col][row - 1] == TERRAIN.PAVED_LINE) {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.ROUNDED_PAVED_LINE
                        + TERRAIN_MODS.TOP_RIGHT;
            } else if (row + 1 < mHeight && mTerrainMap[col][row + 1] == TERRAIN.PAVED_LINE) {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.ROUNDED_PAVED_LINE
                        + TERRAIN_MODS.BOTTOM_RIGHT;
            } else {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.STRAIGHT_PAVED_LINE
                        + TERRAIN_MODS.HORIZONTAL;
            }
        } else if (row - 1 >= 0 && mTerrainMap[col][row - 1] == TERRAIN.PAVED_LINE) {
//            if (mBlend[col][row] && row + 1 < mHeight) {
//                if (col - 1 >= 0 && mTerrainMap[col - 1][row + 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.VERTICAL * 4 + TERRAIN_MODS.BOTTOM_LEFT;
//                    return true;
//                } else if (col + 1 < mWidth && mTerrainMap[col + 1][row + 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.VERTICAL * 4 + TERRAIN_MODS.BOTTOM_RIGHT;
//                    return true;
//                }
//            }
            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.STRAIGHT_PAVED_LINE
                    + TERRAIN_MODS.VERTICAL;
        } else if (row + 1 < mHeight && mTerrainMap[col][row + 1] == TERRAIN.PAVED_LINE) {
//            if (mBlend[col][row] && row - 1 >= 0) {
//                if (col - 1 >= 0 && mTerrainMap[col - 1][row - 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.VERTICAL * 4 + TERRAIN_MODS.TOP_LEFT;
//                    return true;
//                } else if (col + 1 < mWidth && mTerrainMap[col + 1][row - 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.VERTICAL * 4 + TERRAIN_MODS.TOP_RIGHT;
//                    return true;
//                }
//            }
            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.STRAIGHT_PAVED_LINE
                    + TERRAIN_MODS.VERTICAL;
        } else {
            return false;
        }
        return true;
    }
}
