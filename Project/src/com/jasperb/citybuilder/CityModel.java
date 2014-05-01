/**
 * 
 */
package com.jasperb.citybuilder;

import java.io.IOException;
import java.util.Arrays;

import android.util.Log;

import com.jasperb.citybuilder.Constant.OBJECTS;
import com.jasperb.citybuilder.Constant.TERRAIN;
import com.jasperb.citybuilder.Constant.TERRAIN_MODS;
import com.jasperb.citybuilder.util.FileStreamUtils;

/**
 * @author Jasper
 * 
 */
public class CityModel {
    /**
     * String used for identifying this class.
     */
    public static final String TAG = "CityModel";

    public class ObjectSlice {
        public short row, col;
        public short id;
        public byte type;
        public byte sliceIndex;
        public ObjectSlice next = null;

        public ObjectSlice(short row, short col, short id, byte type, byte sliceIndex) {
            this.row = row;
            this.col = col;
            this.id = id;
            this.type = type;
            this.sliceIndex = sliceIndex;
        }

        public ObjectSlice() {}

        public void log(String tag) {
            Log.d(tag, id + ": [R" + row + ",C" + col + "] = " + type + "[" + sliceIndex + "]");
        }

        public void write(FileStreamUtils stream) throws IOException {
            stream.write(row);
            stream.write(col);
            stream.write(id);
            stream.write(type);
            stream.write(sliceIndex);
        }

        public boolean read(FileStreamUtils stream) throws IOException {
            row = stream.readShort();
            if (row == -1)
                return false;
            col = stream.readShort();
            id = stream.readShort();
            type = stream.readByte();
            sliceIndex = stream.readByte();
            return true;
        }
    }

    private int mWidth, mHeight;
    private byte[][] mTerrainMap = null;
    private byte[][] mTerrainModMap;
    private byte[][] mTerrainBlend;
    private short[][] mObjectMap;
    private boolean[] mUsedObjectIDs;
    private ObjectSlice mObjectList = null;
    private short mNumObjects = 0;

    @SuppressWarnings("unused")
    private CityModel() {}// Prevent constructing without a width and height

