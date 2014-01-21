/**
 * 
 */
package com.jasperb.citybuilder.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import com.jasperb.citybuilder.util.Constant.TERRAIN;
import com.jasperb.citybuilder.view.CityViewState;

/**
 * @author Jasper
 * 
 */
public class TileBitmaps {
    private Bitmap[] mFullBitmaps = new Bitmap[Constant.TERRAIN.values().length];
    private Bitmap[] mScaledBitmaps = new Bitmap[Constant.TERRAIN.values().length];

    public TileBitmaps() {
        Canvas canvas = new Canvas();
        Matrix m = new Matrix();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);// Anti-aliasing helps make smoother edges
        paint.setStyle(Paint.Style.FILL);

        // Draw the tile to fit into the bitmap
        Path path = new Path();
        path.moveTo(Constant.TILE_WIDTH / 2, 0);
        path.lineTo(Constant.TILE_WIDTH, Constant.TILE_HEIGHT / 2);
        path.lineTo(Constant.TILE_WIDTH / 2, Constant.TILE_HEIGHT);
        path.lineTo(0, Constant.TILE_HEIGHT / 2);

        // To eliminate lines between tiles, we extend the length of the edges/size of a tile by 2 pixels
        double edgeLength = Math.sqrt((Constant.TILE_WIDTH / 2) * (Constant.TILE_WIDTH / 2)
                + (Constant.TILE_HEIGHT / 2) * (Constant.TILE_HEIGHT / 2));
        edgeLength = (edgeLength + 2) / edgeLength;
        m.setScale((float) edgeLength, (float) edgeLength);
        path.transform(m);

        // Draw every tile into their own bitmap
        for (TERRAIN terrain : TERRAIN.values()) {
            mFullBitmaps[terrain.ordinal()] = Bitmap.createBitmap(Constant.TILE_WIDTH + 4, Constant.TILE_HEIGHT + 2, Bitmap.Config.ARGB_8888);
            canvas.setBitmap(mFullBitmaps[terrain.ordinal()]);
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
    
    public void resizeBitmaps(CityViewState state) {
        for(int i = 0; i < mFullBitmaps.length; i++) {
            mScaledBitmaps[i] = Bitmap.createScaledBitmap(mFullBitmaps[i], state.getTileWidth(), state.getTileHeight(), true);
        }
    }

    public Bitmap getBitmap(TERRAIN terrain) {
        return mScaledBitmaps[terrain.ordinal()];
    }
}
