package net.chetch.captainslog;

import android.content.DialogInterface;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.chetch.captainslog.data.CaptainsLogRepository;
import net.chetch.captainslog.data.LogEntry;
import net.chetch.utilities.Utils;
import net.chetch.webservices.employees.Employee;

public class LogEntryFragment extends Fragment implements View.OnClickListener {

    public LogEntry logEntry;
    public Employee crewMember;

    protected int getResourceID(String resourceName, String resourceType){
        return getResources().getIdentifier(resourceName,resourceType, getContext().getPackageName());
    }


    private String getKnownAsAndEvent(){
        int resource = getResourceID("log_entry.event." + logEntry.getEvent(), "string");
        String eventString = getString(resource);
        String mark = logEntry.requiresRevision() ? "* " : "";
        return mark + crewMember.getKnownAs() + " " + eventString.toLowerCase();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View contentView = inflater.inflate(R.layout.log_entry_fragment, container, false);

        ImageView iv = contentView.findViewById(R.id.crewProfileImage);
        iv.setImageBitmap(crewMember.profileImage);

        iv = contentView.findViewById(R.id.eventIcon);
        String resourceName = "ic_event_" + logEntry.getEvent().toString().toLowerCase() + "_white_24dp";
        int resource = getResourceID(resourceName, "drawable");
        iv.setImageResource(resource);

        TextView tv = contentView.findViewById(R.id.knownAs);
        tv.setText(getKnownAsAndEvent());

        tv = contentView.findViewById(R.id.logEntryDate);
        String dt = Utils.formatDate(logEntry.getCreated(), "dd/MM/yyyy HH:mm:ss Z");
        tv.setText(dt);

        String lat = String.format("%.5f", logEntry.getLatitude());
        String lon = String.format("%.5f", logEntry.getLongitude());
        String latLon = lat + "/" + lon;
        tv = contentView.findViewById(R.id.logEntryLatLon);
        tv.setText(latLon);

        String comment = logEntry.getComment();
        tv = contentView.findViewById(R.id.comment);
        if(comment != null && comment.length() > 0){
            tv.setVisibility(View.VISIBLE);
            tv.setText(comment);
        } else {
            tv.setVisibility(View.GONE);
        }


        contentView.setOnClickListener(this);
        contentView.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View view) {

                if(logEntry.requiresRevision()){
                    markForRevision();
                } else {
                    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            markForRevision();
                        }
                    };

                    String message = getString(R.string.dialog_confirmation_markforrevision_text);
                    ((GenericActivity) getActivity()).showConfirmationDialog(message, listener);
                }
                return false;
            }

        });

        return contentView;
    }

    private void markForRevision(){
        logEntry.setRequiresRevision(!logEntry.requiresRevision());
        try {
            ((MainActivity) getActivity()).model.saveLogEntry(logEntry, null).observe(entry -> {
                ((TextView) getView().findViewById(R.id.knownAs)).setText(getKnownAsAndEvent());
                logEntry.read(entry);
                Log.i("LEF", "Marked entry ");
            });
        } catch (Exception e){
            ((MainActivity) getActivity()).showError(e);
        }
    }

    @Override
    public void onClick(View v) {

    }
}
