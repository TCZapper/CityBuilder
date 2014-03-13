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
import android.graphics.Rect;
import android.util.Log;

import com.jasperb.citybuilder.util.Constant.OBJECTS;

/**
 * @author Jasper
 * 
 */
public class ObjectBitmaps {
    /**
     * String used for identifying this class.
     */
    public static final String TAG = "ObjectBitmaps";

    public static Bitmap[][] mFullObjectBitmaps = null;

    public ObjectBitmaps() {}

    /**
     * Create a TileBitmaps objects, loading the static bitmaps from the assets into memory.
     * 
     * @param context
     */
    public static void loadStaticBitmaps(Context context) {
        if (mFullObjectBitmaps == null) {
            mFullObjectBitmaps = new Bitmap[OBJECTS.count][];
            AssetManager assets = context.getAssets();
            InputStream ims = null;
            Bitmap tempBitmap;
            try {
                Canvas canvas = new Canvas();

                int i = OBJECTS.TEST2X4;
                int sliceWidth, sliceCount, height, width;
                Rect trimmedRect = new Rect(), bitmapRect = new Rect();

                ims = assets.open("BUILDINGS/test2x4.png");
                tempBitmap = BitmapFactory.decodeStream(ims);
                height = tempBitmap.getHeight();
                width = tempBitmap.getWidth();
                sliceWidth = (Constant.TILE_WIDTH / 2) * (2 + (OBJECTS.objectNumRows[i] - 1));
                sliceCount = width / sliceWidth;
                mFullObjectBitmaps[i] = new Bitmap[sliceCount];

                for (int j = 0; j < sliceCount; j++) {
                    if (j == 0) {
                        trimmedRect.set(0, 0, sliceWidth, height);
                    } else if (j == sliceCount - 1) {
                        trimmedRect.set(sliceWidth * j, 0, width, height);
                    } else {
                        trimmedRect.set(sliceWidth * j, 0, sliceWidth * (j + 1), height);
                    }
                    bitmapRect.set(0, 0, trimmedRect.width(), trimmedRect.height());
                    mFullObjectBitmaps[i][j] = Bitmap.createBitmap(trimmedRect.width(), height, Config.ARGB_8888);
                    canvas.setBitmap(mFullObjectBitmaps[i][j]);
                    canvas.drawBitmap(tempBitmap, trimmedRect, bitmapRect, null);
                }
                ims.close();

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
}
