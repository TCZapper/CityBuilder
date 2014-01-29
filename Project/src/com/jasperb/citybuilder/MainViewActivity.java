package com.jasperb.citybuilder;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.RelativeLayout;

import com.jasperb.citybuilder.util.TileBitmaps;
import com.jasperb.citybuilder.view.CityView;
import com.jasperb.citybuilder.view.CityViewController;
import com.jasperb.citybuilder.view.CityViewState;

public class MainViewActivity extends Activity {
    /**
     * String used for identifying this class
     */
    private static final String TAG = "MainView";
    private static final String STATE_FOCUS_ROW = "focusRow";
    private static final String STATE_FOCUS_COL = "focusCol";
    private static final String STATE_SCALE_FACTOR = "scaleFactor";
    private static final String STATE_DRAW_GRID_LINES = "drawGridLines";

    private CityViewState mState;
    private CityViewController mCityViewController;
    private OverlayController mOverlayController;
    private CityModel mCityModel;

    private boolean mAllocated = false;

    private CityView mCityView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_view);

        mCityModel = new CityModel(200, 200);
        TileBitmaps.loadFullBitmaps(this);
        mState = new CityViewState();
        mState.mScroller = new OverScroller(this);
        mCityViewController = new CityViewController();
        mOverlayController = new OverlayController();

        mCityView = (CityView) findViewById(R.id.City);

        mOverlayController.mGridButton = (ImageView) findViewById(R.id.GridButton);
        mOverlayController.mTerrainButton = (ImageView) findViewById(R.id.TerrainButton);

        mOverlayController.mTileStyleButton = (FrameLayout) findViewById(R.id.TileStyleButton);
        mOverlayController.mPaintButton = (ImageView) findViewById(R.id.PaintButton);
        mOverlayController.mSelectButton = (ImageView) findViewById(R.id.SelectButton);
        mOverlayController.mTileSyleIcon = (ImageView) findViewById(R.id.TileStyleIcon);

        mOverlayController.mTerrainTools = (LinearLayout) findViewById(R.id.TerrainTools);

        mOverlayController.mLeftButton = (ImageView) findViewById(R.id.LeftButton);
        mOverlayController.mUpButton = (ImageView) findViewById(R.id.UpButton);
        mOverlayController.mDownButton = (ImageView) findViewById(R.id.DownButton);
        mOverlayController.mRightButton = (ImageView) findViewById(R.id.RightButton);
        mOverlayController.mMoveButtons = (RelativeLayout) findViewById(R.id.MoveButtons);

        mOverlayController.mTerrainTools.setVisibility(View.GONE);
        mOverlayController.mMoveButtons.setVisibility(View.GONE);
        mOverlayController.mTileSyleIcon.setImageBitmap(TileBitmaps.getFullBitmap(mState.mTerrainTypeSelected));

        if (savedInstanceState != null) {
            // Restore state of the city view
            synchronized (mState) {
                mState.mFocusRow = savedInstanceState.getFloat(STATE_FOCUS_ROW);
                mState.mFocusCol = savedInstanceState.getFloat(STATE_FOCUS_COL);
                mState.setScaleFactor(savedInstanceState.getFloat(STATE_SCALE_FACTOR));
                mState.mDrawGridLines = savedInstanceState.getBoolean(STATE_DRAW_GRID_LINES);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "ON START");

        if (!mAllocated) {
            mCityViewController.init(this, mState);
            mOverlayController.init(this, mState);
            mState.mCityModel = mCityModel;
            mCityView.init(mCityViewController, mState);
            mAllocated = true;
        }
        mCityView.startDrawThread();
    }

    @Override
    protected void onResume() {
        super.onStart();
        Log.v(TAG, "ON RESUME");
    }

    @Override
    protected void onPause() {
        super.onStart();
        Log.v(TAG, "ON PAUSE");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "ON STOP");

        mCityView.stopDrawThread();
    }

    @Override
    public void onTrimMemory(int level) {
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            Log.v(TAG, "TRIM UI");
            if (mAllocated) {
                mCityViewController.cleanup();
                mOverlayController.cleanup();
                mCityView.cleanup();
                mAllocated = false;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        synchronized (mState) {
            savedInstanceState.putFloat(STATE_FOCUS_ROW, mState.mFocusRow);
            savedInstanceState.putFloat(STATE_FOCUS_COL, mState.mFocusRow);
            savedInstanceState.putFloat(STATE_SCALE_FACTOR, mState.getScaleFactor());
            savedInstanceState.putBoolean(STATE_DRAW_GRID_LINES, mState.mDrawGridLines);
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_view, menu);
        return true;
    }
}
