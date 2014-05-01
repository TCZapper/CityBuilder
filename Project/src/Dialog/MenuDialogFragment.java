package Dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.jasperb.citybuilder.R;

public class MenuDialogFragment extends DialogFragment {
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Menu")
               .setItems(R.array.menu_options, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       if(which == 0) {//Save
                           
                       } else if(which == 1) {//Restore
                           
                       } else if(which == 2) {//Settings
                           
                       } else if(which == 3) {//Main Menu
                           
                       }
                   }
               });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
