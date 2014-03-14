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
     * Time it takes to constrain the focus within the valid range when the focus is outside of that range.
     */
    public static final int FOCUS_CONSTRAIN_TIME = 500;

    /**
     * Defines the permitted range for the focus as the valid range +/- this many tiles
     */
    public static final int FOCUS_EXTENDED_BOUNDARY = 8;
    
    public static final short OBJECT_LIMIT = 3000;

    /**
     * Types of available terrain.
     */
    public static class TERRAIN {
        public static final int GRASS = 0, DIRT = 1, CONCRETE = 2, SIDEWALK = 3, PAVEMENT = 4, PAVED_LINE = 5;
        public static final int count = 6;
        
        public static String getName(int terrain) {
            switch(terrain) {
            case GRASS:
                return "Grass";
            case DIRT:
                return "Dirt";
            case CONCRETE:
                return "Concrete";
            case SIDEWALK:
                return "Sidewalk";
            case PAVEMENT:
                return "Pavement";
            case PAVED_LINE:
                return "Paved Line";
            default:
                throw new IllegalArgumentException();
            }
        }

        /**
         * Get the base terrain type for terrain that are modifications of a base terrain type.
         */
        public static int getBaseType(int terrain) {
            if (terrain == TERRAIN.PAVED_LINE) {
                return TERRAIN.PAVEMENT;
            }
            return terrain;
        }
    }

    /**
     * Maximum number of terrain mods that can be applied to a tile.
     */
    public static int MAX_NUMBER_OF_TERRAIN_MODS = 5;

    /**
     * Types of available terrain mods.
     */
    public static class TERRAIN_MODS {
        //Types of terrain mods
        public static final int ROUNDED_GRASS = 0, ROUNDED_DIRT = 4, ROUNDED_CONCRETE = 8, ROUNDED_PAVEMENT = 12, SMOOTHED_PAVED_LINE = 16,
                ROUNDED_PAVED_LINE = 24, STRAIGHT_PAVED_LINE = 28, GRASS_DECORATION = 30, NONE = 33;
        //Number of terrain decorations by type
        public static final int GRASS_DECORATION_COUNT = 3;
        //What is the 1 in X chance for a terrain decoration by type 
        public static final int GRASS_DECORATION_CHANCE = 10;
        //Distinguish which corner of the tile a mod is for (top left touches the row-1,col-1 corner)
        public static final int TOP_LEFT = 0, TOP_RIGHT = 1, BOTTOM_RIGHT = 2, BOTTOM_LEFT = 3;
        //Distinguish between vertical/horizontal directions for mods (vertical is changing rows)
        public static final int VERTICAL = 0, HORIZONTAL = 1;
        //Total number of terrain mods (excluding the NONE type)
        public static final int count = NONE;
        //The value for the first decoration (all following values should be decorations as well, or the NONE type)
        public static final int FIRST_DECORATION = GRASS_DECORATION;

        /**
         * Returns true if this terrain can have its corners rounded using standard rounding mods.
         */
        public static boolean supportsStandardRounding(int terrain) {
            return terrain != TERRAIN.SIDEWALK;
        }

        /**
         * Returns true if this terrain can be used as a standard rounded corner.
         */
        public static boolean hasStandardRoundingMods(int terrain) {
            return terrain != TERRAIN.SIDEWALK;
        }

        /**
         * Returns the type of standard rounded corner terrain mod for blending a tile with adjacent tiles of the specified terrain type
         */
        public static int getRoundedType(int terrain) {
            switch (terrain) {
            case TERRAIN.GRASS:
                return ROUNDED_GRASS;
            case TERRAIN.DIRT:
                return ROUNDED_DIRT;
            case TERRAIN.PAVEMENT:
                return ROUNDED_PAVEMENT;
            case TERRAIN.CONCRETE:
                return ROUNDED_CONCRETE;
            default:
                return NONE;
            }
        }

        public static boolean isTerrainDecoration(int mod) {
            return mod >= FIRST_DECORATION && mod != NONE;
        }
    }
    
    public static class OBJECTS {
        public static final int TEST2X4 = 0;
        public static final int buildingCount = 1;
        
        public static final int count = buildingCount;
        public static final int NONE = count;
        
        public static final byte[] objectNumColumns = new byte[count];
        public static final byte[] objectNumRows = new byte[count];
        static {
            objectNumColumns[TEST2X4] = 4;
            objectNumRows[TEST2X4] = 2;
        }
    }

    /**
     * Types of available modes for the city viewer.
     */
    public static class CITY_VIEW_MODES {
        public static final int VIEW = 0, EDIT_TERRAIN = 1;
    }

    /**
     * Types of available terrain editing tools.
     */
    public static class TERRAIN_TOOLS {
        public static final int BRUSH = 0, SELECT = 1, EYEDROPPER = 2;
    }

    /**
     * Different types of brushes that can be used to draw with.
     */
    public static class BRUSH_TYPES {
        public static final int SQUARE1X1 = 0, SQUARE3X3 = 1, SQUARE5X5 = 2;
    }
}
