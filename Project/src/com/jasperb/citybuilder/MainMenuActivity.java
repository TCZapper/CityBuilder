package com.jasperb.citybuilder;

import com.jasperb.citybuilder.dialog.CitySelectDialogFragment;
import com.jasperb.citybuilder.dialog.CreateCityDialogFragment;
import com.jasperb.citybuilder.dialog.CitySelectDialogFragment.CitySelectDialogListener;
import com.jasperb.citybuilder.dialog.CreateCityDialogFragment.CreateCityDialogListener;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
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
    
    public void gotoMainView(View v) {
        CitySelectDialogFragment newFragment = new CitySelectDialogFragment();
        Bundle b = new Bundle();
        b.putInt(CitySelectDialogFragment.TYPE, CitySelectDialogFragment.TYPE_LOAD);
        newFragment.setArguments(b);
        newFragment.show(getFragmentManager(), "CitySelectDialog");
    }

    public void gotoMainView(final String cityName, final int width, final int height) {
        findViewById(R.id.SplashScreen).setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // This method will be executed once the timer is over
                Intent i = new Intent(MainMenuActivity.this, MainViewActivity.class);
                //EditText editText = (EditText) findViewById(R.id.edit_message);
                //String message = editText.getText().toString();
                i.putExtra(MainViewActivity.STATE_CITY_NAME, cityName);
                i.putExtra(MainViewActivity.STATE_CITY_WIDTH, width);
                i.putExtra(MainViewActivity.STATE_CITY_HEIGHT, height);
                startActivity(i);
            }
        }, 500);
    }
    
    public void gotoMainView(final String cityName) {
        findViewById(R.id.SplashScreen).setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // This method will be executed once the timer is over
                Intent i = new Intent(MainMenuActivity.this, MainViewActivity.class);
                //EditText editText = (EditText) findViewById(R.id.edit_message);
                //String message = editText.getText().toString();
                i.putExtra(MainViewActivity.STATE_CITY_NAME, cityName);
                startActivity(i);
            }
        }, 500);
    }

    public void clearSave(View v) {
        CitySelectDialogFragment newFragment = new CitySelectDialogFragment();
        Bundle b = new Bundle();
        b.putInt(CitySelectDialogFragment.TYPE, CitySelectDialogFragment.TYPE_DELETE);
        newFragment.setArguments(b);
        newFragment.show(getFragmentManager(), "CitySelectDialog");
    }

    public void clearSave(String cityName) {
        deleteFile(cityName);
    }

    @Override
    public void onCitySelectDialogPick(int type, String cityName) {
        if(type == CitySelectDialogFragment.TYPE_LOAD) {
            if(cityName.equalsIgnoreCase(CitySelectDialogFragment.NEW_CITY)) {
                CreateCityDialogFragment newFragment = new CreateCityDialogFragment();
                newFragment.show(getFragmentManager(), "CreateCityDialog");
            } else {
                gotoMainView(cityName);
            }
        } else if(type == CitySelectDialogFragment.TYPE_DELETE) {
            clearSave(cityName);
        }
    }

    @Override
    public void onCreateCityDialogAccept(String inputText, int width, int height) {
        inputText = inputText.trim();
        String[] fileNames = getFilesDir().list();
        for(int i = 0; i < fileNames.length; i++) {
            if(fileNames[i].equalsIgnoreCase(inputText)) {
                Toast.makeText(this, "City name already in use.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        gotoMainView(inputText, width, height);
    }
    
}
