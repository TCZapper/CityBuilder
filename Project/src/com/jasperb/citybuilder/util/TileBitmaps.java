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

    private static Bitmap[] mFullTileBitmaps = null;
    private Bitmap[] mScaledTileBitmaps = new Bitmap[TERRAIN.count];
    private static Bitmap[] mFullModBitmaps;
    private Bitmap[] mScaledModBitmaps = new Bitmap[TERRAIN_MODS.count];
    private static int[] mModOffsetX;
    private static int[] mModOffsetY;
    private Canvas mCanvas = new Canvas();
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Matrix mMatrix = new Matrix();

    public TileBitmaps() {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setARGB(255, 225, 225, 225);
        mPaint.setStrokeWidth(0);
        for (int i = 0; i < TERRAIN_MODS.count; i++) {
            try {
                mScaledModBitmaps[i] = Bitmap.createScaledBitmap(mFullModBitmaps[i], mFullModBitmaps[i].getWidth(),
                        mFullModBitmaps[i].getHeight(), false);
            } catch (NullPointerException e) {//Temp fix until all mods are implemented
                mFullModBitmaps[i] = mFullModBitmaps[0];
                mScaledModBitmaps[i] = Bitmap.createScaledBitmap(mFullModBitmaps[i], mFullModBitmaps[i].getWidth(),
                        mFullModBitmaps[i].getHeight(), false);
            }
        }
    }

    /**
     * @param mod the terrain mod to get the offset for
     * @return the X offset of the mod relative to the top-right corner of the tile
     */
    public static int getModOffsetX(int mod) {
        return mModOffsetX[mod];
    }

    /**
     * @param mod the terrain mod to get the offset for
     * @return the Y offset of the mod relative to the top-right corner of the tile
     */
    public static int getModOffsetY(int mod) {
        return mModOffsetY[mod];
    }

    /**
     * Create a TileBitmaps objects, loading the static bitmaps from the assets into memory.
     * 
     * @param context
     */
    public static void loadStaticBitmaps(Context context) {
        if (mFullTileBitmaps == null) {
            mFullTileBitmaps = new Bitmap[TERRAIN.count];
            mFullModBitmaps = new Bitmap[TERRAIN_MODS.count];
            mModOffsetX = new int[TERRAIN_MODS.count];
            mModOffsetY = new int[TERRAIN_MODS.count];
            AssetManager assets = context.getAssets();
            try {
                InputStream ims = assets.open("TERRAIN/TileGrass.png");
                Bitmap tempBitmap = BitmapFactory.decodeStream(ims);
                mFullTileBitmaps[TERRAIN.GRASS] = tempBitmap.copy(Config.ARGB_8888, true);
                ims.close();

                ims = assets.open("TERRAIN/TileDirt.png");
                tempBitmap = BitmapFactory.decodeStream(ims);
                mFullTileBitmaps[TERRAIN.DIRT] = tempBitmap.copy(Config.ARGB_8888, true);
                ims.close();
                
                ims = assets.open("TERRAIN/TileSidewalk.png");
                tempBitmap = BitmapFactory.decodeStream(ims);
                mFullTileBitmaps[TERRAIN.SIDEWALK] = tempBitmap.copy(Config.ARGB_8888, true);
                ims.close();
                
                ims = assets.open("TERRAIN/TilePavement.png");
                tempBitmap = BitmapFactory.decodeStream(ims);
                mFullTileBitmaps[TERRAIN.PAVEMENT] = tempBitmap.copy(Config.ARGB_8888, true);
                ims.close();

                int[] roundedMods = { TERRAIN_MODS.ROUNDED_GRASS, TERRAIN_MODS.ROUNDED_DIRT, TERRAIN_MODS.ROUNDED_PAVEMENT };
                String[] roundedNames = { "Grass", "Dirt", "Pavement" };
                for (int j = 0; j < roundedNames.length; j++) {
                    for (int i = 0; i <= 3; i++) {
                        switch (i) {
                        case TERRAIN_MODS.TOP_LEFT:
                            ims = assets.open("TERRAIN_MODS/Rounded" + roundedNames[j] + "Top.png");
                            mModOffsetX[roundedMods[j] + i] = 27;
                            mModOffsetY[roundedMods[j] + i] = 0;
                            break;
                        case TERRAIN_MODS.TOP_RIGHT:
                            ims = assets.open("TERRAIN_MODS/Rounded" + roundedNames[j] + "Right.png");
                            mModOffsetX[roundedMods[j] + i] = 76;
                            mModOffsetY[roundedMods[j] + i] = 15;
                            break;
                        case TERRAIN_MODS.BOTTOM_LEFT:
                            ims = assets.open("TERRAIN_MODS/Rounded" + roundedNames[j] + "Left.png");
                            mModOffsetX[roundedMods[j] + i] = 0;
                            mModOffsetY[roundedMods[j] + i] = 15;
                            break;
                        case TERRAIN_MODS.BOTTOM_RIGHT:
                            ims = assets.open("TERRAIN_MODS/Rounded" + roundedNames[j] + "Bottom.png");
                            mModOffsetX[roundedMods[j] + i] = 26;
                            mModOffsetY[roundedMods[j] + i] = 38;
                            break;
                        }
                        tempBitmap = BitmapFactory.decodeStream(ims);
                        mFullModBitmaps[roundedMods[j] + i] = tempBitmap.copy(Config.ARGB_8888, true);
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
        mFullTileBitmaps = null;
        mFullModBitmaps = null;
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
        float visualScale = state.getTileWidth() / (float) Constant.TILE_WIDTH;
        mMatrix.setScale(visualScale, visualScale);
        if (state.mDrawGridLines) {
            for (int i = 0; i < mFullTileBitmaps.length; i++) {
                mScaledTileBitmaps[i] = Bitmap.createScaledBitmap(mFullTileBitmaps[i], state.getTileWidth(), state.getTileHeight(), true);
                mCanvas.setBitmap(mScaledTileBitmaps[i]);
                mCanvas.drawLine(state.getTileWidth() / 2, 0, state.getTileWidth(), state.getTileHeight() / 2, mPaint);
                mCanvas.drawLine(state.getTileWidth() / 2, 0, 0, state.getTileHeight() / 2, mPaint);
            }
            for (int i = 0; i < mFullModBitmaps.length; i++) {
                mScaledModBitmaps[i].eraseColor(android.graphics.Color.TRANSPARENT);
                mCanvas.setBitmap(mScaledModBitmaps[i]);
                mCanvas.drawBitmap(mFullModBitmaps[i], mMatrix, mPaint);
                mCanvas.drawLine(state.getTileWidth() / 2 - mModOffsetX[i] * visualScale,
                        -mModOffsetY[i] * visualScale,
                        state.getTileWidth() - mModOffsetX[i] * visualScale,
                        state.getTileHeight() / 2 - mModOffsetY[i] * visualScale, mPaint);
                mCanvas.drawLine(state.getTileWidth() / 2 - mModOffsetX[i] * visualScale,
                        -mModOffsetY[i] * visualScale,
                        -mModOffsetX[i] * visualScale,
                        state.getTileHeight() / 2 - mModOffsetY[i] * visualScale, mPaint);
            }
        } else {
            for (int i = 0; i < mFullTileBitmaps.length; i++) {
                mScaledTileBitmaps[i] = Bitmap.createScaledBitmap(mFullTileBitmaps[i], state.getTileWidth(), state.getTileHeight(), true);
            }
            for (int i = 0; i < mFullModBitmaps.length; i++) {
                mScaledModBitmaps[i].eraseColor(android.graphics.Color.TRANSPARENT);
                mCanvas.setBitmap(mScaledModBitmaps[i]);
                mCanvas.drawBitmap(mFullModBitmaps[i], mMatrix, mPaint);
            }
        }
    }

    /**
     * Fetch the saved bitmap for a specific terrain type. The bitmap should have already been modified for use by calling remakeBitmaps.
     * 
     * @param terrain
     *            the type of terrain to fetch
     */
    public Bitmap getScaledTileBitmap(int terrain) {
        return mScaledTileBitmaps[terrain];
    }

    /**
     * Fetch the saved bitmap for a specific terrain type. The bitmap should have already been modified for use by calling remakeBitmaps.
     * 
     * @param terrain
     *            the type of terrain to fetch
     */
    public static Bitmap getFullTileBitmap(int terrain) {
        return mFullTileBitmaps[terrain];
    }

    /**
     * Fetch the saved bitmap for a specific terrain mod. The bitmap should have already been modified for use by calling remakeBitmaps.
     * 
     * @param mod
     *            the type of mod to fetch
     */
    public Bitmap getScaledModBitmap(int mod) {
        return mScaledModBitmaps[mod];
    }

    /**
     * Fetch the saved bitmap for a specific terrain mod. This is the full-sized unmodified bitmap.
     * 
     * @param mod
     *            the type of mod to fetch
     */
    public static Bitmap getFullModBitmap(int mod) {
        return mFullModBitmaps[mod];
    }

}
