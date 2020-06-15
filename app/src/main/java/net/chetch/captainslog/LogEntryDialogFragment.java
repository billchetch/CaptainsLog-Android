package net.chetch.captainslog;

import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import net.chetch.captainslog.data.LogEntry;
import net.chetch.utilities.Logger;
import net.chetch.webservices.employees.Employee;
import net.chetch.webservices.employees.Employees;

import java.util.ArrayList;
import java.util.List;


public class LogEntryDialogFragment extends AppCompatDialogFragment implements View.OnClickListener{

    View contentView;
    List<View> eventButtons = new ArrayList<>();
    View selectedEventButton = null;

    public Employees crew = null;
    public LogEntry currentLogEntry = null;
    LogEntry logEntry = new LogEntry();

    public LogEntryDialogFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("LED", "Created");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Employees activeCrew = crew.active();
        for(Employee employee : activeCrew){
            CrewFragment cf = new CrewFragment();
            cf.crew = employee;
            fragmentTransaction.add(R.id.logEntryDialogCrewList, cf);
        }
        fragmentTransaction.commit();

        Log.i("LED", "View Created");
        return contentView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        //get the content view
        contentView = inflater.inflate(R.layout.log_entry_dialog, null);

        LogEntry.Event[] allEvents = LogEntry.Event.values();
        List<LogEntry.Event> possibleEvents = LogEntry.getPossibleEvents(currentLogEntry == null ? LogEntry.State.IDLE : currentLogEntry.getState());
        ImageButton btn;
        for(LogEntry.Event event : allEvents){
            btn = contentView.findViewById(getResources().getIdentifier(event.toString(),"id", getContext().getPackageName()));
            if(possibleEvents.contains(event)){
                btn.setEnabled(true);
                btn.setAlpha(1.0f);
                btn.setOnClickListener(this);
            } else {
                btn.setEnabled(false);
                btn.setAlpha(0.2f);
            }
            btn.setTag(event);
            eventButtons.add(btn);
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

        // Create the Dialog object and return it
        Dialog dialog = builder.create();

        Log.i("LED", "Dialog created");

        return dialog;
    }


    @Override
    public void onClick(View view) {
        if(eventButtons.contains(view) && selectedEventButton != view){
            LogEntry.Event event = (LogEntry.Event)view.getTag();
            logEntry.setEvent(event, currentLogEntry == null ? LogEntry.State.IDLE : currentLogEntry.getState());

            Log.i("LED", "Handling on click " + event);

            view.setBackgroundResource(R.drawable.image_background_rect_selected);
            if(selectedEventButton != null)selectedEventButton.setBackgroundResource(R.drawable.image_background_rect);
            selectedEventButton = view;
        }
    }
}
