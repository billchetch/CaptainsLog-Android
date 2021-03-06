package net.chetch.captainslog;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import net.chetch.appframework.GenericDialogFragment;
import net.chetch.captainslog.data.Crew;
import net.chetch.captainslog.data.CrewMember;
import net.chetch.captainslog.data.LogEntry;
import net.chetch.webservices.employees.Employee;

import java.util.ArrayList;
import java.util.List;


public class LogEntryDialogFragment extends GenericDialogFragment implements View.OnClickListener{

    List<View> eventButtons = new ArrayList<>();
    View selectedEventButton = null;
    List<CrewFragment> crewFragments = new ArrayList<>();

    public Crew crew = null;
    public LogEntry latestLogEntry = null;
    public LogEntry logEntry = new LogEntry();
    public List<LogEntry.Event> restrictToEvents = null;
    public LogEntry.Event selectEventOnOpen = null;

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

        if(selectEventOnOpen != null){
            Runnable runnbale = new Runnable()
            {
                @Override
                public void run()
                {
                    ImageButton btn = contentView.findViewById(getResourceID(selectEventOnOpen.toString(),"id"));
                    btn.callOnClick();
                }
            };

            Handler handler = new Handler();
            handler.postDelayed(runnbale, 100);//Message will be delivered in 1 second.
        }

        return contentView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        inflateContentView(R.layout.log_entry_dialog);

        //add event buttons
        LogEntry.Event[] allEvents = LogEntry.Event.values();
        LogEntry.State currentState = latestLogEntry == null ? LogEntry.State.IDLE :  latestLogEntry.getStateForAfterEvent();
        List<LogEntry.Event> possibleEvents = LogEntry.getPossibleEvents(currentState);
        ImageButton btn;
        for(LogEntry.Event event : allEvents){

            btn = contentView.findViewById(getResources().getIdentifier(event.toString(),"id", getContext().getPackageName()));
            if(btn == null)continue;

            if(possibleEvents.contains(event) && (restrictToEvents == null || restrictToEvents.contains(event))){
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
        contentView.findViewById(R.id.logEntryComment).setVisibility(View.GONE);


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

        Dialog dialog = createDialog();

        Log.i("LED", "Dialog created");

        return dialog;
    }

    private void populateCrewView(Crew crew){
        ((ViewGroup)contentView.findViewById(R.id.logEntryDialogCrewList)).removeAllViews();

        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        for(CrewMember cm : crew){
            CrewFragment cf = new CrewFragment();
            cf.crewMember = cm;
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
                        populateCrewView(crew.<Crew>active().exclude("employee_id", latestLogEntry.getEmployeeID()));
                    } else {
                        populateCrewView(crew.active());
                    }
                    contentView.findViewById(R.id.logEntryDialogCrewListContainer).setVisibility(View.VISIBLE);
                    contentView.findViewById(R.id.logEntryComment).clearFocus();
                    contentView.findViewById(R.id.logEntryComment).setVisibility(View.GONE);
                    logEntry.setEmployeeID(null); //set to null as it needs to be chosen later
                    break;

                default:
                    logEntry.setEmployeeID(latestLogEntry == null ? null : latestLogEntry.getEmployeeID());
                    if(event == LogEntry.Event.COMMENT){
                        contentView.findViewById(R.id.logEntryComment).setVisibility(View.VISIBLE);
                        contentView.findViewById(R.id.logEntryComment).requestFocus();
                    } else {
                        contentView.findViewById(R.id.logEntryComment).setVisibility(View.GONE);
                    }
                    contentView.findViewById(R.id.logEntryDialogCrewListContainer).setVisibility(View.GONE);
                    break;
            }

            int resource = getResourceID("button.log_entry.event." + event.toString(), "string");
            String eventString = getString(resource);
            ((TextView)contentView.findViewById(R.id.logEntryEventSelected)).setText(eventString.toUpperCase());

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
            dialogManager.showWarningDialog(getString(R.string.dialog_warning_choose_event));
        } else if(logEntry.getEmployeeID() == null){
            dialogManager.showWarningDialog(getString(R.string.dialog_warning_choose_crew));
        } else {
            String comment = null;
            if(logEntry.getEvent() == LogEntry.Event.COMMENT) {
                TextView tv = contentView.findViewById(R.id.logEntryComment);
                comment = tv.getText() == null ? null : tv.getText().toString().trim();
                if (comment == null || comment.length() == 0) {
                    dialogManager.showWarningDialog(getString(R.string.dialog_warning_add_comment));
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
