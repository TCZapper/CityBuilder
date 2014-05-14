package com.jasperb.citybuilder;

import java.util.LinkedList;

import android.app.Activity;
import android.widget.OverScroller;

import com.jasperb.citybuilder.Constant.BRUSH_TYPES;
import com.jasperb.citybuilder.Constant.CITY_VIEW_MODES;
import com.jasperb.citybuilder.Constant.OBJECTS;
import com.jasperb.citybuilder.Constant.TERRAIN;
import com.jasperb.citybuilder.Constant.TERRAIN_TOOLS;
import com.jasperb.citybuilder.util.ObjectEdit;
import com.jasperb.citybuilder.util.Observer;
import com.jasperb.citybuilder.util.TerrainEdit;

/**
 * NOTE: No methods within this class are thread-safe with regards to this class's content.
 * Modifications to the city model however are synchronized on the model.
 * 
 * The variable naming convention indicates how the variable should be protected.
 * TS_* indicates all reads and writes must be protected (completely Thread Safe)
 * UIS_* indicates the UI thread reads and writes to it, but the draw thread only reads, so only UI thread reading can be unprotected
 * NS_* indicates Not Safe, meaning no protection needed
 * 
 * @author Jasper
 */
public class SharedState {
    
    // Thread safe member variables (read from and written to by multiple threads)
    public float TS_FocusRow = 0, TS_FocusCol = 0;
    public OverScroller TS_Scroller = null;
    private LinkedList<TerrainEdit> TS_TerrainEdits = new LinkedList<TerrainEdit>();
    private ObjectEdit TS_ObjectEdits = null;

    // Thread safe member variables (read from by multiple threads, but only written to by UI thread)
    public int UIS_Width = 0, UIS_Height = 0;
    private float UIS_ScaleFactor;
    private int UIS_TileWidth;
    private int UIS_TileHeight;
    public CityModel UIS_CityModel = null;
    public boolean UIS_DrawGridLines = false;
    public int UIS_SelectedTerrainType = TERRAIN.GRASS;
    public int UIS_SelectedObjectType = OBJECTS.TEST2X4;
    public int UIS_Mode = CITY_VIEW_MODES.VIEW;
    public int UIS_Tool = TERRAIN_TOOLS.BRUSH;
    public int UIS_PreviousTool = TERRAIN_TOOLS.BRUSH;
    public int UIS_BrushType = BRUSH_TYPES.SQUARE1X1;
    public int UIS_SelectedObjectID = -1;
    public int UIS_FirstSelectedRow = -1, UIS_FirstSelectedCol = -1, UIS_SecondSelectedRow = -1, UIS_SecondSelectedCol = -1;
    public boolean UIS_SelectingFirstTile = true;
    public boolean UIS_InputActive = false;
    public int UIS_DestRow = -1, UIS_DestCol = -1;
    public int UIS_OrigRow = -1, UIS_OrigCol = -1;

    // Only ever read
    public Observer NS_Overlay;
    public String NS_CityName;
    public Activity NS_Activity = null;
    
    // Only used by UI thread 
    public boolean NS_DrawWithBlending = true;

    public SharedState() {
        setScaleFactor(Constant.MAXIMUM_SCALE_FACTOR);
    }

    /**
     * Copy the contents of a CityViewState object into this one.
     * 
     * @param state
     *            the CityViewState object to copy from
     */
    public void copyFrom(SharedState state) {
        TS_FocusRow = state.TS_FocusRow;
        TS_FocusCol = state.TS_FocusCol;
        UIS_Width = state.UIS_Width;
        UIS_Height = state.UIS_Height;
        setScaleFactor(state.getScaleFactor());
        UIS_CityModel = state.UIS_CityModel;
        UIS_DrawGridLines = state.UIS_DrawGridLines;
        UIS_Mode = state.UIS_Mode;
        UIS_Tool = state.UIS_Tool;
        UIS_FirstSelectedRow = state.UIS_FirstSelectedRow;
        UIS_FirstSelectedCol = state.UIS_FirstSelectedCol;
        UIS_SecondSelectedRow = state.UIS_SecondSelectedRow;
        UIS_SecondSelectedCol = state.UIS_SecondSelectedCol;
        UIS_SelectingFirstTile = state.UIS_SelectingFirstTile;
        UIS_DestRow = state.UIS_DestRow;
        UIS_DestCol = state.UIS_DestCol;
        UIS_SelectedObjectType = state.UIS_SelectedObjectType;
    }

