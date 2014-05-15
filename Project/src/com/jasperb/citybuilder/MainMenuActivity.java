package com.jasperb.citybuilder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import com.jasperb.citybuilder.dialog.CitySelectDialogFragment;
import com.jasperb.citybuilder.dialog.CitySelectDialogFragment.CitySelectDialogListener;
import com.jasperb.citybuilder.dialog.CreateCityDialogFragment;
import com.jasperb.citybuilder.dialog.CreateCityDialogFragment.CreateCityDialogListener;

public class MainMenuActivity extends Activity implements CitySelectDialogListener, CreateCityDialogListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        findViewById(R.id.SplashScreen).setVisibility(View.GONE);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    /**
     * User has requested to open a city, but has not yet chosen which city, so we must ask
     * 
     * @param v
     *            the view triggering this function
     */
    public void gotoMainView(View v) {
        CitySelectDialogFragment newFragment = new CitySelectDialogFragment();
        Bundle b = new Bundle();
        b.putInt(CitySelectDialogFragment.TYPE, CitySelectDialogFragment.TYPE_LOAD);
        newFragment.setArguments(b);
        newFragment.show(getFragmentManager(), "CitySelectDialog");
    }

    /**
     * Open the main view for a new city
     * 
     * @param cityName
     *            name of the new city
     * @param width
     *            width of the new city
     * @param height
     *            height of the new city
     */
    public void gotoMainView(final String cityName, final int width, final int height) {
        findViewById(R.id.SplashScreen).setVisibility(View.VISIBLE);//Show a splash screen before loading the main view

        //Delay the main view loading to let the splash screen to show (a simple hack to produce a "loading screen")
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // This method will be executed once the timer is over
                Intent i = new Intent(MainMenuActivity.this, MainViewActivity.class);
                i.putExtra(MainViewActivity.STATE_CITY_NAME, cityName);
                i.putExtra(MainViewActivity.STATE_CITY_WIDTH, width);
                i.putExtra(MainViewActivity.STATE_CITY_HEIGHT, height);
                startActivity(i);
            }
        }, 500);//delay for 0.5s
    }

    /**
     * Open the main view with an existing city
     * 
     * @param cityName
     *            name of an existing city
     */
    public void gotoMainView(final String cityName) {
        findViewById(R.id.SplashScreen).setVisibility(View.VISIBLE);//Show a splash screen before loading the main view

        //Delay the main view loading to let the splash screen to show (a simple hack to produce a "loading screen")
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // This method will be executed once the timer is over
                Intent i = new Intent(MainMenuActivity.this, MainViewActivity.class);
                i.putExtra(MainViewActivity.STATE_CITY_NAME, cityName);
                startActivity(i);
            }
        }, 500);
    }

    /**
     * User has requested to delete an existing city, but we don't know which, so we must ask
     * 
     * @param v
     *            the view triggering this function
     */
    public void clearSave(View v) {
        CitySelectDialogFragment newFragment = new CitySelectDialogFragment();
        Bundle b = new Bundle();
        b.putInt(CitySelectDialogFragment.TYPE, CitySelectDialogFragment.TYPE_DELETE);
        newFragment.setArguments(b);
        newFragment.show(getFragmentManager(), "CitySelectDialog");
    }

    /**
     * Deletes an existing city
     * 
     * @param cityName
     *            the name of the city to delete
     */
    public void clearSave(String cityName) {
        deleteFile(cityName);
    }

    @Override
    public void onCitySelectDialogPick(int type, String cityName) {
        if (type == CitySelectDialogFragment.TYPE_LOAD) {
            if (cityName.equalsIgnoreCase(CitySelectDialogFragment.NEW_CITY)) {
                CreateCityDialogFragment newFragment = new CreateCityDialogFragment();
                newFragment.show(getFragmentManager(), "CreateCityDialog");
            } else {
                gotoMainView(cityName);
            }
        } else if (type == CitySelectDialogFragment.TYPE_DELETE) {
            clearSave(cityName);
        }
    }

    @Override
    public void onCreateCityDialogAccept(String inputText, int width, int height) {
        inputText = inputText.trim();
        String[] fileNames = getFilesDir().list();
        for (int i = 0; i < fileNames.length; i++) {
            if (fileNames[i].equalsIgnoreCase(inputText)) {
                Toast.makeText(this, "City name already in use.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        gotoMainView(inputText, width, height);
    }

}
