package net.chetch.captainslog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class ErrorDialogFragment extends GenericDialogFragment implements OnClickListener {

    public int errorType;
    public String errorMessage;

    public ErrorDialogFragment(int errorType, String errorMessage){
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    public int getErrorType(){ return errorType; }

    public boolean isErrorType(int errorType){
        return this.errorType == errorType;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        contentView = inflater.inflate(R.layout.error_dialog, null);
        TextView details = (TextView)contentView.findViewById(R.id.errorDetails);
        details.setText(errorType + ": " + errorMessage);

        contentView.setOnClickListener(this);

        // Create the AlertDialog object and return it
        dialog = createDialog();

        return dialog;
    }

    @Override
    public void onClick(View v){
        dismiss();

        dialogManager.onDialogPositiveClick(this);
    }


}