    public CityModel(int width, int height) {
        Log.v(TAG, "Create City: " + width + "x" + height);
        mWidth = width;
        mHeight = height;

        // Draw thread processes by column, so keep data in a single column close in memory
        mTerrainMap = new byte[mWidth][mHeight];
        mObjectMap = new short[mWidth][mHeight];
        mUsedObjectIDs = new boolean[Constant.OBJECT_LIMIT];
        Arrays.fill(mUsedObjectIDs, false);

        // Try to be clever with memory by keeping all of the terrain mods close in memory
        mTerrainModMap = new byte[mWidth][mHeight * Constant.MAX_NUMBER_OF_TERRAIN_MODS];
        mTerrainBlend = new byte[mWidth][mHeight];

        for (int col = 0; col < mWidth; col++) {
            for (int row = 0; row < mHeight; row++) {
                mTerrainMap[col][row] = TERRAIN.GRASS;
                mTerrainMap[col][row] = (byte) (Math.random() * (TERRAIN.count + 3));
                if (mTerrainMap[col][row] >= TERRAIN.count) {
                    if (row == 0) {
                        mTerrainMap[col][row] = (byte) (Math.random() * TERRAIN.count);
                    } else {
                        mTerrainMap[col][row] = mTerrainMap[col][row - 1];
                    }
                }
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS] = TERRAIN_MODS.NONE;
                mTerrainBlend[col][row] = 1;
                mObjectMap[col][row] = -1;
            }
        }
        for (int col = 0; col < mWidth; col++) {
            for (int row = 0; row < mHeight; row++) {
                determineTerrainDecorations(row, col);
                determineTerrainMods(row, col);
            }
        }
    }

    public CityModel(FileStreamUtils stream) {
        if(!restore(stream)) {
            mWidth = 0;
            mHeight = 0;
        }
        Log.v(TAG, "Create City: " + mWidth + "x" + mHeight);
    }

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

    /**
     * Gets the type of TERRAIN located at the specified row and column.
     * 
     * @param row
     *            the row of the tile
     * @param col
     *            the column of the tile
     * @return the type of tile at the specified location
     */
    public byte getTerrain(int row, int col) {
        return mTerrainMap[col][row];
    }

    /**
     * @param row
     *            the row of the tile
     * @param col
     *            the column of the tile
     * @param index
     *            the mod index
     * @return the type of mod at the specified location and mod index
     */
    public byte getMod(int row, int col, int index) {
        return mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + index];
    }

    /**
     * Gets the id of object located at the specified row and column.
     * 
     * @param row
     *            the row of the tile
     * @param col
     *            the column of the tile
     * @return the id of building at the specified location
     */
    public short getObjectID(int row, int col) {
        return mObjectMap[col][row];
    }

    public ObjectSlice getObjectList() {
        return mObjectList;
    }

    /**
     * @return the number of objects in the city model.
     */
    public short getNumberOfObjects() {
        return mNumObjects;
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
     * @param blend
     *            true if the terrain at these tiles should use blending mods
     */
    public void setTerrain(int startRow, int startCol, int endRow, int endCol, int terrain, boolean blend) {
        for (int col = startCol; col <= endCol; col++) {
            for (int row = startRow; row <= endRow; row++) {
                mTerrainMap[col][row] = (byte) terrain;
                mTerrainBlend[col][row] = (byte) (blend ? 1 : 0);
                determineTerrainDecorations(row, col);
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
     * @param blend
     *            true if the terrain at this tile should use blending mods
     */
    public void setTerrain(int row, int col, int terrain, boolean blend) {
        mTerrainMap[col][row] = (byte) terrain;
        mTerrainBlend[col][row] = (byte) (blend ? 1 : 0);
        determineTerrainDecorations(row, col);
        for (int c = Math.max(col - 1, 0); c <= Math.min(col + 1, mWidth - 1); c++) {
            for (int r = Math.max(row - 1, 0); r <= Math.min(row + 1, mHeight - 1); r++) {
                determineTerrainMods(r, c);
            }
        }
    }

    /**
     * Determine the terrain mods to be used for a specified tile based off the surrounding 8 tiles
     * 
     * @param row
     * @param col
     * @param overwriteDecorations
     *            if true terrain decorations are overwritten
     */
    private void determineTerrainMods(int row, int col) {
        int terrain = mTerrainMap[col][row];
        int modIndex = 0;
        int blendTerrain;

        if (TERRAIN_MODS.isTerrainDecoration(mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex]))
            modIndex++;

        if (mTerrainBlend[col][row] != 0) {
            if (TERRAIN_MODS.supportsStandardRounding(terrain)) {
                if (col - 1 >= 0) {
                    blendTerrain = TERRAIN.getBaseType(mTerrainMap[col - 1][row]);
                    //Check that the surrounding terrain can be used for blending, and that blending is even needed (not same base type)
                    if (TERRAIN_MODS.hasStandardRoundingMods(blendTerrain) && blendTerrain != TERRAIN.getBaseType(terrain)) {
                        //Only blend the top left corner if all 3 tiles touching the corner are of the same base type
                        if (row - 1 >= 0 && TERRAIN.getBaseType(mTerrainMap[col - 1][row - 1]) == blendTerrain
                                && TERRAIN.getBaseType(mTerrainMap[col][row - 1]) == blendTerrain) {
                            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] =
                                    (byte) (TERRAIN_MODS.getRoundedType(blendTerrain) + TERRAIN_MODS.TOP_LEFT);
                            modIndex++;
                        }
                        //Only blend the bottom left corner if all 3 tiles touching the corner are of the same base type
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
                    //Check that the surrounding terrain can be used for blending, and that blending is even needed (not same base type)
                    if (TERRAIN_MODS.hasStandardRoundingMods(blendTerrain) && blendTerrain != TERRAIN.getBaseType(terrain)) {
                        //Only blend the top right corner if all 3 tiles touching the corner are of the same base type
                        if (row - 1 >= 0 && TERRAIN.getBaseType(mTerrainMap[col + 1][row - 1]) == blendTerrain
                                && TERRAIN.getBaseType(mTerrainMap[col][row - 1]) == blendTerrain) {
                            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] =
                                    (byte) (TERRAIN_MODS.getRoundedType(blendTerrain) + TERRAIN_MODS.TOP_RIGHT);
                            modIndex++;
                        }
                        //Only blend the bottom right corner if all 3 tiles touching the corner are of the same base type
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

        //Paved line must always have a mod to indicate that it is a paved line        
        if (terrain == TERRAIN.PAVED_LINE) {
            if (setPavedLineMod(row, col, modIndex)) {
                modIndex++;
            } else {//if there is no adjacent paved lines to connect to, just draw a generic one
                //If the limit on terrain mods is exceeded, just drop all of them in favour of the paved line
                if (modIndex == Constant.MAX_NUMBER_OF_TERRAIN_MODS) {
                    modIndex = 0;
                }
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.STRAIGHT_PAVED_LINE;
                modIndex++;
            }
        }

        if (modIndex < Constant.MAX_NUMBER_OF_TERRAIN_MODS)
            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.NONE;
        else
            assert (modIndex == Constant.MAX_NUMBER_OF_TERRAIN_MODS);
    }

    /**
     * Determine which paved line mods make sense based off adjacent terrain paved line tiles.
     * Note: this method does not supply a default paved line mod if there are no adjacent paved line tiles.
     * 
     * @param row
     * @param col
     * @param modIndex
     *            the current number of mods already assigned to the specified tile
     * @return true if at least one adjacent paved line tile was found
     */
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
            //Test for the two L shaped scenarios 
            if (row - 1 >= 0 && mTerrainMap[col][row - 1] == TERRAIN.PAVED_LINE) {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.ROUNDED_PAVED_LINE
                        + TERRAIN_MODS.TOP_LEFT;
            } else if (row + 1 < mHeight && mTerrainMap[col][row + 1] == TERRAIN.PAVED_LINE) {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.ROUNDED_PAVED_LINE
                        + TERRAIN_MODS.BOTTOM_LEFT;
            } else {//Not L-shaped, so default to straight
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
            //Test for the two L shaped scenarios 
            if (row - 1 >= 0 && mTerrainMap[col][row - 1] == TERRAIN.PAVED_LINE) {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.ROUNDED_PAVED_LINE
                        + TERRAIN_MODS.TOP_RIGHT;
            } else if (row + 1 < mHeight && mTerrainMap[col][row + 1] == TERRAIN.PAVED_LINE) {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.ROUNDED_PAVED_LINE
                        + TERRAIN_MODS.BOTTOM_RIGHT;
            } else {//Not L-shaped, so default to straight
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
            //L-shaped scenario would have been caught prior, so default to straight 
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
            //L-shaped scenario would have been caught prior, so default to straight 
            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.STRAIGHT_PAVED_LINE
                    + TERRAIN_MODS.VERTICAL;
        } else {
            return false;
        }
        return true;
    }

    private void determineTerrainDecorations(int row, int col) {
        if (mTerrainMap[col][row] == TERRAIN.GRASS) {
            int rand = (int) ((Math.random() * TERRAIN_MODS.GRASS_DECORATION_COUNT) * TERRAIN_MODS.GRASS_DECORATION_CHANCE);
            if (rand < TERRAIN_MODS.GRASS_DECORATION_COUNT) {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS] = (byte) (TERRAIN_MODS.GRASS_DECORATION + rand);
            } else {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS] = TERRAIN_MODS.NONE;
            }
        } else {
            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS] = TERRAIN_MODS.NONE;
        }
    }

    public void addObject(int row, int col, int type, int id) {
        createObject(row, col, type, id);
        for (int c = col; c < col + OBJECTS.objectNumColumns[type]; c++) {
            for (int r = row; r < row + OBJECTS.objectNumRows[type]; r++) {
                mObjectMap[c][r] = (short) id;
            }
        }
    }

    public int allocateNewObjectID() {
        if (mNumObjects == Constant.OBJECT_LIMIT)
            return -1;
        int i = 0;
        for (; i <= mNumObjects; i++) {
            if (!mUsedObjectIDs[i]) {
                mUsedObjectIDs[i] = true;
                mNumObjects++;
                break;
            }
        }

        if (i == mNumObjects + 1) {
            throw new IllegalStateException("There should be at least one free Object ID"); //This shouldn't be possible
        } else {
            return i;
        }
    }

    private void createObject(int row, int col, int type, int id) {
        int sliceColumns = OBJECTS.getSliceWidth(type) / (Constant.TILE_WIDTH / 2);
        int lastColumn = col + OBJECTS.objectNumColumns[type] - 1;
        int sliceCount = OBJECTS.getSliceCount(type);
        //Log.d(TAG, "NUM SLICES: " + sliceCount);
        int sliceCol = col;
        int sliceRow = row + OBJECTS.objectNumRows[type] - 1;
        byte sliceIndex = 0;
        ObjectSlice currentSlice = mObjectList, previousSlice = null;
        while (currentSlice != null) {
            //Log.d(TAG, "SLICE ANALYSIS: " + currentSlice.row + " :: " + currentSlice.col + " -- " + row + " :: " + sliceCol);
            if (currentSlice.col > sliceCol || (currentSlice.col == sliceCol && currentSlice.row > row)) {
                previousSlice = addObjectSlice(previousSlice, new ObjectSlice((short) sliceRow, (short) sliceCol, (short) id,
                        (byte) type, sliceIndex));
                sliceIndex++;
                if (sliceIndex == sliceCount) {
                    return;
                } else {
                    if (sliceCol == lastColumn) {
                        break;
                    } else {
                        sliceCol += sliceColumns;
                        if (sliceCol > lastColumn) {
                            sliceCol = lastColumn;
                        }
                    }
                }
            } else {
                previousSlice = currentSlice;
                currentSlice = currentSlice.next;
            }
        }

        while (sliceIndex < sliceCount) {
            //Log.d(TAG, "APPEND SLICE: " + sliceIndex);
            previousSlice = addObjectSlice(previousSlice, new ObjectSlice((short) sliceRow, (short) sliceCol, (short) id, (byte) type,
                    sliceIndex));
            sliceIndex++;
            if (sliceCol != lastColumn) {
                sliceCol += sliceColumns;
                if (sliceCol > lastColumn) {
                    sliceCol = lastColumn;
                }
            }
        }
    }

    /**
     * Add an object slice to the object linked list
     * 
     * @param node
     *            the node to append the new slice after
     * @param newSlice
     *            the new slice to append to the list
     * @return the new slice added
     */
    private ObjectSlice addObjectSlice(ObjectSlice node, ObjectSlice newSlice) {
        if (node == null) {
            newSlice.next = mObjectList;
            mObjectList = newSlice;
        } else {
            newSlice.next = node.next;
            node.next = newSlice;
        }
        return newSlice;
    }

    public ObjectSlice removeObject(int id) {
        ObjectSlice firstSlice = null;
        ObjectSlice currentSlice = mObjectList, previousSlice = null;
        int slicesRemoved = 0;
        while (currentSlice != null) {
            if (currentSlice.id == id) {
                if (firstSlice == null)
                    firstSlice = currentSlice;

                currentSlice = removeObjectSlice(previousSlice);
                slicesRemoved++;
                if (slicesRemoved == OBJECTS.getSliceCount(firstSlice.type))
                    break;
            } else {
                previousSlice = currentSlice;
                currentSlice = currentSlice.next;
            }
        }
        return firstSlice;
    }

    public ObjectSlice removeObjectSlice(ObjectSlice parentNode) {
        if (parentNode == null) {
            mObjectList = mObjectList.next;
            return mObjectList;
        } else {
            parentNode.next = parentNode.next.next;
            return parentNode.next;
        }
    }

    public ObjectSlice getObjectSlice(int id) {
        ObjectSlice currentSlice = mObjectList;
        while (currentSlice != null && currentSlice.id != id)
            currentSlice = currentSlice.next;

        return currentSlice;
    }

    /*
     * Format:
     * int mWidth, mHeight;
     * short mNumObjects;
     * byte[][] mTerrainMap;
     * byte[][] mTerrainModMap;
     * boolean[][] mTerrainBlend;
     * ObjectSlice mObjectList;
     */
    public boolean save(FileStreamUtils stream) {
        Log.v(TAG, "Saving...");
        try {
            stream.write(Constant.CURRENT_VERSION_NUM);
            stream.write(mWidth);
            stream.write(mHeight);
            stream.write(mNumObjects);
            for (byte[] arr : mTerrainMap) {
                stream.write(arr);
            }
            for (byte[] arr : mTerrainModMap) {
                stream.write(arr);
            }
            for (byte[] arr : mTerrainBlend) {
                stream.write(arr);
            }
            ObjectSlice curSlice = mObjectList;
            while (curSlice != null) {
                curSlice.write(stream);
                curSlice = curSlice.next;
            }
            stream.write((short)-1);//Mark ending for object slice loop
            stream.write(0xDEADBEEF);//Mark EOF
            stream.flush();
        } catch (IOException e) {
            Log.d(TAG, "IO Exception on saving City Model: " + e.getLocalizedMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                return false;
            }
        }
        Log.v(TAG, "Done Saving");
        return true;
    }

    public boolean restore(FileStreamUtils stream) {
        Log.v(TAG, "Restoring...");
        try {
            @SuppressWarnings("unused")
            int curVersion = stream.readInt();
            mWidth = stream.readInt();
            mHeight = stream.readInt();
            mNumObjects = stream.readShort();
            if (mTerrainMap == null) {
                mTerrainMap = new byte[mWidth][mHeight];
                mTerrainModMap = new byte[mWidth][mHeight * Constant.MAX_NUMBER_OF_TERRAIN_MODS];
                mTerrainBlend = new byte[mWidth][mHeight];
                mObjectMap = new short[mWidth][mHeight];
                mUsedObjectIDs = new boolean[Constant.OBJECT_LIMIT];
                Arrays.fill(mUsedObjectIDs, false);
                for(short[] arr : mObjectMap) {
                    Arrays.fill(arr, (short) -1);
                }
            }
            for (int i = 0; i < mWidth; i++) {
                stream.readBytes(mTerrainMap[i], mHeight);
            }
            for (int i = 0; i < mWidth; i++) {
                stream.readBytes(mTerrainModMap[i], mHeight * Constant.MAX_NUMBER_OF_TERRAIN_MODS);
            }
            for (int i = 0; i < mWidth; i++) {
                stream.readBytes(mTerrainBlend[i], mHeight);
            }
            ObjectSlice curSlice, newSlice = new ObjectSlice();
            if (newSlice.read(stream)) {
                setupReadObject(newSlice);
                curSlice = newSlice;
                mObjectList = newSlice;
                newSlice = new ObjectSlice();
                while (newSlice.read(stream)) {
                    setupReadObject(newSlice);
                    curSlice.next = newSlice;
                    curSlice = newSlice;
                    newSlice = new ObjectSlice();
                }

                curSlice.next = null;
            } else {
                mObjectList = null;
            }
            if(stream.readInt() != 0xDEADBEEF) {//Check for EOF
                Log.d(TAG, "Missing DEADBEEF at EOF");
                return false;
            }
        } catch (IOException e) {
            Log.d(TAG, "IO Exception on restoring City Model: " + e.getLocalizedMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                return false;
            }
        }
        Log.v(TAG, "Done Restoring");
        return true;
    }

    private void setupReadObject(ObjectSlice slice) {
        if (!mUsedObjectIDs[slice.id]) {
            mUsedObjectIDs[slice.id] = true;
            int sliceRow = slice.row - OBJECTS.objectNumRows[slice.type] + 1;
            for (int c = slice.col; c < slice.col + OBJECTS.objectNumColumns[slice.type]; c++) {
                for (int r = sliceRow; r < sliceRow + OBJECTS.objectNumRows[slice.type]; r++) {
                    mObjectMap[c][r] = (short) slice.id;
                }
            }
        }
    }
}
