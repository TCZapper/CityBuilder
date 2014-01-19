/**
 * 
 */
package com.jasperb.citybuilder.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.jasperb.citybuilder.CityModel;
import com.jasperb.citybuilder.util.Common;
import com.jasperb.citybuilder.util.Constant;
import com.jasperb.citybuilder.util.PerfTools;
import com.jasperb.citybuilder.util.TileBitmaps;

/**
 * Custom view for handling rendering the city model. Includes the ability to pan, fling and scale within the view.
 * 
 * @author Jasper
 * 
 */
public class CityView extends View {

    /**
     * Identifier string for debug messages originating from this class
     */
    public static final String TAG = "CityView";

    public static final boolean LOG_FPS = false;

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mDetector;
    private float mFocusRow = 0, mFocusCol = 0;
    private int mWidth = 0, mHeight = 0;
    private float mScaleFactor = Constant.MAXIMUM_SCALE_FACTOR;
    private Bitmap mBufferBitmap = null;
    private Canvas mBufferCanvas = null;
    private boolean mRedrawCity = true;
    private CityModel mCityModel = null;
    private boolean mDrawGridLines = true;
    private TileBitmaps mTileBitmaps = null;
    private Paint mGridPaint;
    private Paint mBitmapPaint;
    private Matrix mMatrix;

    public void setCityModel(CityModel model) {
        mCityModel = model;
    }

    public float getFocusRow() {
        return mFocusRow;
    }

    public float getFocusCol() {
        return mFocusCol;
    }

    public void setFocusCoords(float row, float col) {
        this.mFocusRow = row;
        this.mFocusCol = col;
        invalidateAll();
    }

    public float getScaleFactor() {
        return mScaleFactor;
    }

    public void setScaleFactor(float scaleFactor) {
        this.mScaleFactor = scaleFactor;
        invalidateAll();
    }

    public void invalidateAll() {
        Log.d(TAG, "INVALIDATE ALL");
        mRedrawCity = true;
        invalidate();
    }

    /**
     * Class constructor taking a context and an attribute set. This constructor is used by the layout engine to construct a
     * {@link CityView} from a set of XML attributes.
     * 
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs
     *            An attribute set which can contain attributes inherited from {@link android.view.View}.
     */
    public CityView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Class constructor taking a context. This constructor is used by the layout engine to construct a {@link CityView}.
     * 
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     */
    public CityView(Context context) {
        super(context);
        init();
    }

