/**
 * 
 */
package com.jasperb.citybuilder.util;

import com.jasperb.citybuilder.CityModel;

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

    /**
     * Returns true if and only if the tile exists in the model
     * 
     * @param model
     *            the city model that knows the dimensions of the city
     * @param row
     *            the row of the tile to test
     * @param col
     *            the column of the tile to test
     */
    public static boolean isTileValid(CityModel model, int row, int col) {
        return row >= 0 && col >= 0 && row < model.getHeight() && col < model.getWidth();
    }

    /**
     * Return true if and only if a tile with its top corner drawn at real coordinates (x,y) would be visible in the view
     * 
     * @param scaleFactor
     *            the amount to scale the width and height of the tile by
     * @param viewWidth
     *            the width of the view in pixels
     * @param viewHeight
     *            the height of the view in pixels
     * @param x
     *            the x coordinate for where the tile would be drawn in the view's canvas
     * @param y
     *            the y coordinate for where the tile would be drawn in the view's canvas
     */
    public static boolean isTileVisible(float scaleFactor, int viewWidth, int viewHeight, float x, float y) {
        return !(y >= viewHeight
                || y + (scaleFactor * Constant.TILE_HEIGHT) < 0
                || x + (scaleFactor * Constant.TILE_WIDTH / 2) < 0
                || x - (scaleFactor * Constant.TILE_WIDTH / 2) >= viewWidth);
    }
}
