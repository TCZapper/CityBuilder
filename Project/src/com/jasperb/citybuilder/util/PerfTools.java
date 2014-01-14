/**
 * 
 */
package com.jasperb.citybuilder.util;


/**
 * @author Jasper
 * 
 */
public class PerfTools {
    private static final int MAXSAMPLES = 100;
    private static long mLastTime = 0;
    private static int mIndex = 0;
    private static int mSum = 0;
    private static int mTickList[] = new int[MAXSAMPLES];

    public static double CalcAverageTick(long newTime) {
        int newTick = (int) (newTime - mLastTime);
        mSum -= mTickList[mIndex];
        mSum += newTick;
        mTickList[mIndex] = newTick;
        if (++mIndex == MAXSAMPLES) {
            mIndex = 0;
        }
        mLastTime = newTime;

        return ((double) mSum / MAXSAMPLES);
    }
}
