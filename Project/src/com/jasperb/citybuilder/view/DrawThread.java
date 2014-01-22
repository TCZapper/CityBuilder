/**
 * 
 */
package com.jasperb.citybuilder.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;

import com.jasperb.citybuilder.util.PerfTools;
import com.jasperb.citybuilder.util.TileBitmaps;

/**
 * @author Jasper
 * 
 */
public class DrawThread extends Thread {
    /**
     * String used for identifying this class
     */
    public static final String TAG = "DrawThread";

    public static final boolean LOG_TTD = true;//Time To Draw

    //CityView variables
    private TileBitmaps mTileBitmaps = null;
    private Paint mGridPaint = null;
    private SurfaceHolder mSurfaceHolder = null;
    private CityViewState mState = new CityViewState();
    private CityViewState mNextState = new CityViewState();
    private final Object mStateLock = new Object();
    private boolean mRun = true;

    public DrawThread(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
    }

    /**
     * Updates the state to be used for the next time we draw.
     * Only the most recent state will be used when the thread goes to draw.
     * @param state
     */
    public void setDrawState(CityViewState state) {
        synchronized (mStateLock) {
            mNextState.copy(state);
        }
    }

    /**
     * Retrieve the most recent state and store it separately so other threads don't interfere with it
     */
    private void getDrawState() {
        synchronized (mStateLock) {
            mState.copy(mNextState);
        }
    }

    /**
     * Initialize and allocate the necessary components of the view, except those that depend on the view size or city size
     */
    protected void init(Context context) {
        synchronized (mSurfaceHolder) {
            synchronized (mStateLock) {
                mTileBitmaps = new TileBitmaps(context);
                mTileBitmaps.remakeBitmaps(mState);
            }

            mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mGridPaint.setStyle(Paint.Style.STROKE);
            mGridPaint.setStrokeWidth(0);// thinnest line is 0 width
            mGridPaint.setARGB(255, 225, 225, 225);
        }
    }

    /**
     * Stop the thread after the current drawing finishes
     */
    protected void stopThread() {
        synchronized (mSurfaceHolder) {
            mRun = false;
        }
    }

    /**
     * Cleanup the components allocated by init()
     */
    protected void cleanup() {
        synchronized (mSurfaceHolder) {
            mTileBitmaps = null;
            mGridPaint = null;
        }
    }

