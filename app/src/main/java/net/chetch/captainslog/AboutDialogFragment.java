package net.chetch.captainslog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import net.chetch.utilities.Logger;


public class AboutDialogFragment extends GenericDialogFragment {

    public AboutDialogFragment(){

        //this.repository = repository;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        //get the content view
        View contentView = inflater.inflate(R.layout.about_dialog, null);

        //fill in the about stuff
        String blurb = "Blurb goes here...";
        TextView btv = contentView.findViewById(R.id.aboutBlurb);
        btv.setText(blurb);

        //fill in log info
        String logData = Logger.read();
        final TextView ltv = contentView.findViewById(R.id.log);
        if(logData != null) {
            ltv.setText(logData);
        }

        builder.setView(contentView).setTitle(R.string.app_name);

        //set clear log button
        Button clearLogButton = contentView.findViewById(R.id.clearLogButton);
        clearLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Logger.clear();
                ltv.setText(Logger.read());
            }
        });

        //set the close button
        Button closeButton = contentView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
