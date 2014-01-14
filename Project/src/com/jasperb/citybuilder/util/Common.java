/**
 * 
 */
package com.jasperb.citybuilder.util;

/**
 * @author Jasper
 * 
 */
public class Common {

    /**
     * Calculate the real X coordinate relative to the real coordinates of the top-most tile using the isometric coordinates
     */
    public static float isoToRealX(float row, float col) {
        return (Constant.TILE_WIDTH / 2) * (col - row);
    }

    /**
     * Calculate the real Y coordinate relative to the real coordinates of the top-most tile using the isometric coordinates
     */
    public static float isoToRealY(float row, float col) {
        return (Constant.TILE_HEIGHT / 2) * (col + row);
    }

    /**
     * Calculate the isometric row coordinate using the real coordinates relative to those of the top-most tile
     */
    public static float realToIsoRow(float x, float y) {
        return (y / Constant.TILE_HEIGHT) - (x / Constant.TILE_WIDTH);
    }

    /**
     * Calculate the isometric column coordinate using the real coordinates relative to those of the top-most tile
     */
    public static float realToIsoCol(float x, float y) {
        return (y / Constant.TILE_HEIGHT) + (x / Constant.TILE_WIDTH);
    }
}
