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
    private float mFocusRow = 2, mFocusCol = -10;
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
        if (LOG_FPS) {
            Log.d(TAG + "FPS", "" + 1000 / PerfTools.CalcAverageTick(System.currentTimeMillis()));
        }

        mBufferCanvas.drawColor(Color.BLACK);// Clear the canvas

        Log.d(TAG, "FOCUS TILE: " + mFocusRow + "x" + mFocusCol);

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

        int topLeftRow = (int) Math.floor(Common.realToIsoRow(-realTopX / mScaleFactor, -realTopY / mScaleFactor));
        int topLeftCol = (int) Math.floor(Common.realToIsoCol(-realTopX / mScaleFactor, -realTopY / mScaleFactor));
        int topRightRow = (int) Math.floor(Common.realToIsoRow((-realTopX + mWidth) / mScaleFactor, (-realTopY) / mScaleFactor));
        int topRightCol = (int) Math.floor(Common.realToIsoCol((-realTopX + mWidth) / mScaleFactor, (-realTopY) / mScaleFactor));
        int bottomRightRow = (int) Math.floor(Common.realToIsoRow((-realTopX + mWidth) / mScaleFactor, (-realTopY + mHeight) / mScaleFactor));
        int bottomRightCol = (int) Math.floor(Common.realToIsoCol((-realTopX + mWidth) / mScaleFactor, (-realTopY + mHeight) / mScaleFactor));
        Log.d(TAG, "ORIGIN TILE: " + topLeftRow + "x" + topLeftCol + " --- "
                + Common.realToIsoRow(-realTopX, -realTopY) + "x" + Common.realToIsoCol(-realTopX, -realTopY));
        Log.d(TAG,
                "BOTTOM TILE: " + bottomRightRow + "x" + bottomRightCol + " - "
                        + Common.realToIsoRow((-realTopX + mWidth) / mScaleFactor, (-realTopY + mHeight) / mScaleFactor));

        // Determine the first (the top-most visible) tile to draw by the following method:
        // 1. Is there a tile in the top left corner of the view? If so draw that first
        // 2. Is the top-most tile (row=0, col=0) visible? If so draw that first
        // 3. Is the top-most tile left of the left edge of the view?
        //      If so we must locate the top-most, valid tile that shares the same x coordinate as the top left corner tile
        //      If this tile is valid and visible within the view, then draw that first
        // 4. If the top-most tile is above the top edge of the view?
        //      If so we must locate the top-most, valid tile that shares the same x coordinate as the top-most tile
        //      If this tile is valid and visible within the view, then draw that first
        // 5. Locate the top-most tile that shares the same x coordinate as the top right corner tile
        //      If this tile is valid and visible within the view, then draw that first
        // 6. Otherwise there are no visible tiles
        int firstRow = 0, firstCol = 0;
        boolean cityVisible = false;
        do {// Do while loop only so we can break from it when we've determined the first tile to draw
            if (Common.isTileValid(mCityModel, topLeftRow, topLeftCol)) {//This is the most likely scenario, so we check for it first
                firstRow = topLeftRow;
                firstCol = topLeftCol;
                cityVisible = true;
                break;
            }
            if (Common.isTileVisible(mScaleFactor, mWidth, mHeight, realTopX, realTopY)) {
                firstRow = 0;
                firstCol = 0;
                cityVisible = true;
                break;
            }
            if (realTopX < 0 && topLeftRow < 0) {// if topLeftRow >= 0 we know that there can't be a valid tile on the left edge of the view
                firstRow = 0;
                firstCol = topLeftCol - topLeftRow;
                if (Common.isTileValid(mCityModel, firstRow, firstCol)
                        && Common.isTileVisible(mScaleFactor, mWidth, mHeight,
                                Common.isoToRealX(firstRow, firstCol) * mScaleFactor + realTopX,
                                Common.isoToRealY(firstRow, firstCol) * mScaleFactor + realTopY)) {
                    cityVisible = true;
                    break;
                }
            }
            if (realTopY < 0) {
                if (topLeftCol >= 0) {
                    firstRow = mCityModel.getHeight() - 1;
                    firstCol = topLeftCol + (topLeftRow - firstRow);
                } else {
                    firstRow = topLeftRow + topLeftCol;
                    firstCol = 0;
                }
                if (Common.isTileValid(mCityModel, firstRow, firstCol)
                        && Common.isTileVisible(mScaleFactor, mWidth, mHeight,
                                Common.isoToRealX(firstRow, firstCol) * mScaleFactor + realTopX,
                                Common.isoToRealY(firstRow, firstCol) * mScaleFactor + realTopY)) {
                    cityVisible = true;
                    break;
                }
            }
            if (topRightCol < 0) {// if topRightCol >= 0 we know that there can't be a valid tile on the right edge of the view
                firstRow = topRightRow - topRightCol;
                firstCol = 0;
                if (Common.isTileValid(mCityModel, firstRow, firstCol)
                        && Common.isTileVisible(mScaleFactor, mWidth, mHeight,
                                Common.isoToRealX(firstRow, firstCol) * mScaleFactor + realTopX,
                                Common.isoToRealY(firstRow, firstCol) * mScaleFactor + realTopY)) {
                    cityVisible = true;
                    break;
                }
            }
            // 6. No visible tiles if this is reached
        } while (false);

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

        int bottomBoundRow = bottomRightRow;
        int bottomBoundCol = bottomRightCol;
        if ((Common.isoToRealY(bottomRightRow, bottomRightCol) + (Constant.TILE_HEIGHT / 2)) * mScaleFactor + realTopY < mHeight) {
            Log.d(TAG, "BOTTOM SHIFT");
            bottomBoundRow++;
        }

        Log.d(TAG, "DRAW: " + cityVisible + " - " + firstRow + " : " + firstCol);
        if (cityVisible) {
            int lastCol;
            if (bottomRightRow < 0) {
                // The last column to draw is same as that of the row=0 tile that sits on the bottom edge
                lastCol = Math.min(mCityModel.getWidth() - 1, bottomRightCol + bottomRightRow);
            } else if (bottomRightRow >= mCityModel.getHeight()) {
                // The last column is the same as that of the row=mCityModel.getHeight()-1 tile that sits on the right edge
                lastCol = Math.min(mCityModel.getWidth() - 1, bottomRightCol - (bottomRightRow - (mCityModel.getHeight() - 1)));
            } else {// The bottom right tile is last column
                lastCol = Math.min(mCityModel.getWidth() - 1, bottomRightCol);
            }

            for (int col = firstCol; col <= lastCol; col++) {
                // Find the last row by figuring out the rows where this column crosses the bottom and left edge,
                // and draw up to the whichever row corresponds edge we hit first
                int lastRow = Math.min(mCityModel.getHeight() - 1,
                        Math.min(leftBoundRow + (col - leftBoundCol), bottomBoundRow + (bottomBoundCol - col)));
                // Find the first row by checking against where it collides with the right and top edges
                // The top edge is a special case as we can get it by adding one row for every column we've already drawn
                int row = Math.max(0, Math.max(firstRow - (col - firstCol), rightBoundRow - (rightBoundCol - col)));
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

        mDetector = new GestureDetector(this.getContext(), new GestureListener());

        // Turn off long press--this control doesn't use it, and if long press is enabled, you can't scroll for a bit, pause, then scroll
        // some more (the pause is interpreted as a long press, apparently)
        mDetector.setIsLongpressEnabled(false);

        mScaleDetector = new ScaleGestureDetector(this.getContext(), new ScaleListener());

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
        mBufferCanvas = new Canvas(mBufferBitmap);
        Log.d(TAG, "MEMORY: " + mBufferBitmap.getByteCount());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Currently we do not properly try to handle these events, rather we use them as to aid debugging by forcing a complete redraw

        // Let the GestureDetectors interpret this event
        boolean result = mDetector.onTouchEvent(event);
        result = mScaleDetector.onTouchEvent(event);

        // If the GestureDetector doesn't want this event, do some custom processing.
        // This code just tries to detect when the user is done scrolling by looking
        // for ACTION_UP events.
        if (!result) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // User is done scrolling, it's now safe to do things like autocenter
                // stopScrolling();
                result = true;
            }
        }

        return result;
    }

    /**
     * Extends {@link GestureDetector.SimpleOnGestureListener} to provide custom gesture processing.
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            // Debug.stopMethodTracing();
            invalidateAll();// debug force redraw
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }
    }

    /**
     * The scale listener, used for handling multi-finger scale gestures.
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        /**
         * This is the active focal point in terms of the viewport. Could be a local variable but kept here to minimize per-frame
         * allocations.
         */
        private PointF viewportFocus = new PointF();
        private float lastSpanX;
        private float lastSpanY;

        // Detects that new pointers are going down.
        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            lastSpanX = scaleGestureDetector.getCurrentSpanX();
            lastSpanY = scaleGestureDetector.getCurrentSpanY();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {

            float spanX = scaleGestureDetector.getCurrentSpanX();
            float spanY = scaleGestureDetector.getCurrentSpanY();

            float scaleFactor = mScaleFactor * scaleGestureDetector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));

            float focusX = scaleGestureDetector.getFocusX();
            float focusY = scaleGestureDetector.getFocusY();
            // Makes sure that the chart point is within the chart region.
            // See the sample for the implementation of hitTest().

            // constrainViewport();

            lastSpanX = spanX;
            lastSpanY = spanY;
            return true;
        }

    };

}
