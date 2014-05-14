/**
 * 
 */
package com.jasperb.citybuilder.util;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Log;

import com.jasperb.citybuilder.SharedState;
import com.jasperb.citybuilder.Constant.TERRAIN;

/**
 * @author Jasper
 * 
 */
public class TileBitmapSheets {
    /**
     * String used for identifying this class.
     */
    public static final String TAG = "TileBitmapSheets";

    private static Bitmap mFullBitmap = null;
    private Bitmap mScaledBitmap;
    private Canvas mCanvas;
    private Paint mPaint, mBitmapPaint;
    private Rect mRect;

    public TileBitmapSheets() {
        mCanvas = new Canvas();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setARGB(255, 225, 225, 225);
        mPaint.setStrokeWidth(0);
        mBitmapPaint = new Paint();
        mBitmapPaint.setXfermode(new PorterDuffXfermode(Mode.SRC));
        mScaledBitmap = Bitmap.createBitmap(mFullBitmap.getWidth(), mFullBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(mScaledBitmap);

        mRect = new Rect();
    }

    /**
     * Create a TileBitmaps objects, loading the full-sized bitmaps from the assets into memory.
     * 
     * @param context
     */
    public static void loadFullBitmaps(Context context) {
        if (mFullBitmap == null) {
            AssetManager assets = context.getAssets();
            try {
                InputStream ims = assets.open("TerrainSheet.png");
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                mFullBitmap = BitmapFactory.decodeStream(ims, null, options);
                ims.close();

                Log.d(TAG, "DONE LOADING");
            } catch (IOException ex) {
                Log.e(TAG, ex.toString());
            }
        }
    }

    /**
     * Free the memory used by the full bitmaps
     */
    public static void freeFullBitmaps() {
        mFullBitmap = null;
    }

    /**
     * Recreate the bitmaps that getBitmap returns based off the state.
     * That includes drawing gridlines and scaling based off the scale factor.
     * 
     * @param state
     *            the state that dictates the properties of the tiles
     */
    public void remakeBitmaps(SharedState state) {
        Log.v(TAG, "REMAKE BITMAPS");

        mRect.set(0, 0, state.getTileWidth() * TERRAIN.count, state.getTileHeight());
        //mCanvas.
        mCanvas.drawBitmap(mFullBitmap, null, mRect, mBitmapPaint);
        if (state.UIS_DrawGridLines) {
            for (int i = 0; i < TERRAIN.count; i++) {
                mCanvas.drawLine(i * state.getTileWidth() + state.getTileWidth() / 2, 0,
                        i * state.getTileWidth() + state.getTileWidth(), state.getTileHeight() / 2, mPaint);
                mCanvas.drawLine(i * state.getTileWidth() + state.getTileWidth() / 2, 0,
                        i * state.getTileWidth(), state.getTileHeight() / 2, mPaint);
            }
        }
    }

    /**
     * Fetch the saved bitmap for a specific terrain type. The bitmap should have already been modified for use by calling remakeBitmaps.
     * 
     * @param terrain
     *            the type of terrain to fetch
     */
    public Bitmap getScaledBitmap() {
        return mScaledBitmap;
    }

    public static Bitmap getFullBitmap() {
        return mFullBitmap;
    }
}
