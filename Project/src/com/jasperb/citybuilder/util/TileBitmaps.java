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
import android.graphics.Paint;
import android.util.Log;

import com.jasperb.citybuilder.util.Constant.TERRAIN;
import com.jasperb.citybuilder.view.CityViewState;

/**
 * @author Jasper
 * 
 */
public class TileBitmaps {
    /**
     * String used for identifying this class.
     */
    public static final String TAG = "TileBitmaps";

    private final Bitmap[] mFullBitmaps = new Bitmap[Constant.TERRAIN.values().length];
    private Bitmap[] mScaledBitmaps = new Bitmap[Constant.TERRAIN.values().length];

    /**
     * Create a TileBitmaps objects, loading the full-sized bitmaps from the assets into memory.
     * 
     * @param context
     */
    public TileBitmaps(Context context) {
        AssetManager assets = context.getAssets();
        try {
            InputStream ims = assets.open("TERRAIN/TileGrass.png");
            Bitmap tempBitmap = BitmapFactory.decodeStream(ims);
            mFullBitmaps[TERRAIN.GRASS.ordinal()] = tempBitmap.copy(Config.ARGB_8888, true);
            ims.close();

            ims = assets.open("TERRAIN/TileDirt.png");
            tempBitmap = BitmapFactory.decodeStream(ims);
            mFullBitmaps[TERRAIN.DIRT.ordinal()] = tempBitmap.copy(Config.ARGB_8888, true);
            ims.close();

            Log.d(TAG, "DONE LOADING");
        } catch (IOException ex) {
            Log.e(TAG, ex.toString());
        }
    }

    /**
     * Recreate the bitmaps that getBitmap returns based off the state.
     * That includes drawing gridlines and scaling based off the scale factor.
     * 
     * @param state
     *            the state that dictates the properties of the tiles
     */
    public void remakeBitmaps(CityViewState state) {
        Log.v(TAG, "REMAKE BITMAPS");
        if (state.mDrawGridLines) {
            Canvas canvas = new Canvas();
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.STROKE);
            paint.setARGB(255, 225, 225, 225);
            paint.setStrokeWidth(0);
            for (int i = 0; i < mFullBitmaps.length; i++) {
                mScaledBitmaps[i] = Bitmap.createScaledBitmap(mFullBitmaps[i], state.getTileWidth(), state.getTileHeight(), true);
                canvas.setBitmap(mScaledBitmaps[i]);
                canvas.drawLine(state.getTileWidth() / 2, 0, state.getTileWidth(), state.getTileHeight() / 2, paint);
                canvas.drawLine(state.getTileWidth() / 2, 0, 0, state.getTileHeight() / 2, paint);
            }
        } else {
            for (int i = 0; i < mFullBitmaps.length; i++) {
                mScaledBitmaps[i] = Bitmap.createScaledBitmap(mFullBitmaps[i], state.getTileWidth(), state.getTileHeight(), true);
            }
        }
    }

    /**
     * Fetch the saved bitmap for a specific terrain type. The bitmap should have already been modified for use by calling remakeBitmaps.
     * 
     * @param terrain
     *            the type of terrain to fetch
     */
    public Bitmap getBitmap(TERRAIN terrain) {
        return mScaledBitmaps[terrain.ordinal()];
    }
}
