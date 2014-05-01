package Dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class CitySelectDialogFragment extends DialogFragment {
    public static final int TYPE_LOAD = 0, TYPE_DELETE = 1;
    public static final String TYPE = "Type";
    public static final String NEW_CITY = "New City";

    public interface CitySelectDialogListener {
        public void onCitySelectDialogPick(int type, String cityName);
    }
    CitySelectDialogListener mListener = null;
    private String[] options;
    private int mType;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mType = getArguments().getInt(TYPE);

        //Create array of options
        String[] fileNames = getActivity().getFilesDir().list();
        if (mType == TYPE_LOAD) {
            options = new String[fileNames.length + 1];
            options[0] = NEW_CITY;
            System.arraycopy(fileNames, 0, options, 1, fileNames.length);
        } else if (mType == TYPE_DELETE) {
            options = fileNames;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Pick a City");
        builder.setTitle("Menu")
               .setItems(options, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       mListener.onCitySelectDialogPick(mType, options[which]);
                   }
               });

        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (CitySelectDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement CitySelectDialogListener");
        }
    }
}
