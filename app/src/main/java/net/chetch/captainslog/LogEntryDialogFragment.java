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
import net.chetch.webservices.employees.Employees;


public class LogEntryDialogFragment extends AppCompatDialogFragment {

    Employees crew = null;

    public LogEntryDialogFragment(Employees crew){
        this.crew = crew;
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
        View contentView = inflater.inflate(R.layout.log_entry_dialog, null);



        //set the close button
        /*Button closeButton = contentView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });*/

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
