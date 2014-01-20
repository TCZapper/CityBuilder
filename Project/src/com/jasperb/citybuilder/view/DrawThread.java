/**
 * 
 */
package com.jasperb.citybuilder.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;

import com.jasperb.citybuilder.util.Common;
import com.jasperb.citybuilder.util.Constant;
import com.jasperb.citybuilder.util.PerfTools;
import com.jasperb.citybuilder.util.TileBitmaps;

/**
 * @author Jasper
 * 
 */
public class DrawThread extends Thread {
    public static final String TAG = "DrawThread";

    public static final boolean LOG_TTD = true;//Time To Draw

    //CityView variables
    private TileBitmaps mTileBitmaps = null;
    private Paint mGridPaint = null;
    private Paint mBitmapPaint = null;
    private Matrix mMatrix = null;
    private SurfaceHolder mSurfaceHolder = null;
    private CityViewState mState = new CityViewState();
    private CityViewState mNextState = new CityViewState();
    private final Object mStateLock = new Object();
    private boolean mRun = true;

    public DrawThread(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
    }

    public void setDrawState(CityViewState state) {
        synchronized (mStateLock) {
            mNextState.copy(state);
        }
    }

    public void getDrawState() {
        synchronized (mStateLock) {
            mState.copy(mNextState);
        }
    }

    /**
     * Initialize and allocate the necessary components of the view, except those that depend on the view size or city size
     */
    protected void init() {
        synchronized (mSurfaceHolder) {
            mTileBitmaps = new TileBitmaps();

            mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mGridPaint.setStyle(Paint.Style.STROKE);
            mGridPaint.setStrokeWidth(0);// thinnest line is 0 width
            mGridPaint.setColor(Color.WHITE);

            // Use for scaling bitmaps when drawing them to the canvas
            // Anti-aliasing helps shrink small defects on the edges, while filter bitmap makes the edges much smoother
            mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

            mMatrix = new Matrix();
        }
    }
    
    protected void stopThread() {
        synchronized (mSurfaceHolder) {
            mRun = false;
        }
    }

