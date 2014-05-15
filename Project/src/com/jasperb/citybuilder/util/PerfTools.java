/**
 * 
 */
package com.jasperb.citybuilder.util;

/**
 * Simple object to track the average frame draw time
 */
public class PerfTools {
    private static final int MAXSAMPLES = 75;
    private static int mIndex = 0;
    private static int mSum = 0;
    private static int mTickList[] = new int[MAXSAMPLES];

    public static double CalcAverageTick(int newTick) {
        mSum -= mTickList[mIndex];
        mSum += newTick;
        mTickList[mIndex] = newTick;
        if (++mIndex == MAXSAMPLES) {
            mIndex = 0;
        }

        return ((double) mSum / MAXSAMPLES);
    }
}
