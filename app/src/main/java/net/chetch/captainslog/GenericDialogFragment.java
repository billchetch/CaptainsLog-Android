package net.chetch.captainslog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;
import android.view.Window;

public class GenericDialogFragment extends AppCompatDialogFragment {

    protected Dialog dialog;
    protected View contentView;
    protected IDialogManager dialogManager;

    protected int getResourceID(String resourceName, String resourceType){
        return getResources().getIdentifier(resourceName,resourceType, getContext().getPackageName());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        dialogManager = (IDialogManager)context;
    }

    protected Dialog createDialog(){

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(contentView);

        // Create the Dialog object and return it
        dialog = builder.create();

        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        return dialog;

    }

    public boolean isShowing(){
        return dialog != null ? dialog.isShowing() : false;
    }
}
