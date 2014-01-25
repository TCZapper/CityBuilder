package com.jasperb.citybuilder;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

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
    private ImageView mGridButton, mTerrainButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_view);

        mCityModel = new CityModel(200, 200);

        mCityView = (CityView) findViewById(R.id.City);
        mGridButton = (ImageView) findViewById(R.id.Grid);
        mTerrainButton = (ImageView) findViewById(R.id.Terrain);
        
        OnClickListener clickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.equals(mGridButton)) {
                    mCityView.setDrawGridLines(!mCityView.getDrawGridLines());
                }
            }
        };
        mGridButton.setClickable(true);
        mGridButton.setOnClickListener(clickListener);
        mTerrainButton.setClickable(true);
        mTerrainButton.setOnClickListener(clickListener);
        

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
