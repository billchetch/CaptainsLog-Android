package net.chetch.captainslog;

import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import net.chetch.captainslog.data.LogEntry;
import net.chetch.utilities.Logger;
import net.chetch.webservices.employees.Employee;
import net.chetch.webservices.employees.Employees;

import java.util.List;


public class LogEntryDialogFragment extends AppCompatDialogFragment {

    Employees crew = null;
    LogEntry currentLogEntry = null;

    public LogEntryDialogFragment(Employees crew, LogEntry currentLogEntry){
        this.crew = crew;
        this.currentLogEntry = currentLogEntry;
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

        LogEntry.Event[] allEvents = LogEntry.Event.values();
        List<LogEntry.Event> possibleEvents = LogEntry.getPossibleEvents(currentLogEntry == null ? LogEntry.State.IDLE : currentLogEntry.getState());
        ImageButton btn;
        for(LogEntry.Event event : allEvents){
            btn = (ImageButton)contentView.findViewById(getResources().getIdentifier(event.toString(),"id", getContext().getPackageName()));
            if(possibleEvents.contains(event)){
                btn.setEnabled(true);
                btn.setAlpha(1.0f);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        switch(event){
                            case RAISE_ANCHOR:
                            case DUTY_CHANGE:
                                contentView.findViewById(R.id.crew).setVisibility(View.VISIBLE);
                                break;

                            default:
                                contentView.findViewById(R.id.crew).setVisibility(View.GONE);
                                break;
                        }

                        Log.i("LogEntry", "Selected event: " + event);
                    }
                });
            } else {
                btn.setEnabled(false);
                btn.setAlpha(0.2f);
            }
        }

        for(Employee employee : crew){

        }

        //set the close button
        /*Button closeButton = contentView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });*/

        builder.setView(contentView).setTitle("Add or Edit");

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
