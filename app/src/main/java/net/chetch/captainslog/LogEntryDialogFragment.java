package net.chetch.captainslog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import net.chetch.captainslog.data.LogEntry;
import net.chetch.utilities.Logger;
import net.chetch.webservices.employees.Employee;
import net.chetch.webservices.employees.Employees;

import java.util.ArrayList;
import java.util.List;


public class LogEntryDialogFragment extends GenericDialogFragment implements View.OnClickListener{

    List<View> eventButtons = new ArrayList<>();
    View selectedEventButton = null;
    List<CrewFragment> crewFragments = new ArrayList<>();

    public Employees crew = null;
    public LogEntry latestLogEntry = null;
    public LogEntry logEntry = new LogEntry();

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

        //add event buttons
        LogEntry.Event[] allEvents = LogEntry.Event.values();
        LogEntry.State currentState = latestLogEntry == null ? LogEntry.State.IDLE :  latestLogEntry.getStateForAfterEvent();
        List<LogEntry.Event> possibleEvents = LogEntry.getPossibleEvents(currentState);
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
        ((TextView)contentView.findViewById(R.id.logEntryEventSelected)).setText("");

        //hide other sections
        contentView.findViewById(R.id.logEntryDialogCrewListContainer).setVisibility(View.GONE);
        contentView.findViewById(R.id.logEntryCommentContainer).setVisibility(View.GONE);


        //set the close button
        Button cancelButton = contentView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        //set the close button
        Button actionButton = contentView.findViewById(R.id.actionButton);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openLogEntryConfirmation();
            }
        });

        builder.setView(contentView);

        // Create the Dialog object and return it
        Dialog dialog = builder.create();
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Log.i("LED", "Dialog created");

        return dialog;
    }

    private void populateCrewView(Employees crew){
        ((ViewGroup)contentView.findViewById(R.id.logEntryDialogCrewList)).removeAllViews();

        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        for(Employee employee : crew){
            CrewFragment cf = new CrewFragment();
            cf.crewMember = employee;
            fragmentTransaction.add(R.id.logEntryDialogCrewList, cf);
            crewFragments.add(cf);
        }
        fragmentTransaction.commit();
    }

    @Override
    public void onClick(View view) {
        if(eventButtons.contains(view) && selectedEventButton != view){
            LogEntry.Event event = (LogEntry.Event)view.getTag();
            switch(event){
                case RAISE_ANCHOR:
                case DUTY_CHANGE:
                    if(event == LogEntry.Event.DUTY_CHANGE && latestLogEntry != null) {
                        populateCrewView(crew.active().exclude("employee_id", latestLogEntry.getEmployeeID()));
                    } else {
                        populateCrewView(crew.active());
                    }
                    contentView.findViewById(R.id.logEntryDialogCrewListContainer).setVisibility(View.VISIBLE);
                    contentView.findViewById(R.id.logEntryCommentContainer).setVisibility(View.GONE);
                    logEntry.setEmployeeID(null);
                    break;

                default:
                    logEntry.setEmployeeID(latestLogEntry == null ? null : latestLogEntry.getEmployeeID());
                    if(event == LogEntry.Event.COMMENT){
                        contentView.findViewById(R.id.logEntryCommentContainer).setVisibility(View.VISIBLE);
                    } else {
                        contentView.findViewById(R.id.logEntryCommentContainer).setVisibility(View.GONE);
                    }
                    contentView.findViewById(R.id.logEntryDialogCrewListContainer).setVisibility(View.GONE);
                    break;
            }


            ((TextView)contentView.findViewById(R.id.logEntryEventSelected)).setText(event.toString());

            LogEntry.State state = latestLogEntry == null ? LogEntry.State.IDLE : latestLogEntry.getStateForAfterEvent();
            logEntry.setEvent(event, state);
            view.setBackgroundResource(R.drawable.image_background_rect_selected);
            if(selectedEventButton != null)selectedEventButton.setBackgroundResource(R.drawable.image_background_rect);
            selectedEventButton = view;
        } else if(view.getTag() instanceof Employee){
            Employee selectedCrewMember = (Employee)view.getTag();
            logEntry.setEmployeeID(selectedCrewMember.getEmployeeID());
            for(CrewFragment cf : crewFragments){
                cf.select(cf.crewMember.getID() == selectedCrewMember.getID());
            }
        }
    }



    public void openLogEntryConfirmation(){
        if(logEntry.getEvent() == null || logEntry.getState() == null){
            dialogManager.showWarningDialog("Please choose an event");
        } else if(logEntry.getEmployeeID() == null){
            dialogManager.showWarningDialog("Please choose a crew member");
        } else {
            String comment = null;
            if(logEntry.getEvent() == LogEntry.Event.COMMENT) {
                TextView tv = contentView.findViewById(R.id.logEntryComment);
                comment = tv.getText() == null ? null : tv.getText().toString().trim();
                if (comment == null || comment.length() == 0) {
                    dialogManager.showWarningDialog("Please add a comment");
                    return;
                }
            }
            logEntry.setComment(comment);

            LogEntryConfirmationDialogFragment dialog = new LogEntryConfirmationDialogFragment();
            dialog.crewMember = crew.get("employee_id", logEntry.getEmployeeID());
            dialog.logEntry = logEntry;

            dialog.show(getChildFragmentManager(), "LogEntryConfirmationDialog");

        }

    }
}
