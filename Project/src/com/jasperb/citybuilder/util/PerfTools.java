/**
 * 
 */
package com.jasperb.citybuilder.util;

import android.util.Log;

/**
 * @author Jasper
 * 
 */
public class PerfTools {
    /**
     * Identifier string for debug messages originating from this class
     */
    public static final String TAG = "FPS";

    private static final int MAXSAMPLES = 100;
    private static int mIndex = 0;
    private static int mSum = 0;
    private static int mTickList[] = new int[MAXSAMPLES];

    public static double CalcAverageTick(int newtick) {
        mSum -= mTickList[mIndex];
        mSum += newtick;
        mTickList[mIndex] = newtick;
        if (++mIndex == MAXSAMPLES) {
            mIndex = 0;
        }
        Log.d(TAG, "" + 1 / ((double) mSum / MAXSAMPLES));

        return ((double) mSum / MAXSAMPLES);
    }
}
