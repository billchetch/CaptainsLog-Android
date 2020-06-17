package net.chetch.captainslog;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.chetch.captainslog.data.LogEntry;
import net.chetch.utilities.Utils;
import net.chetch.webservices.employees.Employee;

import java.util.Calendar;

public class LogEntryFragment extends Fragment implements View.OnClickListener {

    public LogEntry logEntry;
    public Employee crewMember;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View contentView = inflater.inflate(R.layout.log_entry_fragment, container, false);

        ImageView iv = contentView.findViewById(R.id.crewProfileImage);
        iv.setImageBitmap(crewMember.profileImage);

        iv = contentView.findViewById(R.id.eventIcon);
        String resourceName = "ic_event_" + logEntry.getEvent().toString().toLowerCase() + "_white_24dp";
        int resource = getResources().getIdentifier(resourceName,"drawable", getContext().getPackageName());
        iv.setImageResource(resource);

        TextView tv = contentView.findViewById(R.id.knownAsAndEvent);
        tv.setText(crewMember.getKnownAs() + " " + logEntry.getEvent().toString());

        tv = contentView.findViewById(R.id.eventDetails);
        String dt = Utils.formatDate(logEntry.getCreated(), "dd/MM/yyyy HH:mm:ss Z");
        String latLon = logEntry.getLatitude() + ", " + logEntry.getLongitude();
        tv.setText("On " + dt + " @ " + latLon + " (lat/lon)");


        contentView.setOnClickListener(this);

        return contentView;
    }

    @Override
    public void onClick(View v) {
       Log.i("Log Entry Fragment", "Clicked");
    }
}
