package net.chetch.captainslog;

import java.util.Calendar;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.chetch.captainslog.data.LogEntry;
import net.chetch.utilities.Utils;
import net.chetch.webservices.Webservice;
import net.chetch.webservices.employees.Employee;

import java.util.List;

public class LogEntryConfirmationDialogFragment extends AppCompatDialogFragment{

    public LogEntry logEntry;
    public Employee crewMember;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        //get the content view
        View contentView = inflater.inflate(R.layout.log_entry_confirmation_dialog, null);

        ImageView iv = contentView.findViewById(R.id.crewProfileImage);
        iv.setImageBitmap(crewMember.profileImage);

        iv = contentView.findViewById(R.id.eventIcon);
        String resourceName = "ic_event_" + logEntry.getEvent().toString().toLowerCase() + "_white_24dp";
        int resource = getResources().getIdentifier(resourceName,"drawable", getContext().getPackageName());
        iv.setImageResource(resource);

        TextView tv = contentView.findViewById(R.id.crewMemberKnownAs);
        tv.setText(crewMember.getKnownAs());

        tv = contentView.findViewById(R.id.eventName);
        tv.setText(logEntry.getEvent().toString());

        tv = contentView.findViewById(R.id.eventDetails);
        String now = Utils.formatDate(Calendar.getInstance(), "dd/MM/yyyy HH:mm:ss Z");
        tv.setText("On " + now + " @ 2.3342, -243324 (lat/lon)");

        //set the close button
        Button cancelButton = contentView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        //set the close button
        Button confirmButton = contentView.findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveLogEntry();
            }
        });

        builder.setView(contentView);

        // Create the Dialog object and return it
        Dialog dialog = builder.create();

        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        Log.i("LEDC", "Dialog created");

        return dialog;
    }

    public void saveLogEntry(){

    }
}
