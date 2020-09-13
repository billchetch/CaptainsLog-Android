package net.chetch.captainslog;

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.chetch.captainslog.data.CrewMember;
import net.chetch.captainslog.data.LogEntry;
import net.chetch.utilities.Utils;
import net.chetch.utilities.Animation;

//NOTE: this class is a bit strange as it was originally for a fragment and then adapted
//for use with RecyclerView.Adapter class LogEntriesAdapter, specifically so as to work with the ViewHolder class

public class LogEntryFragment extends Fragment implements View.OnClickListener {

    public LogEntry logEntry;
    public CrewMember crewMember;
    public MainActivity mainActivity;
    public boolean populateOnCreate = true;
    private View contentView;

    private MainActivity getMainActivity(){
        if(mainActivity == null){
            mainActivity = ((MainActivity) getActivity());
        }
        return mainActivity;
    }

    protected int getResourceID(String resourceName, String resourceType){
        return getMainActivity().getResources().getIdentifier(resourceName,resourceType, getMainActivity().getBaseContext().getPackageName());
    }


    private String getKnownAsAndEvent(){
        int resource = getResourceID("log_entry.event." + logEntry.getEvent(), "string");
        String eventString = getMainActivity().getString(resource);
        String mark = logEntry.requiresRevision() ? "* " : "";
        return mark + crewMember.getKnownAs() + " " + eventString.toLowerCase();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        contentView = inflater.inflate(R.layout.log_entry_fragment, container, false);
        if(populateOnCreate){
            populateContent();
        }

        return contentView;
    }

    public void populateContent(){
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

                    String message = getMainActivity().getString(R.string.dialog_confirmation_markforrevision_text);
                    getMainActivity().showConfirmationDialog(message, listener);
                }
                return false;
            }

        });
    }

    private void markForRevision(){
        logEntry.setRequiresRevision(!logEntry.requiresRevision());
        try {
            getMainActivity().model.saveLogEntry(logEntry, null).observe(entry -> {
                TextView tv = contentView.findViewById(R.id.knownAs);
                tv.setText(getKnownAsAndEvent());
                logEntry.read(entry);
                Log.i("LEF", "Marked entry ");
            });
        } catch (Exception e){
           getMainActivity().showError(e);
        }
    }

    @Override
    public void onClick(View v) {

    }

    public void flash(){
        int colour = contentView.getResources().getColor(R.color.bluegreen);
        Animation.flashBackground(contentView, colour, 3500, 0);
    }
}
