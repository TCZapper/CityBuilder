package com.jasperb.citybuilder;

import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.RelativeLayout;

import com.jasperb.citybuilder.cityview.CityView;
import com.jasperb.citybuilder.cityview.CityViewController;
import com.jasperb.citybuilder.dialog.GridViewDialogFragment;
import com.jasperb.citybuilder.dialog.GridViewDialogFragment.GridViewDialogListener;
import com.jasperb.citybuilder.util.FileStreamUtils;
import com.jasperb.citybuilder.util.ObjectBitmaps;
import com.jasperb.citybuilder.util.TileBitmaps;

public class MainViewActivity extends Activity implements GridViewDialogListener {
    /**
     * String used for identifying this class
     */
    private static final String TAG = "MainView";
    public static final String STATE_CITY_NAME = "cityName";
    public static final String STATE_CITY_WIDTH = "cityWidth";
    public static final String STATE_CITY_HEIGHT = "cityHeight";
    public static final String STATE_FOCUS_ROW = "focusRow";
    public static final String STATE_FOCUS_COL = "focusCol";
    public static final String STATE_SCALE_FACTOR = "scaleFactor";
    public static final String STATE_DRAW_GRID_LINES = "drawGridLines";

    private SharedState mState;
    private CityViewController mCityViewController;
    private OverlayController mOverlayController;
    private CityModel mCityModel;

    private boolean mAllocated = false;

    private CityView mCityView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_view);
        
        Intent intent = getIntent();
        String cityName = intent.getStringExtra(STATE_CITY_NAME);
        Log.v(TAG,"ON CREATE: " + cityName);
        
        try {
            mCityModel = new CityModel(new FileStreamUtils(openFileInput(cityName)));
        } catch (FileNotFoundException e) {
            mCityModel = new CityModel(intent.getIntExtra(STATE_CITY_WIDTH, 200), intent.getIntExtra(STATE_CITY_HEIGHT, 200));
        }
        if(mCityModel.getWidth() == 0 || mCityModel.getHeight() == 0) {//Error (probably in restoring from saved model)
            finish();
        }
        mState = new SharedState();
        mState.mActivity = this;
        mState.mScroller = new OverScroller(this, new AccelerateInterpolator(Constant.INTERPOLATE_ACCELERATION));
        mState.mScroller.setFriction(Constant.FLING_FRICTION);
        mCityViewController = new CityViewController();
        mOverlayController = new OverlayController();
        mState.mOverlay = mOverlayController;
        mState.mCityName = cityName;

        mCityView = (CityView) findViewById(R.id.City);

        mOverlayController.mGridButton = (ImageView) findViewById(R.id.GridButton);
        mOverlayController.mTerrainButton = (ImageView) findViewById(R.id.TerrainButton);
        mOverlayController.mObjectsButton = (ImageView) findViewById(R.id.ObjectsButton);
        mOverlayController.mMenuButton = (ImageView) findViewById(R.id.MenuButton);
        
        mOverlayController.mObjectTools = (LinearLayout) findViewById(R.id.ObjectTools);
        mOverlayController.mBuildingsButton = (ImageView) findViewById(R.id.BuildingsButton);
        mOverlayController.mSelectObjectButton = (ImageView) findViewById(R.id.SelectObjectButton);

        mOverlayController.mTerrainTools = (LinearLayout) findViewById(R.id.TerrainTools);
        mOverlayController.mTileStyleButton = (FrameLayout) findViewById(R.id.TileStyleButton);
        mOverlayController.mPaintButton = (ImageView) findViewById(R.id.PaintButton);
        mOverlayController.mSelectTerrainButton = (ImageView) findViewById(R.id.SelectTerrainButton);
        mOverlayController.mTileSyleIcon = (ImageView) findViewById(R.id.TileStyleIcon);
        mOverlayController.mEyedropperButton = (ImageView) findViewById(R.id.EyedropperButton);
        mOverlayController.mBlendButton = (ImageView) findViewById(R.id.BlendButton);

        mOverlayController.mBrushTools = (LinearLayout) findViewById(R.id.BrushTools);
        mOverlayController.mBrushSquare1x1 = (ImageView) findViewById(R.id.Brush1x1);
        mOverlayController.mBrushSquare3x3 = (ImageView) findViewById(R.id.Brush3x3);
        mOverlayController.mBrushSquare5x5 = (ImageView) findViewById(R.id.Brush5x5);

        mOverlayController.mLeftButton = (ImageView) findViewById(R.id.LeftButton);
        mOverlayController.mUpButton = (ImageView) findViewById(R.id.UpButton);
        mOverlayController.mDownButton = (ImageView) findViewById(R.id.DownButton);
        mOverlayController.mRightButton = (ImageView) findViewById(R.id.RightButton);
        mOverlayController.mMoveButtons = (RelativeLayout) findViewById(R.id.MoveButtons);

        mOverlayController.mGeneralTools = (LinearLayout) findViewById(R.id.GeneralTools);
        mOverlayController.mAcceptButton = (ImageView) findViewById(R.id.AcceptButton);
        mOverlayController.mCancelButton = (ImageView) findViewById(R.id.CancelButton);
        mOverlayController.mUndoButton = (ImageView) findViewById(R.id.UndoButton);
        mOverlayController.mRedoButton = (ImageView) findViewById(R.id.RedoButton);

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
            TileBitmaps.loadStaticBitmaps(this);
            ObjectBitmaps.loadStaticBitmaps(this);
            mState.mCityModel = mCityModel;
            mCityViewController.init(this, mState);
            mOverlayController.init(this, mState);
            mOverlayController.update();
            mCityView.init(mCityViewController, mState);
            mAllocated = true;
        }
        mCityView.startDrawThread();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "ON RESUME");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "ON PAUSE");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "ON STOP");

        mCityView.stopDrawThread();
        try {
            mCityModel.save(new FileStreamUtils(openFileOutput(mState.mCityName, Context.MODE_PRIVATE)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            Log.v(TAG, "TRIM UI");
            if (mAllocated) {
                mCityViewController.cleanup();
                mOverlayController.cleanup();
                mCityView.cleanup();
                mCityView.stopDrawThread();//blocks until draw thread is done drawing
                TileBitmaps.freeStaticBitmaps();
                ObjectBitmaps.freeStaticBitmaps();
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

    @Override
    public void onGridViewDialogAccept(int type, int selectedIndex) {
        if (mState != null) {
            if (type == GridViewDialogFragment.TYPE_BUILDINGS) {
                mState.mSelectedObjectType = selectedIndex;
                mState.mDestRow = (int) mState.mFocusRow;
                mState.mDestCol = (int) mState.mFocusCol;
            } else {
                mState.mSelectedTerrainType = selectedIndex;
            }
            mState.notifyOverlay();
        }
    }
}
