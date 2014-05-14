/**
 * 
 */
package com.jasperb.citybuilder.cityview;

import com.jasperb.citybuilder.SharedState;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Custom view for handling rendering the city model. Includes the ability to pan, fling and scale within the view.
 * 
 * @author Jasper
 * 
 */
public class CityView extends SurfaceView implements SurfaceHolder.Callback {
    /**
     * String used for identifying this class.
     */
    public static final String TAG = "CityView";

    private CityViewController mController = null;
    private DrawThread mDrawThread = null;
    private SurfaceHolder mSurfaceHolder = null;
    private boolean mSurfaceExists = false;
    private SharedState mState = null;

    /**
     * Class constructor taking a context and an attribute set. This constructor is used by the layout engine to construct a
     * {@link CityView} from a set of XML attributes.
     * 
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs
     *            An attribute set which can contain attributes inherited from {@link android.view.View}.
     */
    public CityView(Context context, AttributeSet attrs) {
        super(context, attrs);
        construct();
    }

    /**
     * Class constructor taking a context. This constructor is used by the layout engine to construct a {@link CityView}.
     * 
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     */
    public CityView(Context context) {
        super(context);
        construct();
    }

    /**
     * Common method for constructors
     */
    private void construct() {
        mSurfaceHolder = getHolder();//Do this here because we hold onto this for the lifetime of the view
        mSurfaceHolder.addCallback(this);
    }

    /**
     * Initialize and allocate the necessary components of the view, except those related to the drawing thread
     */
    public void init(CityViewController controller, SharedState state) {
        mController = controller;
        mState = state;

        startDrawThread();
    }

    /**
     * Cleanup the components of the view allocated by init()
     */
    public void cleanup() {
        mController = null;
        mState = null;

        stopDrawThread();
    }

    /**
     * Start the drawing thread if possible
     */
    public void startDrawThread() {
        // In some instances, the surface persists even when the view is stopped. We still stop the draw thread.
        // There's no point starting the thread before the surface is created.
        if (mSurfaceExists && mDrawThread == null && mState != null) {
            Log.v(TAG, "startDrawThread");

            mDrawThread = new DrawThread(mSurfaceHolder, mState);

            mDrawThread.start();
        }
    }

    /**
     * Stop the drawing thread if it exists
     */
    public void stopDrawThread() {
        if (mDrawThread != null) {
            Log.v(TAG, "stopDrawThread");

            mDrawThread.stopThread();//tell thread to finish up

            boolean retry = true;
            while (retry) {
                try {
                    mDrawThread.join();//block current thread until draw thread finishes execution
                    retry = false;
                } catch (InterruptedException e) {}
            }

            mDrawThread = null;
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        Log.v(TAG, "SURFACE CHANGED");
        synchronized (mState) {
            mState.UIS_Width = width;
            mState.UIS_Height = height;
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "SURFACE CREATED");
        mSurfaceExists = true;
        startDrawThread();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // WARNING: after this method returns, the Surface/Canvas must never be touched again!
        Log.v(TAG, "SURFACE DESTROYED");
        mSurfaceExists = false;
        stopDrawThread();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mController.onTouchEvent(event);
    }
}
