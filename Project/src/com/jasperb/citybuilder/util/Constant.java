/**
 * 
 */
package com.jasperb.citybuilder.util;

/**
 * @author Jasper
 * 
 */
public class Constant {
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
     * Friction to be applied when flinging. Larger values correspond to more friction.
     */
    public static final float FLING_FRICTION = 0.05f;
    
    /**
     * Friction to be applied when flinging. Larger values correspond to more friction.
     */
    public static final int MOVE_BUTTON_DURATION = 35;
    
    /**
     * Friction to be applied when flinging. Larger values correspond to more friction.
     */
    public static final float INTERPOLATE_ACCELERATION = 0.8f;
    
    /**
     * Time it takes to constrain the focus within the valid range when the focus is outside of that range
     */
    public static final int FOCUS_CONSTRAIN_TIME = 500;
    
    /**
     * Defines the permitted range for the focus as the valid range +/- this many tiles
     */
    public static final int FOCUS_EXTENDED_BOUNDARY = 8;

    /**
     * Types of available terrain.
     */
    public static class TERRAIN {
        public static final int GRASS = 0, DIRT = 1;
        public static final int count = 2;
    }

    /**
     * Types of available modes for the city viewer
     */
    public static class CITY_VIEW_MODES {
        public static final int VIEW = 0, EDIT_TERRAIN = 1;
    }
    
    /**
     * Types of available terrain editing tools.
     */
    public static class TERRAIN_TOOLS {
        public static final int BRUSH = 0, SELECT = 1;
    }
    
    public static class BRUSH_TYPES {
        public static final int SQUARE1X1 = 0, SQUARE3X3 = 1, SQUARE5X5 = 2;
    }
}
