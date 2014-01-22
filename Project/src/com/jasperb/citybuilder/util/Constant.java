/**
 * 
 */
package com.jasperb.citybuilder.util;

/**
 * @author Jasper
 * 
 */
public final class Constant {
    /**
     * Maximum amount of zoom for the contents of the city view.
     */
    public static final float MAXIMUM_SCALE_FACTOR = 1.f;

    /**
     * Minimum amount of zoom for the contents of the city view.
     */
    public static final float MINIMUM_SCALE_FACTOR = 0.6f;

    /**
     * Width of a single, full-sized tile in pixels.
     */
    public static final int TILE_WIDTH = 96;// Must be a multiple of 4

    /**
     * Height of a single, full-sized tile in pixels.
     */
    public static final int TILE_HEIGHT = TILE_WIDTH / 2;// Must be half of TILE_WIDTH

    /**
     * Types of available terrain.
     */
    public enum TERRAIN {
        GRASS, DIRT
    };

}
