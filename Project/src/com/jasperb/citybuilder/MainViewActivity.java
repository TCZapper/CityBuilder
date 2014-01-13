package com.jasperb.citybuilder;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

import com.jasperb.citybuilder.view.CityView;

public class MainViewActivity extends Activity {
    private CityModel mCityModel;
    private CityView mCityView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_view);

        mCityModel = new CityModel(10, 5);

        mCityView = (CityView) findViewById(R.id.City);
        mCityView.setCityModel(mCityModel);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_view, menu);
        return true;
    }

}