    protected void cleanup() {
        synchronized (mSurfaceHolder) {
            mTileBitmaps = null;
            mGridPaint = null;
            mBitmapPaint = null;
            mMatrix = null;
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
                    getDrawState();
                    mState.mWidth = c.getWidth();
                    mState.mHeight = c.getHeight();
                    if (mState.mWidth != 0) {
                        synchronized (mSurfaceHolder) {
                            if(mRun) drawGround(c);
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

    private void drawGround(Canvas canvas) {
        Log.d(TAG, "DRAW GROUND");
        long startTime = System.currentTimeMillis();
        canvas.drawColor(Color.BLACK);// Clear the canvas

        // Calculate real coordinates for the top-most tile based off the focus iso coordinates being the centre of the screen
        float realTopX = mState.mWidth / 2 - (Common.isoToRealX(mState.mFocusRow, mState.mFocusCol) * mState.mScaleFactor);
        float realTopY = mState.mHeight / 2 - (Common.isoToRealY(mState.mFocusRow, mState.mFocusCol) * mState.mScaleFactor);

        // Shift the bitmap left to horizontally center the tile around 0,0 and subtract 1 because the image isn't drawn perfectly centered
        float bitmapOffsetX = (-Constant.TILE_WIDTH / 2 - 1) * mState.mScaleFactor;

        //Draw all tiles for testing
//        for (int col = 0; col < mState.mCityModel.getWidth(); col++) {
//            for (int row = 0; row < mState.mCityModel.getHeight(); row++) {
//                // Log.d(TAG,"Paint Tile: " + row + " : " + col);
//                mMatrix.setScale(mState.mScaleFactor, mState.mScaleFactor);
//                mMatrix.postTranslate(Common.isoToRealX(row, col) * mState.mScaleFactor + realTopX + bitmapOffsetX,
//                        Common.isoToRealY(row, col) * mState.mScaleFactor + realTopY);
//                mBufferCanvas.drawBitmap(mTileBitmaps.getBitmap(TERRAIN.DIRT), mMatrix, mBitmapPaint);
//            }
//        }

        // Calculate the tile that is at the top left corner of the view, and the one at the bottom right corner
        int topLeftRow = (int) Math.floor(Common.realToIsoRow(-realTopX / mState.mScaleFactor, -realTopY / mState.mScaleFactor));
        int topLeftCol = (int) Math.floor(Common.realToIsoCol(-realTopX / mState.mScaleFactor, -realTopY / mState.mScaleFactor));
        int bottomRightRow = (int) Math.floor(Common.realToIsoRow((-realTopX + mState.mWidth) / mState.mScaleFactor,
                (-realTopY + mState.mHeight) / mState.mScaleFactor));
        int bottomRightCol = (int) Math.floor(Common.realToIsoCol((-realTopX + mState.mWidth) / mState.mScaleFactor,
                (-realTopY + mState.mHeight) / mState.mScaleFactor));
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
        if (Common.isoToRealX(topLeftRow, topLeftCol) * mState.mScaleFactor + realTopX > 0) {
            leftBoundRow++;
        }

        int rightBoundRow = bottomRightRow;
        int rightBoundCol = bottomRightCol;
        if (Common.isoToRealX(bottomRightRow, bottomRightCol) * mState.mScaleFactor + realTopX < mState.mWidth) {
            rightBoundRow--;
        }

        int topBoundRow = topLeftRow;
        int topBoundCol = topLeftCol;
        if ((Common.isoToRealY(topLeftRow, topLeftCol) + (Constant.TILE_HEIGHT / 2)) * mState.mScaleFactor + realTopY > 0) {
            topBoundRow--;
        }

        int bottomBoundRow = bottomRightRow;
        int bottomBoundCol = bottomRightCol;
        if ((Common.isoToRealY(bottomRightRow, bottomRightCol) + (Constant.TILE_HEIGHT / 2)) * mState.mScaleFactor + realTopY < mState.mHeight) {
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
        if (Common.isTileValid(mState.mCityModel, firstRow, firstCol)//is the first tile to draw even valid/visible?
                && Common.isTileVisible(mState.mScaleFactor, mState.mWidth, mState.mHeight,
                        Common.isoToRealX(firstRow, firstCol) * mState.mScaleFactor + realTopX,
                        Common.isoToRealY(firstRow, firstCol) * mState.mScaleFactor + realTopY)) {
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
                    // Time to draw the terrain to the buffer. Technically, scaling for every tile is not ideal, but scaling at the moment
                    // we draw onto the buffer canvas has two benefits:
                    // 1. No memory allocations needed when the scale factor changes
                    // 2. It does not have any issues lining up tiles, unlike all other methods tried
//                    Log.d(TAG, "Paint Tile: " + row + " : " + col);
                    mMatrix.setScale(mState.mScaleFactor, mState.mScaleFactor);
                    mMatrix.postTranslate(Common.isoToRealX(row, col) * mState.mScaleFactor + realTopX + bitmapOffsetX,
                            Common.isoToRealY(row, col) * mState.mScaleFactor + realTopY);
                    canvas.drawBitmap(mTileBitmaps.getBitmap(mState.mCityModel.getTerrain(row, col)), mMatrix, mBitmapPaint);
                }
            }

            // Draw grid lines
            if (mState.mDrawGridLines) {
                // For simplicity we only draw the grid lines for visible rows and columns, but we only make a small effort at
                // preventing drawing outside of the view (every line should have an on-screen section, but we may extend it too far)
                for (int col = firstCol; col <= lastCol + 1; col++) {
                    canvas.drawLine(Common.isoToRealX(minRow, col) * mState.mScaleFactor + realTopX,
                            Common.isoToRealY(minRow, col) * mState.mScaleFactor + realTopY,
                            Common.isoToRealX(maxRow + 1, col) * mState.mScaleFactor + realTopX,
                            Common.isoToRealY(maxRow + 1, col) * mState.mScaleFactor + realTopY, mGridPaint);
                }
                for (int row = minRow; row <= maxRow + 1; row++) {
                    canvas.drawLine(Common.isoToRealX(row, firstCol) * mState.mScaleFactor + realTopX,
                            Common.isoToRealY(row, firstCol) * mState.mScaleFactor + realTopY,
                            Common.isoToRealX(row, lastCol + 1) * mState.mScaleFactor + realTopX,
                            Common.isoToRealY(row, lastCol + 1) * mState.mScaleFactor + realTopY, mGridPaint);
                }
            }
            if (LOG_TTD) {
                long endTime = System.currentTimeMillis();
                Log.d(TAG + "TTD", "" + PerfTools.CalcAverageTick((int) (endTime - startTime)));
            }
        } else {
            Log.d(TAG, "NOTHING TO DRAW");
        }

        // Draw a very thin plus sign spanning the entire screen that indicates the middle of the screen
//         mBufferCanvas.drawLine(mState.mWidth / 2, 0, mState.mWidth / 2, mState.mHeight, mGridPaint);
//         mBufferCanvas.drawLine(0, mState.mHeight / 2, mState.mWidth, mState.mHeight / 2, mGridPaint);
    }
}