    /*
     * @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
     * // Do nothing. Do not call the superclass method--that would start a layout pass
     * // on this view's children.
     * }
     */
    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "ON DRAW");

        canvas.drawColor(Color.BLACK);// Blank the canvas

        if (mBufferCanvas == null || mCityModel == null)
            return;
        if (mRedrawCity) {
            if (LOG_FPS) {
                Log.d(TAG + "FPS", "" + 1000 / PerfTools.CalcAverageTick(System.currentTimeMillis()));
            }
            drawGround();
        }
        // Push everything onto screen
        canvas.drawBitmap(mBufferBitmap, 0, 0, null);

        mRedrawCity = false;
        if (LOG_FPS)
            invalidateAll();
        super.onDraw(canvas);
    }

    private void drawGround() {
        Log.d(TAG, "DRAW GROUND");

        mBufferCanvas.drawColor(Color.BLACK);// Clear the canvas

        // Calculate real coordinates for the top-most tile based off the focus iso coordinates being the centre of the screen
        float realTopX = mWidth / 2 - (Common.isoToRealX(mFocusRow, mFocusCol) * mScaleFactor);
        float realTopY = mHeight / 2 - (Common.isoToRealY(mFocusRow, mFocusCol) * mScaleFactor);

        // Shift the bitmap left to horizontally center the tile around 0,0 and subtract 1 because the image isn't drawn perfectly centered
        float bitmapOffsetX = (-Constant.TILE_WIDTH / 2 - 1) * mScaleFactor;

        //Draw all tiles for testing
//        for (int col = 0; col < mCityModel.getWidth(); col++) {
//            for (int row = 0; row < mCityModel.getHeight(); row++) {
//                // Log.d(TAG,"Paint Tile: " + row + " : " + col);
//                mMatrix.setScale(mScaleFactor, mScaleFactor);
//                mMatrix.postTranslate(Common.isoToRealX(row, col) * mScaleFactor + realTopX + bitmapOffsetX,
//                        Common.isoToRealY(row, col) * mScaleFactor + realTopY);
//                mBufferCanvas.drawBitmap(mTileBitmaps.getBitmap(TERRAIN.DIRT), mMatrix, mBitmapPaint);
//            }
//        }

        // Calculate the tile that is at the top left corner of the view, and the one at the bottom right corner
        int topLeftRow = (int) Math.floor(Common.realToIsoRow(-realTopX / mScaleFactor, -realTopY / mScaleFactor));
        int topLeftCol = (int) Math.floor(Common.realToIsoCol(-realTopX / mScaleFactor, -realTopY / mScaleFactor));
        int bottomRightRow = (int) Math.floor(Common.realToIsoRow((-realTopX + mWidth) / mScaleFactor, (-realTopY + mHeight) / mScaleFactor));
        int bottomRightCol = (int) Math.floor(Common.realToIsoCol((-realTopX + mWidth) / mScaleFactor, (-realTopY + mHeight) / mScaleFactor));
        Log.d(TAG, "TL TILE: " + topLeftRow + " : " + topLeftCol);
        Log.d(TAG, "BR TILE: " + bottomRightRow + " : " + bottomRightCol);

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
        if (Common.isoToRealX(topLeftRow, topLeftCol) * mScaleFactor + realTopX > 0) {
            leftBoundRow++;
        }

        int rightBoundRow = bottomRightRow;
        int rightBoundCol = bottomRightCol;
        if (Common.isoToRealX(bottomRightRow, bottomRightCol) * mScaleFactor + realTopX < mWidth) {
            rightBoundRow--;
        }

        int topBoundRow = topLeftRow;
        int topBoundCol = topLeftCol;
        if ((Common.isoToRealY(topLeftRow, topLeftCol) + (Constant.TILE_HEIGHT / 2)) * mScaleFactor + realTopY > 0) {
            topBoundRow--;
        }

        int bottomBoundRow = bottomRightRow;
        int bottomBoundCol = bottomRightCol;
        if ((Common.isoToRealY(bottomRightRow, bottomRightCol) + (Constant.TILE_HEIGHT / 2)) * mScaleFactor + realTopY < mHeight) {
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
        } else if (topBoundRow >= mCityModel.getHeight()) {
            firstCol = Math.max(0, topBoundCol + (topBoundRow - (mCityModel.getHeight() - 1)));
        } else {
            firstCol = Math.max(0, topLeftCol);
        }

        int firstRow = Math.max(0, Math.max(topBoundRow - (firstCol - topBoundCol),//where does firstCol cross the bottom edge
                rightBoundRow - (rightBoundCol - firstCol)));//where does firstCol cross the right edge
        Log.d(TAG, "DRAW: " + firstRow + " : " + firstCol);
        if (Common.isTileValid(mCityModel, firstRow, firstCol)//is the first tile to draw even valid/visible?
                && Common.isTileVisible(mScaleFactor, mWidth, mHeight,
                        Common.isoToRealX(firstRow, firstCol) * mScaleFactor + realTopX,
                        Common.isoToRealY(firstRow, firstCol) * mScaleFactor + realTopY)) {
            int lastCol;// Calculate last column to draw by using same logic as first column (except bottom/right boundaries)
            if (bottomRightRow < 0) {
                lastCol = Math.min(mCityModel.getWidth() - 1, bottomBoundCol + bottomBoundRow);
            } else if (bottomRightRow >= mCityModel.getHeight()) {
                lastCol = Math.min(mCityModel.getWidth() - 1, rightBoundCol - (rightBoundRow - (mCityModel.getHeight() - 1)));
            } else {
                lastCol = Math.min(mCityModel.getWidth() - 1, bottomRightCol);
            }

            for (int col = firstCol; col <= lastCol; col++) {
                // Find the last row by figuring out the rows where this column crosses the bottom and left edge,
                // and draw up to the whichever row corresponds edge we hit first
                int lastRow = Math.min(mCityModel.getHeight() - 1,
                        Math.min(leftBoundRow + (col - leftBoundCol), bottomBoundRow + (bottomBoundCol - col)));
                // Find the first row by checking against where it collides with the right and top edges, same as with the last row
                int row = Math.max(0, Math.max(topBoundRow - (col - topBoundCol), rightBoundRow - (rightBoundCol - col)));
                for (; row <= lastRow; row++) {
                    // Time to draw the terrain to the buffer. Technically, scaling for every tile is not ideal, but scaling at the moment
                    // we draw onto the buffer canvas has two benefits:
                    // 1. No memory allocations needed when the scale factor changes
                    // 2. It does not have any issues lining up tiles, unlike all other methods tried
//                    Log.d(TAG, "Paint Tile: " + row + " : " + col);
                    mMatrix.setScale(mScaleFactor, mScaleFactor);
                    mMatrix.postTranslate(Common.isoToRealX(row, col) * mScaleFactor + realTopX + bitmapOffsetX,
                            Common.isoToRealY(row, col) * mScaleFactor + realTopY);
                    mBufferCanvas.drawBitmap(mTileBitmaps.getBitmap(mCityModel.getTerrain(row, col)), mMatrix, mBitmapPaint);
                }
            }
        } else {
            Log.d(TAG, "NOTHING TO DRAW");
        }

        // Draw grid lines
        if (mDrawGridLines) {
            for (int col = 0; col <= mCityModel.getWidth(); col++) {
                mBufferCanvas.drawLine(Common.isoToRealX(0, col) * mScaleFactor + realTopX,
                        Common.isoToRealY(0, col) * mScaleFactor + realTopY,
                        Common.isoToRealX(mCityModel.getHeight(), col) * mScaleFactor + realTopX,
                        Common.isoToRealY(mCityModel.getHeight(), col) * mScaleFactor + realTopY, mGridPaint);
            }
            for (int row = 0; row <= mCityModel.getHeight(); row++) {
                mBufferCanvas.drawLine(Common.isoToRealX(row, 0) * mScaleFactor + realTopX,
                        Common.isoToRealY(row, 0) * mScaleFactor + realTopY,
                        Common.isoToRealX(row, mCityModel.getWidth()) * mScaleFactor + realTopX,
                        Common.isoToRealY(row, mCityModel.getWidth()) * mScaleFactor + realTopY, mGridPaint);
            }
        }

        // Draw a very thin + spanning the entire screen that indicates the middle of the screen
//         mBufferCanvas.drawLine(mWidth / 2, 0, mWidth / 2, mHeight, mGridPaint);
//         mBufferCanvas.drawLine(0, mHeight / 2, mWidth, mHeight / 2, mGridPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(TAG, "ON SIZE CHANGED");
        super.onSizeChanged(w, h, oldw, oldh);

        // Account for padding
        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ypad = (float) (getPaddingTop() + getPaddingBottom());

        mWidth = (int) Math.ceil((float) w - xpad);
        mHeight = (int) Math.ceil((float) h - ypad);

        // Debug.startMethodTracing();

        initCanvas();
        invalidateAll();
    }

    /**
     * Initialize and allocate the necessary components of the view, except those that depend on the view size or city size
     */
    private void init() {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        mDetector = new GestureDetector(getContext(), new GestureListener());

        // Turn off long press--this control doesn't use it, and if long press is enabled, you can't scroll for a bit, pause, then scroll
        // some more (the pause is interpreted as a long press, apparently)
        mDetector.setIsLongpressEnabled(false);

        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

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

    /**
     * Initialize and allocate the necessary components of the view that relate to the size of the canvas
     */
    private void initCanvas() {
        // Recycle the old bitmap first, hopefully preventing out of memory issues when we create its replacement
        if (mBufferBitmap != null)
            mBufferBitmap.recycle();

        mBufferBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mBufferBitmap.setHasAlpha(false);
        mBufferCanvas = new Canvas(mBufferBitmap);
        Log.d(TAG, "MEMORY: " + mBufferBitmap.getByteCount());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Let the GestureDetectors interpret this event
        boolean result = mDetector.onTouchEvent(event);
        mScaleDetector.onTouchEvent(event);

        // If the GestureDetectors don't want this event, do some custom processing.
        // This code just tries to detect when the user is done scrolling by looking for ACTION_UP events.
//        if (!result) {
//            if (event.getAction() == MotionEvent.ACTION_UP) {
//                // stopScrolling();
//                result = true;
//            }
//        }
        invalidateAll();
        return result;
    }

    /**
     * The gesture listener, used for handling scroll and fling gestures (for panning)
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {// For some reason, scaling and scroll don't work without this
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mFocusRow += Common.realToIsoRow(distanceX, distanceY) / mScaleFactor;
            mFocusCol += Common.realToIsoCol(distanceX, distanceY) / mScaleFactor;
            
            invalidateAll();
            return true;
        }
    }

    /**
     * The scale listener, used for handling multi-finger scale gestures.
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            float scaleFactor = mScaleFactor * scaleGestureDetector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(Constant.MINIMUM_SCALE_FACTOR, Math.min(scaleFactor, Constant.MAXIMUM_SCALE_FACTOR));
            
            invalidateAll();
            return true;
        }

    };

}
