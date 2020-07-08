package net.chetch.captainslog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.chetch.appframework.GenericDialogFragment;
import net.chetch.captainslog.data.CrewMember;
import net.chetch.captainslog.data.LogEntry;
import net.chetch.webservices.employees.Employee;

public class LogEntryConfirmationDialogFragment extends GenericDialogFragment{

    public LogEntry logEntry;
    public CrewMember crewMember;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        inflateContentView(R.layout.log_entry_confirmation_dialog);

        ImageView iv = contentView.findViewById(R.id.crewProfileImage);
        iv.setImageBitmap(crewMember.profileImage);

        iv = contentView.findViewById(R.id.eventIcon);
        String resourceName = "ic_event_" + logEntry.getEvent().toString().toLowerCase() + "_white_24dp";
        int resource = getResourceID(resourceName, "drawable");
        iv.setImageResource(resource);


        resource = getResourceID("log_entry.event." + logEntry.getEvent(), "string");
        String eventString = getString(resource);
        TextView tv = contentView.findViewById(R.id.knownAs);
        tv.setText(crewMember.getKnownAs() + " " + eventString.toLowerCase());

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

        // Create the Dialog object and return it
        Dialog dialog = createDialog();

        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        Log.i("LEDC", "Dialog created");

        return dialog;
    }

    public void saveLogEntry(){
        dismiss();

        dialogManager.onDialogPositiveClick(this);
    }
}