    /**
     * Updates the CityView's state and then copies the state into the passed argument
     * 
     * @param to
     *            the object to copy the state into
     */
    public void updateThenCopyState(SharedState to) {
        // The purpose of this method is to be used by the draw thread to update the CityView's state and then retrieve that state
        // This lets us easily continuously update the CityView's state and keep the update rate synced with the FPS of the draw thread

        // Process all of the terrain edits since our last update
        // Doing all modifications to the model on the draw thread means the draw thread doesn't need to waste time with
        // thread-safety on reading from the model (which it must do many, many times).
        synchronized (UIS_CityModel) {
            for (TerrainEdit edit : TS_TerrainEdits)
                edit.setTerrain(UIS_CityModel);

            if (TS_ObjectEdits != null) {
                TS_ObjectEdits.processEdit(UIS_CityModel);
                TS_ObjectEdits = null;
            }
        }
        TS_TerrainEdits.clear();

        if (TS_Scroller == null)// Happens if cleanup was called but the draw thread is still active
            return;

        // Update the focus based off an active scroller
        // Or if the user input is not active and we are out of bounds, create a new scroller to put us in bounds
        if (!TS_Scroller.isFinished()) {
            TS_Scroller.computeScrollOffset();
            TS_FocusRow = TS_Scroller.getCurrX() / Constant.TILE_WIDTH;
            TS_FocusCol = TS_Scroller.getCurrY() / Constant.TILE_WIDTH;
        } else if (!UIS_InputActive && !isTileValid(TS_FocusRow, TS_FocusCol)) {
            int startRow = Math.round(TS_FocusRow * Constant.TILE_WIDTH);
            int startCol = Math.round(TS_FocusCol * Constant.TILE_WIDTH);
            int endRow = startRow;
            int endCol = startCol;

            if (TS_FocusRow < 0) {
                endRow = 0;
            } else if (TS_FocusRow >= UIS_CityModel.getHeight()) {
                endRow = UIS_CityModel.getHeight() * Constant.TILE_WIDTH - 1;
            }
            if (TS_FocusCol < 0) {
                endCol = 0;
            } else if (TS_FocusCol >= UIS_CityModel.getWidth()) {
                endCol = UIS_CityModel.getWidth() * Constant.TILE_WIDTH - 1;
            }
            TS_Scroller.startScroll(startRow, startCol, endRow - startRow, endCol - startCol, Constant.FOCUS_CONSTRAIN_TIME);
        }

        to.copyFrom(this);
    }

    /**
     * Queue a terrain edit of any type
     * 
     * @param edit
     *            the terrain edit to queue up
     */
    public void addTerrainEdit(TerrainEdit edit) {
        TS_TerrainEdits.add(edit);
    }

    /**
     * Creates and queues up a terrain edit that fills the selected region with the selected tile type
     */
    public void addSelectedTerrainEdit() {
        if (UIS_FirstSelectedRow == -1)
            return;
        int minRow, maxRow, minCol, maxCol;
        if (UIS_FirstSelectedRow < UIS_SecondSelectedRow) {
            minRow = UIS_FirstSelectedRow;
            maxRow = UIS_SecondSelectedRow;
        } else {
            minRow = UIS_SecondSelectedRow;
            maxRow = UIS_FirstSelectedRow;
        }
        if (UIS_FirstSelectedCol < UIS_SecondSelectedCol) {
            minCol = UIS_FirstSelectedCol;
            maxCol = UIS_SecondSelectedCol;
        } else {
            minCol = UIS_SecondSelectedCol;
            maxCol = UIS_FirstSelectedCol;
        }
        if (UIS_SecondSelectedRow == -1) {
            minRow = maxRow;
            minCol = maxCol;
        }
        TS_TerrainEdits.add(new TerrainEdit(minRow, minCol, maxRow, maxCol, UIS_SelectedTerrainType, NS_DrawWithBlending));
    }

