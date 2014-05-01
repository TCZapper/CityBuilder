package com.jasperb.citybuilder.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

import com.jasperb.citybuilder.Constant;
import com.jasperb.citybuilder.R;

public class CreateCityDialogFragment extends DialogFragment {
    public interface CreateCityDialogListener {
        public void onCreateCityDialogAccept(String inputText, int width, int height);
    }

    CreateCityDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (CreateCityDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement CreateCityDialogListener");
        }
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.fragment_create_city_dialog, null);
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Create City");

        // Set up the input
        final EditText textboxCityName = (EditText) dialogView.findViewById(R.id.cityNameTextbox);
        
        String[] displayedValues = new String[Constant.MAX_WORLD_SIZE / Constant.MIN_WORLD_SIZE];
        for(int i = 0; i < displayedValues.length; i++) {
            displayedValues[i] = String.valueOf((i + 1) * Constant.MIN_WORLD_SIZE);
        }
        
        final NumberPicker pickerWidth = (NumberPicker) dialogView.findViewById(R.id.widthNumberPicker);
        pickerWidth.setMinValue(0);
        pickerWidth.setMaxValue(displayedValues.length-1);
        pickerWidth.setDisplayedValues(displayedValues);
        final NumberPicker pickerHeight = (NumberPicker) dialogView.findViewById(R.id.heightNumberPicker);
        pickerHeight.setMinValue(0);
        pickerHeight.setMaxValue(displayedValues.length-1);
        pickerHeight.setDisplayedValues(displayedValues);

        builder.setView(dialogView);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onCreateCityDialogAccept(textboxCityName.getText().toString(),
                        (pickerWidth.getValue() + 1) * Constant.MIN_WORLD_SIZE,
                        (pickerHeight.getValue() + 1) * Constant.MIN_WORLD_SIZE);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
