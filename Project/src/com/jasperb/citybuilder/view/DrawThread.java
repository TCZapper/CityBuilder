/**
 * 
 */
package com.jasperb.citybuilder.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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

    private TileBitmaps mTileBitmaps = null;
    private Paint mGridPaint = null, mSelectionPaint = null, mSelectedTilePaint = null;
    private SurfaceHolder mSurfaceHolder = null;
    private CityViewState mDrawState = new CityViewState();
    private CityViewState mMainState = null;
    private boolean mRun = true;
    //Boundary calculations are costly, so we store them and reuse them during a single drawing pass
    private int mOriginX, mOriginY, mBitmapOffsetX, mTopLeftRow, mTopLeftCol, mBottomRightRow, mBottomRightCol, mFirstRow, mFirstCol,
            mLastCol, mLeftBoundRow, mLeftBoundCol, mRightBoundRow, mRightBoundCol, mTopBoundRow, mTopBoundCol, mBottomBoundRow,
            mBottomBoundCol, mMinRow, mMaxRow;
    private long startTime;

    public DrawThread(SurfaceHolder surfaceHolder, CityViewState state) {
        mSurfaceHolder = surfaceHolder;
        mMainState = state;

        mTileBitmaps = new TileBitmaps();
        mTileBitmaps.remakeBitmaps(mDrawState);

        mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGridPaint.setStyle(Paint.Style.STROKE);
        mGridPaint.setStrokeWidth(1.5f);// thinnest line is 0 width
        mGridPaint.setARGB(255, 170, 170, 170);

        mSelectedTilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSelectedTilePaint.setStyle(Paint.Style.STROKE);
        mSelectedTilePaint.setStrokeWidth(1.5f);
        mSelectedTilePaint.setARGB(255, 170, 255, 170);

        mSelectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSelectionPaint.setStyle(Paint.Style.FILL);
        mSelectionPaint.setARGB(100, 0, 255, 0);
    }

    /**
     * Stop the thread after the current drawing finishes
     */
    protected void stopThread() {
        synchronized (mSurfaceHolder) {
            mRun = false;
        }
    }

    @Override
    public void run() {
        Log.v(TAG, "RUN");
        while (mRun) {
            Canvas c = null;
            try {
                c = mSurfaceHolder.lockCanvas();
                if (c != null) {
                    synchronized (mSurfaceHolder) {
                        if (mRun) {
                            float oldTileHeight = mDrawState.getTileHeight();
                            boolean oldDrawGridLines = mDrawState.mDrawGridLines;
                            synchronized (mMainState) {
                                mMainState.updateThenCopyState(mDrawState);
                            }
                            if (mDrawState.mWidth != 0 && mDrawState.mWidth == c.getWidth() && mDrawState.mHeight == c.getHeight()) {
                                if (oldTileHeight != mDrawState.getTileHeight() || oldDrawGridLines != mDrawState.mDrawGridLines) {
                                    mTileBitmaps.remakeBitmaps(mDrawState);
                                }
                                // Log.v(TAG,"DRAW AT: " + mState.mFocusRow + " : " + mState.mFocusRow);

                                setStartTime();

                                c.drawARGB(255, 38, 76, 45);// Clear the canvas
                                calculateBoundaries();
                                if (visibileTilesExist()) {
                                    drawGround(c);
                                    drawSelection(c);
                                } else {
                                    Log.v(TAG, "NOTHING TO DRAW");
                                }
                                //drawCenterLines(c);

                                setEndTime();
                            }
                        }
                    }
                }
            } finally {// Ensure we actually release the lock
                if (c != null) {
                    mSurfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }
        Log.v(TAG, "DONE RUN");
    }

    private void calculateBoundaries() {
        // Calculate real coordinates for the top-most tile based off the focus iso coordinates being the centre of the screen
        mOriginX = mDrawState.getOriginX();
        mOriginY = mDrawState.getOriginY();

        // Shift the bitmap left to horizontally center the tile around 0,0
        mBitmapOffsetX = -mDrawState.getTileWidth() / 2;

        // Calculate the tile that is at the top left corner of the view, and the one at the bottom right corner
        mTopLeftRow = (int) Math.floor(mDrawState.realToIsoRowUpscaling(-mOriginX, -mOriginY));
        mTopLeftCol = (int) Math.floor(mDrawState.realToIsoColUpscaling(-mOriginX, -mOriginY));
        mBottomRightRow = (int) Math.floor(mDrawState.realToIsoRowUpscaling((-mOriginX + mDrawState.mWidth),
                (-mOriginY + mDrawState.mHeight)));
        mBottomRightCol = (int) Math.floor(mDrawState.realToIsoColUpscaling((-mOriginX + mDrawState.mWidth),
                (-mOriginY + mDrawState.mHeight)));
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
        mLeftBoundRow = mTopLeftRow;
        mLeftBoundCol = mTopLeftCol;
        if (mDrawState.isoToRealXDownscaling(mTopLeftRow, mTopLeftCol) + mOriginX > 0) {
            mLeftBoundRow++;
        }

        mRightBoundRow = mBottomRightRow;
        mRightBoundCol = mBottomRightCol;
        if (mDrawState.isoToRealXDownscaling(mBottomRightRow, mBottomRightCol) + mOriginX < mDrawState.mWidth) {
            mRightBoundRow--;
        }

        mTopBoundRow = mTopLeftRow;
        mTopBoundCol = mTopLeftCol;
        if ((mDrawState.isoToRealYDownscaling(mTopLeftRow, mTopLeftCol) + (mDrawState.getTileHeight() / 2)) + mOriginY > 0) {
            mTopBoundRow--;
        }

        mBottomBoundRow = mBottomRightRow;
        mBottomBoundCol = mBottomRightCol;
        if ((mDrawState.isoToRealYDownscaling(mBottomRightRow, mBottomRightCol) + (mDrawState.getTileHeight() / 2)) + mOriginY < mDrawState.mHeight) {
            mBottomBoundRow++;
        }

        // To calculate the first column to draw we base it off a simple idea:
        // When the topBoundRow is less than 0, the city is can only possibly cross the left edge
        // When topBoundRow is larger than the height of the model, the city can only possibly cross the top edge
        // Finally, if neither of those properties are true, then either the top-left corner has a valid tile
        // or the tile (topLeftRow, 0) is below the top edge and right of the left edge, and thus the first column is the 0th
        if (mTopBoundRow < 0) {
            mFirstCol = Math.max(0, mLeftBoundCol - mLeftBoundRow);
        } else if (mTopBoundRow >= mDrawState.mCityModel.getHeight()) {
            mFirstCol = Math.max(0, mTopBoundCol + (mTopBoundRow - (mDrawState.mCityModel.getHeight() - 1)));
        } else {
            mFirstCol = Math.max(0, mTopLeftCol);
        }

        mFirstRow = Math.max(0, Math.max(mTopBoundRow - (mFirstCol - mTopBoundCol),//where does firstCol cross the bottom edge
                mRightBoundRow - (mRightBoundCol - mFirstCol)));//where does firstCol cross the right edge

        // Calculate last column to draw by using same logic as first column (except bottom/right boundaries)
        if (mBottomRightRow < 0) {
            mLastCol = Math.min(mDrawState.mCityModel.getWidth() - 1, mBottomBoundCol + mBottomBoundRow);
        } else if (mBottomRightRow >= mDrawState.mCityModel.getHeight()) {
            mLastCol = Math.min(mDrawState.mCityModel.getWidth() - 1, mRightBoundCol
                    - (mRightBoundRow - (mDrawState.mCityModel.getHeight() - 1)));
        } else {
            mLastCol = Math.min(mDrawState.mCityModel.getWidth() - 1, mBottomRightCol);
        }

        //For now, let drawGround handle this
        mMinRow = mDrawState.mCityModel.getHeight() - 1;
        mMaxRow = 0;
    }

    /**
     * Draw the ground (e.g. grass, water, gridlines) onto the provided canvas.
     * The canvas should have the same dimensions as defined by mState.mWidth and mState.mHeight
     * 
     * @param canvas
     *            the canvas to draw onto
     */
    private void drawGround(Canvas canvas) {
        //Log.d(TAG, "DRAW FIRST: " + firstRow + " : " + firstCol);
        for (int col = mFirstCol; col <= mLastCol; col++) {
            // Find the last row by figuring out the rows where this column crosses the bottom and left edge,
            // and draw up to the whichever row corresponds edge we hit first
            int lastRow = Math.min(mDrawState.mCityModel.getHeight() - 1,
                    Math.min(mLeftBoundRow + (col - mLeftBoundCol), mBottomBoundRow + (mBottomBoundCol - col)));
            // Find the first row by checking against where it collides with the right and top edges, same as with the last row
            int row = Math.max(0, Math.max(mTopBoundRow - (col - mTopBoundCol), mRightBoundRow - (mRightBoundCol - col)));

            if (row < mMinRow)
                mMinRow = row;
            if (lastRow > mMaxRow)
                mMaxRow = lastRow;

            for (; row <= lastRow; row++) {
                // Time to draw the terrain to the buffer. TileBitmaps handles resizing the tiles, we just draw/position them
//                    Log.d(TAG, "Paint Tile: " + row + " : " + col);
                canvas.drawBitmap(mTileBitmaps.getBitmap(mDrawState.mCityModel.getTerrain(row, col)),
                        mDrawState.isoToRealXDownscaling(row, col) + mOriginX + mBitmapOffsetX,
                        mDrawState.isoToRealYDownscaling(row, col) + mOriginY, null);
            }
        }
        // Draw grid lines for the outer edges of the world
        // We have to offset the bottom edge lines because of the extra bits of every tile that stick out from the bottom edges
        if (mFirstCol == 0) {
            canvas.drawLine(mDrawState.isoToRealXDownscaling(mMinRow, 0) + mOriginX,
                    mDrawState.isoToRealYDownscaling(mMinRow, 0) + mOriginY,
                    mDrawState.isoToRealXDownscaling(mMaxRow + 1, 0) + mOriginX,
                    mDrawState.isoToRealYDownscaling(mMaxRow + 1, 0) + mOriginY, mGridPaint);
        }
        if (mLastCol == mDrawState.mCityModel.getWidth() - 1) {
            canvas.drawLine(mDrawState.isoToRealXDownscaling(mMinRow, mLastCol + 1) + mOriginX + 1,
                    mDrawState.isoToRealYDownscaling(mMinRow, mLastCol + 1) + mOriginY,
                    mDrawState.isoToRealXDownscaling(mMaxRow + 1, mLastCol + 1) + mOriginX,
                    mDrawState.isoToRealYDownscaling(mMaxRow + 1, mLastCol + 1) + mOriginY + 1, mGridPaint);
        }
        if (mMinRow == 0) {
            canvas.drawLine(mDrawState.isoToRealXDownscaling(0, mFirstCol) + mOriginX,
                    mDrawState.isoToRealYDownscaling(0, mFirstCol) + mOriginY,
                    mDrawState.isoToRealXDownscaling(0, mLastCol + 1) + mOriginX,
                    mDrawState.isoToRealYDownscaling(0, mLastCol + 1) + mOriginY, mGridPaint);
        }
    }

    private void drawSelection(Canvas canvas) {
        int minRow, maxRow, minCol, maxCol;
        if (mDrawState.mFirstSelectedRow != -1) {
            Path path = new Path();
            if (mDrawState.mSecondSelectedRow == -1) {
                minRow = mDrawState.mFirstSelectedRow;
                maxRow = mDrawState.mFirstSelectedRow + 1;
                minCol = mDrawState.mFirstSelectedCol;
                maxCol = mDrawState.mFirstSelectedCol + 1;
            } else {
                if (mDrawState.mFirstSelectedRow < mDrawState.mSecondSelectedRow) {
                    minRow = mDrawState.mFirstSelectedRow;
                    maxRow = mDrawState.mSecondSelectedRow + 1;
                } else {
                    minRow = mDrawState.mSecondSelectedRow;
                    maxRow = mDrawState.mFirstSelectedRow + 1;
                }
                if (mDrawState.mFirstSelectedCol < mDrawState.mSecondSelectedCol) {
                    minCol = mDrawState.mFirstSelectedCol;
                    maxCol = mDrawState.mSecondSelectedCol + 1;
                } else {
                    minCol = mDrawState.mSecondSelectedCol;
                    maxCol = mDrawState.mFirstSelectedCol + 1;
                }

                int topX, topY;
                if (mDrawState.mSelectingFirstTile) {
                    topX = mDrawState.isoToRealXDownscaling(mDrawState.mFirstSelectedRow, mDrawState.mFirstSelectedCol) + mOriginX;
                    topY = mDrawState.isoToRealYDownscaling(mDrawState.mFirstSelectedRow, mDrawState.mFirstSelectedCol) + mOriginY;
                } else {
                    topX = mDrawState.isoToRealXDownscaling(mDrawState.mSecondSelectedRow, mDrawState.mSecondSelectedCol) + mOriginX;
                    topY = mDrawState.isoToRealYDownscaling(mDrawState.mSecondSelectedRow, mDrawState.mSecondSelectedCol) + mOriginY;
                }
                path.moveTo(topX, topY);//top
                path.lineTo(topX + mDrawState.getTileWidth() / 2, topY + mDrawState.getTileHeight() / 2);//right
                path.lineTo(topX, topY + mDrawState.getTileHeight());//bottom
                path.lineTo(topX - mDrawState.getTileWidth() / 2, topY + mDrawState.getTileHeight() / 2);//left
                path.close();
                canvas.drawPath(path, mSelectionPaint);
                canvas.drawPath(path, mSelectedTilePaint);
                path.reset();
            }
            path.moveTo(mDrawState.isoToRealXDownscaling(minRow, minCol) + mOriginX,
                    mDrawState.isoToRealYDownscaling(minRow, minCol) + mOriginY);//top
            path.lineTo(mDrawState.isoToRealXDownscaling(minRow, maxCol) + mOriginX,
                    mDrawState.isoToRealYDownscaling(minRow, maxCol) + mOriginY);//right
            path.lineTo(mDrawState.isoToRealXDownscaling(maxRow, maxCol) + mOriginX,
                    mDrawState.isoToRealYDownscaling(maxRow, maxCol) + mOriginY);//bottom
            path.lineTo(mDrawState.isoToRealXDownscaling(maxRow, minCol) + mOriginX,
                    mDrawState.isoToRealYDownscaling(maxRow, minCol) + mOriginY);//left
            path.lineTo(mDrawState.isoToRealXDownscaling(minRow, minCol) + mOriginX,
                    mDrawState.isoToRealYDownscaling(minRow, minCol) + mOriginY);//top
            canvas.drawPath(path, mSelectionPaint);
            canvas.drawPath(path, mSelectedTilePaint);
        }
    }

    @SuppressWarnings("unused")
    private void drawCenterLines(Canvas canvas) {
        // Draw a very thin plus sign spanning the entire screen that indicates the middle of the screen
        canvas.drawLine(mDrawState.mWidth / 2, 0, mDrawState.mWidth / 2, mDrawState.mHeight, mGridPaint);
        canvas.drawLine(0, mDrawState.mHeight / 2, mDrawState.mWidth, mDrawState.mHeight / 2, mGridPaint);
    }

    private boolean visibileTilesExist() {
        return mDrawState.isTileValid(mFirstRow, mFirstCol)//is the first tile to draw even valid/visible?
                && mDrawState.isTileVisible(mDrawState.isoToRealXDownscaling(mFirstRow, mFirstCol) + mOriginX,
                        mDrawState.isoToRealYDownscaling(mFirstRow, mFirstCol) + mOriginY);
    }

    private void setStartTime() {
        if (LOG_TTD) {
            startTime = System.currentTimeMillis();
        }
    }

    private void setEndTime() {
        if (LOG_TTD) {
            long endTime = System.currentTimeMillis();
            Log.v("TTD_" + TAG, "" + Math.round(PerfTools.CalcAverageTick((int) (endTime - startTime))));
        }
    }
}