    public int addObject(int row, int col, int type) {
        if (TS_ObjectEdits == null) {
            for (int c = col; c < col + OBJECTS.objectNumColumns[type]; c++) {
                for (int r = row; r < row + OBJECTS.objectNumRows[type]; r++) {
                    if (!isTileValid(r, c) || UIS_CityModel.getObjectID(r, c) != -1) {
                        return -3;
                    }
                }
            }
            int newObjID = UIS_CityModel.allocateNewObjectID();
            if (newObjID != -1)
                TS_ObjectEdits = new ObjectEdit(ObjectEdit.EDIT_TYPE.ADD, row, col, type, newObjID);
            return newObjID;
        }
        //It is currently acceptable to not add an object
        return -2;
    }

    public int addObject(int row, int col, int type, int id) {
        if (TS_ObjectEdits == null) {
            for (int c = col; c < col + OBJECTS.objectNumColumns[type]; c++) {
                for (int r = row; r < row + OBJECTS.objectNumRows[type]; r++) {
                    if (!isTileValid(r, c) || UIS_CityModel.getObjectID(r, c) != -1) {
                        return -3;
                    }
                }
            }
            TS_ObjectEdits = new ObjectEdit(ObjectEdit.EDIT_TYPE.ADD, row, col, type, id);
            return id;
        }
        return -2;
    }

    public void cancelMoveObject() {
        while (TS_ObjectEdits != null) {}

        TS_ObjectEdits = new ObjectEdit(ObjectEdit.EDIT_TYPE.ADD, UIS_OrigRow, UIS_OrigCol, UIS_SelectedObjectType, UIS_SelectedObjectID);
        
        UIS_SelectedObjectID = -1;
        UIS_DestRow = -1;
        UIS_DestCol = -1;
    }

