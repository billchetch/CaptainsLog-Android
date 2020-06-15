package net.chetch.captainslog;

import android.media.Image;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.chetch.webservices.employees.Employee;

public class CrewFragment extends Fragment {

    public Employee crew;
    public Image profilePic;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View contentView =  inflater.inflate(R.layout.crew_fragment, container, false);

        ImageView iv = contentView.findViewById(R.id.crewProfilePic);
        iv.setImageBitmap(crew.profileImage);

        Log.i("CrewFragment", "Creating fragment view");

        return contentView;
    }
}
