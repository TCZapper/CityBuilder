package com.jasperb.citybuilder;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.jasperb.citybuilder.util.Constant.CITY_VIEW_MODES;
import com.jasperb.citybuilder.util.Constant.TERRAIN;
import com.jasperb.citybuilder.util.Constant.TERRAIN_TOOLS;
import com.jasperb.citybuilder.util.TileBitmaps;
import com.jasperb.citybuilder.view.CityView;

public class MainViewActivity extends Activity {
    /**
     * String used for identifying this class
     */
    private static final String TAG = "MainView";
    private static final String STATE_FOCUS_ROW = "focusRow";
    private static final String STATE_FOCUS_COL = "focusCol";
    private static final String STATE_SCALE_FACTOR = "scaleFactor";
    private static final String STATE_DRAW_GRID_LINES = "drawGridLines";

    private CityModel mCityModel;
    private CityView mCityView;
    private ImageView mGridButton, mTerrainButton, mPaintButton, mSelectButton, mTileSyleIcon;
    private FrameLayout mTileStyleButton;
    private LinearLayout mTerrainTools;
    private TileBitmaps mTileBitmaps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_view);

        mCityModel = new CityModel(200, 200);
        mTileBitmaps = new TileBitmaps(this);

        mCityView = (CityView) findViewById(R.id.City);
        mGridButton = (ImageView) findViewById(R.id.GridButton);
        mTerrainButton = (ImageView) findViewById(R.id.TerrainButton);
        mTileStyleButton = (FrameLayout) findViewById(R.id.TileStyleButton);
        mPaintButton = (ImageView) findViewById(R.id.PaintButton);
        mSelectButton = (ImageView) findViewById(R.id.SelectButton);
        mTileSyleIcon = (ImageView) findViewById(R.id.TileStyleIcon);
        mTerrainTools = (LinearLayout) findViewById(R.id.TerrainTools);
        
        mTerrainTools.setVisibility(View.GONE);
        mTileSyleIcon.setImageBitmap(mTileBitmaps.getFullBitmap(mCityView.getTerrainTypeSelected()));
        
        OnClickListener clickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.equals(mGridButton)) {
                    mCityView.setDrawGridLines(!mCityView.getDrawGridLines());
                } else if(v.equals(mTerrainButton)) {
                    if(mCityView.getMode() == CITY_VIEW_MODES.EDIT_TERRAIN) {
                        mCityView.setMode(CITY_VIEW_MODES.VIEW);
                        mTerrainTools.setVisibility(View.GONE);
                    } else {
                        mCityView.setMode( CITY_VIEW_MODES.EDIT_TERRAIN);
                        mTerrainTools.setVisibility(View.VISIBLE);
                    }
                } else if(v.equals(mTileStyleButton)) {
                    int terrain = mCityView.getTerrainTypeSelected() + 1;
                    if(terrain == TERRAIN.count) {
                        terrain = 0;
                    }
                    mCityView.setTerrainTypeSelected(terrain);
                    mTileSyleIcon.setImageBitmap(mTileBitmaps.getFullBitmap(terrain));
                } else if(v.equals(mPaintButton)) {
                    mCityView.setTool(TERRAIN_TOOLS.BRUSH);
                } else if(v.equals(mSelectButton)) {
                    mCityView.setTool(TERRAIN_TOOLS.SELECT);
                }
            }
        };
        mGridButton.setOnClickListener(clickListener);
        mTerrainButton.setOnClickListener(clickListener);
        mTileStyleButton.setOnClickListener(clickListener);
        mPaintButton.setOnClickListener(clickListener);
        mSelectButton.setOnClickListener(clickListener);
        

        if (savedInstanceState != null) {
            // Restore state of the city view
            mCityView.setFocusCoords(savedInstanceState.getFloat(STATE_FOCUS_ROW), savedInstanceState.getFloat(STATE_FOCUS_COL));
            mCityView.setScaleFactor(savedInstanceState.getFloat(STATE_SCALE_FACTOR));
            mCityView.setDrawGridLines(savedInstanceState.getBoolean(STATE_DRAW_GRID_LINES));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "ON START");

        if (!mCityView.isEverythingAllocated()) {
            mCityView.setCityModel(mCityModel);
            mCityView.init();
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
        Log.v(TAG,"ON STOP");
        
        mCityView.stopDrawThread();
    }

    @Override
    public void onTrimMemory(int level) {
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            Log.v(TAG, "TRIM UI");
            if (mCityView.isEverythingAllocated()) {
                mCityView.cleanup();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the state of the city view
        savedInstanceState.putFloat(STATE_FOCUS_ROW, mCityView.getFocusRow());
        savedInstanceState.putFloat(STATE_FOCUS_COL, mCityView.getFocusCol());
        savedInstanceState.putFloat(STATE_SCALE_FACTOR, mCityView.getScaleFactor());
        savedInstanceState.putBoolean(STATE_DRAW_GRID_LINES, mCityView.getDrawGridLines());

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_view, menu);
        return true;
    }

}