    @Override
    public void run() {
        Log.d(TAG, "RUN");
        while (mRun) {
            Canvas c = null;
            try {
                c = mSurfaceHolder.lockCanvas(null);
                if (c != null) {
                    float oldTileHeight = mState.getTileHeight();
                    getDrawState();
                    if (mState.mWidth != 0 && mState.mWidth == c.getWidth() && mState.mHeight == c.getHeight()) {
                        if (oldTileHeight != mState.getTileHeight()) {
                            mTileBitmaps.remakeBitmaps(mState);
                        }
                        synchronized (mSurfaceHolder) {
                            if (mRun)
                                drawGround(c);
                        }
                    }
                }
            } finally {
                if (c != null) {
                    mSurfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }
        cleanup();
        Log.d(TAG, "DONE RUN");
    }

    /**
     * Draw the ground (e.g. grass, water, gridlines) onto the provided canvas.
     * The canvas should have the same dimensions as defined by mState.mWidth and mState.mHeight
     * @param canvas the canvas to draw onto
     */
    private void drawGround(Canvas canvas) {
        long startTime = System.currentTimeMillis();
        canvas.drawColor(Color.BLACK);// Clear the canvas

        // Calculate real coordinates for the top-most tile based off the focus iso coordinates being the centre of the screen
        int realTopX = mState.mWidth / 2 - mState.isoToRealXDownscaling(mState.mFocusRow, mState.mFocusCol);
        int realTopY = mState.mHeight / 2 - mState.isoToRealYDownscaling(mState.mFocusRow, mState.mFocusCol);

        // Shift the bitmap left to horizontally center the tile around 0,0
        int bitmapOffsetX = -mState.getTileWidth() / 2;

        // Calculate the tile that is at the top left corner of the view, and the one at the bottom right corner
        int topLeftRow = (int) Math.floor(mState.realToIsoRowUpscaling(-realTopX, -realTopY));
        int topLeftCol = (int) Math.floor(mState.realToIsoColUpscaling(-realTopX, -realTopY));
        int bottomRightRow = (int) Math.floor(mState.realToIsoRowUpscaling((-realTopX + mState.mWidth), (-realTopY + mState.mHeight)));
        int bottomRightCol = (int) Math.floor(mState.realToIsoColUpscaling((-realTopX + mState.mWidth), (-realTopY + mState.mHeight)));
//        Log.d(TAG, "TL TILE: " + topLeftRow + " : " + topLeftCol);
//        Log.d(TAG, "BR TILE: " + bottomRightRow + " : " + bottomRightCol);

        // Calculate the boundaries of the view
        // The boundaries are defined by a tile that crosses the edge of the view
        // We base it off the tile in either the top left or bottom right corner
        // If this corner tile has the majority of its contents on the visual side of the boundary, we shift the row of the boundary
        // by one such that we have a tile with the majority of its contents on the non-visual side
        // It's based off the knowledge that any tile (row + offset, column + offset) shares the same y coordinate as (row, column)
        // As well that any tile (row + offset, column - offset) shares the same x coordinate as (row, column)
        // This means that knowing the offset, and for a given row or column, we can determine the tile with the majority
        // of its contents on the non-visible side of the boundary
        // This is useful as the first tile we want to draw is always the one with the majority of its content on the non-visible side
        int leftBoundRow = topLeftRow;
        int leftBoundCol = topLeftCol;
        if (mState.isoToRealXDownscaling(topLeftRow, topLeftCol) + realTopX > 0) {
            leftBoundRow++;
        }

        int rightBoundRow = bottomRightRow;
        int rightBoundCol = bottomRightCol;
        if (mState.isoToRealXDownscaling(bottomRightRow, bottomRightCol) + realTopX < mState.mWidth) {
            rightBoundRow--;
        }

        int topBoundRow = topLeftRow;
        int topBoundCol = topLeftCol;
        if ((mState.isoToRealYDownscaling(topLeftRow, topLeftCol) + (mState.getTileHeight() / 2)) + realTopY > 0) {
            topBoundRow--;
        }

        int bottomBoundRow = bottomRightRow;
        int bottomBoundCol = bottomRightCol;
        if ((mState.isoToRealYDownscaling(bottomRightRow, bottomRightCol) + (mState.getTileHeight() / 2)) + realTopY < mState.mHeight) {
            bottomBoundRow++;
        }

        // To calculate the first column to draw we base it off a simple idea:
        // When the topBoundRow is less than 0, the city is can only possibly cross the left edge
        // When topBoundRow is larger than the height of the model, the city can only possibly cross the top edge
        // Finally, if neither of those properties are true, then either the top-left corner has a valid tile
        // or the tile (topLeftRow, 0) is below the top edge and right of the left edge, and thus the first column is the 0th
        int firstCol;
        if (topBoundRow < 0) {
            firstCol = Math.max(0, leftBoundCol - leftBoundRow);
        } else if (topBoundRow >= mState.mCityModel.getHeight()) {
            firstCol = Math.max(0, topBoundCol + (topBoundRow - (mState.mCityModel.getHeight() - 1)));
        } else {
            firstCol = Math.max(0, topLeftCol);
        }

        int firstRow = Math.max(0, Math.max(topBoundRow - (firstCol - topBoundCol),//where does firstCol cross the bottom edge
                rightBoundRow - (rightBoundCol - firstCol)));//where does firstCol cross the right edge

//        Log.d(TAG, "DRAW FIRST: " + firstRow + " : " + firstCol);
        if (mState.isTileValid(firstRow, firstCol)//is the first tile to draw even valid/visible?
                && mState.isTileVisible(mState.isoToRealXDownscaling(firstRow, firstCol) + realTopX,
                        mState.isoToRealYDownscaling(firstRow, firstCol) + realTopY)) {
            int minRow = mState.mCityModel.getHeight() - 1, maxRow = 0;

            int lastCol;// Calculate last column to draw by using same logic as first column (except bottom/right boundaries)
            if (bottomRightRow < 0) {
                lastCol = Math.min(mState.mCityModel.getWidth() - 1, bottomBoundCol + bottomBoundRow);
            } else if (bottomRightRow >= mState.mCityModel.getHeight()) {
                lastCol = Math.min(mState.mCityModel.getWidth() - 1, rightBoundCol
                        - (rightBoundRow - (mState.mCityModel.getHeight() - 1)));
            } else {
                lastCol = Math.min(mState.mCityModel.getWidth() - 1, bottomRightCol);
            }

            for (int col = firstCol; col <= lastCol; col++) {
                // Find the last row by figuring out the rows where this column crosses the bottom and left edge,
                // and draw up to the whichever row corresponds edge we hit first
                int lastRow = Math.min(mState.mCityModel.getHeight() - 1,
                        Math.min(leftBoundRow + (col - leftBoundCol), bottomBoundRow + (bottomBoundCol - col)));
                // Find the first row by checking against where it collides with the right and top edges, same as with the last row
                int row = Math.max(0, Math.max(topBoundRow - (col - topBoundCol), rightBoundRow - (rightBoundCol - col)));

                if (row < minRow)
                    minRow = row;
                if (lastRow > maxRow)
                    maxRow = lastRow;

                for (; row <= lastRow; row++) {
                    // Time to draw the terrain to the buffer. TileBitmaps handles resizing the tiles, we just draw/position them
//                    Log.d(TAG, "Paint Tile: " + row + " : " + col);
                    canvas.drawBitmap(mTileBitmaps.getBitmap(mState.mCityModel.getTerrain(row, col)),
                            mState.isoToRealXDownscaling(row, col) + realTopX + bitmapOffsetX,
                            mState.isoToRealYDownscaling(row, col) + realTopY, null);
                }
            }

            // Draw grid lines (legacy)
//            if (mState.mDrawGridLines) {
//                // For simplicity we only draw the grid lines for visible rows and columns, but we only make a small effort at
//                // preventing drawing outside of the view (every line should have an on-screen section, but we may extend it too far)
//                for (int col = firstCol; col <= lastCol + 1; col++) {
//                    canvas.drawLine(mState.isoToRealXDownscaling(minRow, col) + realTopX,
//                            mState.isoToRealYDownscaling(minRow, col) + realTopY,
//                            mState.isoToRealXDownscaling(maxRow + 1, col) + realTopX,
//                            mState.isoToRealYDownscaling(maxRow + 1, col) + realTopY, mGridPaint);
//                }
//                for (int row = minRow; row <= maxRow + 1; row++) {
//                    canvas.drawLine(mState.isoToRealXDownscaling(row, firstCol) + realTopX,
//                            mState.isoToRealYDownscaling(row, firstCol) + realTopY,
//                            mState.isoToRealXDownscaling(row, lastCol + 1) + realTopX,
//                            mState.isoToRealYDownscaling(row, lastCol + 1) + realTopY, mGridPaint);
//                }
//            }
        } else {
            Log.d(TAG, "NOTHING TO DRAW");
        }

        // Draw a very thin plus sign spanning the entire screen that indicates the middle of the screen
//         mBufferCanvas.drawLine(mState.mWidth / 2, 0, mState.mWidth / 2, mState.mHeight, mGridPaint);
//         mBufferCanvas.drawLine(0, mState.mHeight / 2, mState.mWidth, mState.mHeight / 2, mGridPaint);
        
        if (LOG_TTD) {
            long endTime = System.currentTimeMillis();
            Log.v("TTD_" + TAG, "" + PerfTools.CalcAverageTick((int) (endTime - startTime)));
        }
    }
}
