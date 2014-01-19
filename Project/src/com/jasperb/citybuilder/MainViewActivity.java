package com.jasperb.citybuilder;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.jasperb.citybuilder.view.CityView;

public class MainViewActivity extends Activity {
    private static final String TAG = "MainView";
    private static final String STATE_FOCUS_ROW = "focusRow";
    private static final String STATE_FOCUS_COL = "focusCol";
    private static final String STATE_SCALE_FACTOR = "scaleFactor";
    private static final String STATE_DRAW_GRID_LINES = "drawGridLines";

    private CityModel mCityModel;
    private CityView mCityView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_view);

        mCityModel = new CityModel(10, 10);

        mCityView = (CityView) findViewById(R.id.City);

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
        Log.d(TAG, "ON START");

        if (!mCityView.isAllocated()) {
            mCityView.setCityModel(mCityModel);
            mCityView.init();
            mCityView.initCanvas();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            Log.d(TAG, "TRIM UI");
            if (mCityView.isAllocated()) {
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
