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
import com.jasperb.citybuilder.util.Constant.TERRAIN_MODS;
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

    private static Bitmap[] mFullBitmaps = null;
    private Bitmap[] mScaledBitmaps = new Bitmap[TERRAIN.count];
    private static Bitmap[] mModBitmaps;
    private static int[] mModOffsetX;
    private static int[] mModOffsetY;

    public TileBitmaps() {}

    public static int getModOffsetX(int mod) {
        return mModOffsetX[mod];
    }

    public static int getModOffsetY(int mod) {
        return mModOffsetY[mod];
    }

    /**
     * Create a TileBitmaps objects, loading the static bitmaps from the assets into memory.
     * 
     * @param context
     */
    public static void loadStaticBitmaps(Context context) {
        if (mFullBitmaps == null) {
            mFullBitmaps = new Bitmap[TERRAIN.count];
            mModBitmaps = new Bitmap[TERRAIN_MODS.count - 1];
            mModOffsetX = new int[TERRAIN_MODS.count - 1];
            mModOffsetY = new int[TERRAIN_MODS.count - 1];
            AssetManager assets = context.getAssets();
            try {
                InputStream ims = assets.open("TERRAIN/TileGrass.png");
                Bitmap tempBitmap = BitmapFactory.decodeStream(ims);
                mFullBitmaps[TERRAIN.GRASS] = tempBitmap.copy(Config.ARGB_8888, true);
                ims.close();

                ims = assets.open("TERRAIN/TileDirt.png");
                tempBitmap = BitmapFactory.decodeStream(ims);
                mFullBitmaps[TERRAIN.DIRT] = tempBitmap.copy(Config.ARGB_8888, true);
                ims.close();

//                ims = assets.open("TERRAIN/TileRounded.png");
//                tempBitmap = BitmapFactory.decodeStream(ims);
//                mFullBitmaps[TERRAIN.SIDEWALK] = tempBitmap.copy(Config.ARGB_8888, true);
//                ims.close();

                int[] roundedMods = { TERRAIN_MODS.ROUNDED_GRASS, TERRAIN_MODS.ROUNDED_DIRT };
                String[] roundedNames = { "Grass", "Dirt" };
                for (int j = 0; j < roundedNames.length; j++) {
                    for (int i = 0; i <= 3; i++) {
                        switch (i) {
                        case TERRAIN_MODS.TOP_LEFT:
                            ims = assets.open("TERRAIN_MODS/Corner" + roundedNames[j] + "Top.png");
                            mModOffsetX[roundedMods[j] + i] = 30;
                            mModOffsetY[roundedMods[j] + i] = 0;
                            break;
                        case TERRAIN_MODS.TOP_RIGHT:
                            ims = assets.open("TERRAIN_MODS/Corner" + roundedNames[j] + "Right.png");
                            mModOffsetX[roundedMods[j] + i] = 76;
                            mModOffsetY[roundedMods[j] + i] = 16;
                            break;
                        case TERRAIN_MODS.BOTTOM_LEFT:
                            ims = assets.open("TERRAIN_MODS/Corner" + roundedNames[j] + "Left.png");
                            mModOffsetX[roundedMods[j] + i] = 0;
                            mModOffsetY[roundedMods[j] + i] = 16;
                            break;
                        case TERRAIN_MODS.BOTTOM_RIGHT:
                            ims = assets.open("TERRAIN_MODS/Corner" + roundedNames[j] + "Bottom.png");
                            mModOffsetX[roundedMods[j] + i] = 26;
                            mModOffsetY[roundedMods[j] + i] = 37;
                            break;
                        }
                        tempBitmap = BitmapFactory.decodeStream(ims);
                        mModBitmaps[roundedMods[j] + i] = tempBitmap.copy(Config.ARGB_8888, true);
                        ims.close();
                    }
                }

                Log.d(TAG, "DONE LOADING");
            } catch (IOException ex) {
                Log.e(TAG, ex.toString());
            }
        }
    }

    /**
     * Free the memory used by the static bitmaps
     */
    public static void freeStaticBitmaps() {
        mFullBitmaps = null;
        mModBitmaps = null;
        mModOffsetX = null;
        mModOffsetY = null;
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
    public Bitmap getBaseBitmap(int terrain) {
        return mScaledBitmaps[terrain];
    }
    
    public Bitmap getModBitmap(int mod) {
        return mModBitmaps[mod];
    }

    public static Bitmap getFullBitmap(int terrain) {
        return mFullBitmaps[terrain];
    }
}