    public boolean removeObject(int id, boolean block) {
        ObjectEdit newEdit = new ObjectEdit(ObjectEdit.EDIT_TYPE.REMOVE, id);
        if (TS_ObjectEdits == null) {
            TS_ObjectEdits = newEdit;
            return true;
        } else if (block) {
            if (!TS_ObjectEdits.equals(newEdit)) {
                //new object edit doesn't match existing one, so block until we can perform it
                //this is bad (blocking UI thread), but it's worth it to ensure the state isn't corrupted
                while (TS_ObjectEdits != null) {}
                removeObject(id, true);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Changes the scale factor and potentially the tile width and height.
     * 
     * @param scaleFactor
     *            the new value to set the scale factor to
     * @return true if the size of a tile changed
     */
    public boolean setScaleFactor(float scaleFactor) {
        UIS_ScaleFactor = scaleFactor;
        int newHeight = Math.round(scaleFactor * (Constant.TILE_HEIGHT / 2)) * 2;
        if (newHeight != UIS_TileHeight) {
            UIS_TileHeight = newHeight;
            UIS_TileWidth = UIS_TileHeight * 2;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the real scale factor, unaffected by the size requirements placed on the tile dimensions.
     */
    public float getScaleFactor() {
        return UIS_ScaleFactor;
    }

    /**
     * Get the tile width accounting for the scale factor and maintaining the property that tile height is divisible by 2.
     */
    public int getTileWidth() {
        return UIS_TileWidth;
    }

    /**
     * Get the tile width accounting for the scale factor and maintaining the property that tile height is divisible by 2.
     */
    public int getTileHeight() {
        return UIS_TileHeight;
    }

    /**
     * Calculate the real X coordinate relative to the real coordinates of the top-most tile using the isometric coordinates.
     * Takes row/column unscaled and returns the real X coordinate scaled down by the scale factor.
     */
    public int isoToRealXDownscaling(int row, int col) {
        return (UIS_TileWidth / 2) * (col - row);
    }

    /**
     * Calculate the real Y coordinate relative to the real coordinates of the top-most tile using the isometric coordinates.
     * Takes row/column unscaled and returns the real Y coordinate scaled down by the scale factor.
     */
    public int isoToRealYDownscaling(int row, int col) {
        return (UIS_TileHeight / 2) * (col + row);
    }

    /**
     * Calculate the real X coordinate relative to the real coordinates of the top-most tile using the isometric coordinates.
     * Takes row/column unscaled and returns the real X coordinate scaled down by the scale factor.
     */
    public int isoToRealXDownscaling(float row, float col) {
        return Math.round((UIS_TileWidth / 2) * (col - row));
    }

    /**
     * Calculate the real Y coordinate relative to the real coordinates of the top-most tile using the isometric coordinates.
     * Takes row/column unscaled and returns the real Y coordinate scaled down by the scale factor.
     */
    public int isoToRealYDownscaling(float row, float col) {
        return Math.round((UIS_TileHeight / 2) * (col + row));
    }

    /**
     * Calculate the isometric row coordinate using the real coordinates relative to those of the top-most tile.
     * Takes the real coordinates and scales them up by the scale factor.
     */
    public float realToIsoRowUpscaling(int x, int y) {
        return (y / (float) UIS_TileHeight) - (x / (float) UIS_TileWidth);
    }

    /**
     * Calculate the isometric column coordinate using the real coordinates relative to those of the top-most tile.
     * Takes the real coordinates and scales them up by the scale factor.
     */
    public float realToIsoColUpscaling(int x, int y) {
        return (y / (float) UIS_TileHeight) + (x / (float) UIS_TileWidth);
    }

    /**
     * Returns true if the tile exists in the city model.
     * 
     * @param row
     *            the row of the tile to test
     * @param col
     *            the column of the tile to test
     */
    public boolean isTileValid(int row, int col) {
        return row >= 0 && col >= 0 && row < UIS_CityModel.getHeight() && col < UIS_CityModel.getWidth();
    }

    /**
     * Returns true if the tile exists in the city model.
     * 
     * @param row
     *            the row of the tile to test
     * @param col
     *            the column of the tile to test
     */
    public boolean isTileValid(float row, float col) {
        return row >= 0 && col >= 0 && row < UIS_CityModel.getHeight() && col < UIS_CityModel.getWidth();
    }

    /**
     * Return true if and only if a tile with its top corner drawn at real coordinates (x,y) would be visible in the view.
     * 
     * @param x
     *            the x coordinate for where the tile would be drawn in the view's canvas
     * @param y
     *            the y coordinate for where the tile would be drawn in the view's canvas
     */
    public boolean isTileVisible(int x, int y) {
        return !(y >= UIS_Height
                || y + UIS_TileHeight < 0
                || x + UIS_TileWidth / 2 < 0
                || x - UIS_TileWidth / 2 >= UIS_Width);
    }

    /**
     * Get the real X coordinate for the origin tile (row 0, column 0).
     */
    public int getOriginX() {
        return UIS_Width / 2 - isoToRealXDownscaling(TS_FocusRow, TS_FocusCol);
    }

    /**
     * Get the real Y coordinate for the origin tile (row 0, column 0).
     */
    public int getOriginY() {
        return UIS_Height / 2 - isoToRealYDownscaling(TS_FocusRow, TS_FocusCol);
    }

    /**
     * Force the scroller to stop, but not before updating the current location
     */
    public void forceStopScroller() {
        if (!TS_Scroller.isFinished()) {
            TS_Scroller.computeScrollOffset();//compute current offset before forcing finish
            TS_FocusRow = TS_Scroller.getCurrX() / Constant.TILE_WIDTH;
            TS_FocusCol = TS_Scroller.getCurrY() / Constant.TILE_WIDTH;
            TS_Scroller.forceFinished(true);
        }
    }

    /**
     * Reset the components of the terrain selection tool
     */
    public void resetSelectTool() {
        UIS_FirstSelectedRow = -1;
        UIS_FirstSelectedCol = -1;
        UIS_SecondSelectedRow = -1;
        UIS_SecondSelectedCol = -1;
        UIS_SelectingFirstTile = true;
    }

    /**
     * Notify the associated overlay that it should update.
     */
    public void notifyOverlay() {
        NS_Overlay.update();
    }
}
