/**
 * 
 */
package com.jasperb.citybuilder.util;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.jasperb.citybuilder.Constant;
import com.jasperb.citybuilder.Constant.OBJECTS;
import com.jasperb.citybuilder.SharedState;

/**
 * @author Jasper
 * 
 */
public class ObjectBitmaps {
    /**
     * String used for identifying this class.
     */
    public static final String TAG = "ObjectBitmaps";

    private static Bitmap[][] mFullObjectBitmaps = null;
    private static Bitmap[][] mScaledObjectBitmaps = new Bitmap[OBJECTS.count][];

    private Canvas mCanvas = new Canvas();
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Matrix mMatrix = new Matrix();

    public ObjectBitmaps() {
        for (int i = 0; i < OBJECTS.count; i++) {
            try {
                mScaledObjectBitmaps[i] = new Bitmap[mFullObjectBitmaps[i].length];
                for (int j = 0; j < mFullObjectBitmaps[i].length; j++) {
                    mScaledObjectBitmaps[i][j] = Bitmap.createScaledBitmap(mFullObjectBitmaps[i][j], mFullObjectBitmaps[i][j].getWidth(),
                            mFullObjectBitmaps[i][j].getHeight(), false);
                }
            } catch (NullPointerException e) {//Usually the result of a missing file in the assets folder
                Log.d(TAG, "Failed to scale for object: " + i);
                mFullObjectBitmaps[i] = mFullObjectBitmaps[0];
                i--;
            }
        }
    }

    public static Bitmap[][] getFullObjectBitmaps() {
        return mFullObjectBitmaps;
    }

    /**
     * Create a TileBitmaps objects, loading the static bitmaps from the assets into memory.
     * 
     * @param context
     */
    public static void loadStaticBitmaps(Context context) {
        if (getFullObjectBitmaps() == null) {
            mFullObjectBitmaps = new Bitmap[OBJECTS.count][];
            AssetManager assets = context.getAssets();
            InputStream ims = null;
            Bitmap tempBitmap;
            try {
                Canvas canvas = new Canvas();
                int sliceWidth, sliceCount, height, width;
                Rect trimmedRect = new Rect(), bitmapRect = new Rect();
                for (int i = 0; i < OBJECTS.count; i++) {
                    ims = assets.open("BUILDINGS/" + OBJECTS.getName(i) + ".png");
                    tempBitmap = BitmapFactory.decodeStream(ims);
                    height = tempBitmap.getHeight();
                    width = tempBitmap.getWidth();
                    sliceWidth = OBJECTS.getSliceWidth(i);
                    sliceCount = OBJECTS.getSliceCount(i);
                    getFullObjectBitmaps()[i] = new Bitmap[sliceCount];
                    //Log.d(TAG, "SLICES: " + sliceCount + " :: " + sliceWidth);

                    //Slice the loaded bitmap into multiple bitmaps
                    //Each slice is sized to allow drawing so that slices can be drawn in the order of the tile they lie one
                    for (int j = 0; j < sliceCount; j++) {
                        if (j == 0) {
                            trimmedRect.set(0, 0, sliceWidth, height);
                        } else if (j == sliceCount - 1) {
                            trimmedRect.set(sliceWidth * j, 0, width, height);
                        } else {
                            trimmedRect.set(sliceWidth * j, 0, sliceWidth * (j + 1), height);
                        }
                        bitmapRect.set(0, 0, trimmedRect.width(), trimmedRect.height());
                        getFullObjectBitmaps()[i][j] = Bitmap.createBitmap(trimmedRect.width(), height, Config.ARGB_8888);
                        canvas.setBitmap(getFullObjectBitmaps()[i][j]);
                        canvas.drawBitmap(tempBitmap, trimmedRect, bitmapRect, null);
                    }
                    ims.close();
                }
                Log.d(TAG, "DONE LOADING");
            } catch (IOException ex) {
                Log.e(TAG, ex.toString());
                if (ims != null) {
                    try {
                        ims.close();
                    } catch (IOException e) {}
                }
            }
        }
    }

    /**
     * Free the memory used by the static bitmaps
     */
    public static void freeStaticBitmaps() {
        mFullObjectBitmaps = null;
    }

    /**
     * Recreate the bitmaps that getBitmap returns based off the state.
     * That includes scaling based off the scale factor.
     * 
     * @param state
     *            the state that dictates the properties of the object
     */
    public void remakeBitmaps(SharedState state) {
        // A major part of the bitmap draw cost appears to be related to the size of the bitmap (in memory).
        // As drawing tile bitmaps is a significant chunk of the total draw time, we try to minimize the bitmap size.
        // This require recreating the bitmap as it is scaled, which unfortunately means we do memory allocations on the draw thread.
        // Tile mods take a much smaller chunk of our total draw time, so we just redraw the tile mods into their existing bitmap.

        float visualScale = state.getTileWidth() / (float) Constant.TILE_WIDTH;
        mMatrix.setScale(visualScale, visualScale);

        for (int i = 0; i < mFullObjectBitmaps.length; i++) {
            for (int j = 0; j < mFullObjectBitmaps[i].length; j++) {
                mScaledObjectBitmaps[i][j].eraseColor(android.graphics.Color.TRANSPARENT);
                mCanvas.setBitmap(mScaledObjectBitmaps[i][j]);
                mCanvas.drawBitmap(mFullObjectBitmaps[i][j], mMatrix, mPaint);
            }
        }

    }

    /**
     * Fetch the saved bitmap for a specific object type. The bitmap should have already been modified for use by calling remakeBitmaps.
     * 
     * @param object
     *            the type of object to fetch
     * @param slice
     *            the slice to fetch
     */
    public Bitmap getScaledObjectBitmap(int object, int slice) {
        return mScaledObjectBitmaps[object][slice];
    }
}
