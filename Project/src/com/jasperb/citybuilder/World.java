/**
 * 
 */
package com.jasperb.citybuilder;

import android.util.Log;

/**
 * @author Jasper
 *
 */
public class World { 
	public enum TERRAIN { GRASS, DIRT };
	
	private int mWidth, mHeight;
	private TERRAIN[][] mTerrainMap;
	
	public int getWidth() {
	    return mWidth;
	}
	
	public int getHeight() {
        return mHeight;
    }
	
	@SuppressWarnings("unused")
	private World() {}
	public World(int width, int height) {
		mWidth = width;
		mHeight = height;
		
		mTerrainMap = new TERRAIN[mHeight][mWidth];
		for (int i = 0; i < mHeight; i++) {
			for(int j = 0; j < mWidth; j++) {
				mTerrainMap[i][j] = TERRAIN.GRASS;
			}
		}
	}
	
	public TERRAIN getTerrain(int row, int col) {
		return mTerrainMap[row][col];
	}
}
