/**
 * 
 */
package com.jasperb.citybuilder.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.jasperb.citybuilder.util.Constant.TERRAIN;

/**
 * @author Jasper
 * 
 */
public final class TileBitmaps {
    private Bitmap[] bitmaps = new Bitmap[Constant.TERRAIN.values().length];

    public TileBitmaps() {
        Canvas canvas = new Canvas();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        // Draw the tile to fit into the bitmap, and enlarge it a bit to remove the lines between tiles
        Path path = new Path();
        path.moveTo(Constant.TILE_WIDTH / 2 + 1, 0);
        path.lineTo(Constant.TILE_WIDTH + 2, Constant.TILE_HEIGHT / 2 + 0.5f);
        path.lineTo(Constant.TILE_WIDTH / 2 + 1, Constant.TILE_HEIGHT + 1);
        path.lineTo(0, Constant.TILE_HEIGHT / 2 + 0.5f);

        for (TERRAIN terrain : TERRAIN.values()) {
            bitmaps[terrain.ordinal()] = Bitmap.createBitmap(Constant.TILE_WIDTH + 2, Constant.TILE_HEIGHT + 1, Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bitmaps[terrain.ordinal()]);
            switch (terrain) {
            case GRASS:
                paint.setColor(Color.GREEN);
                break;
            case DIRT:
                paint.setColor(Color.DKGRAY);
                break;
            }
            canvas.drawPath(path, paint);
        }
    }

    public Bitmap getBitmap(TERRAIN terrain) {
        return bitmaps[terrain.ordinal()];
    }
}
