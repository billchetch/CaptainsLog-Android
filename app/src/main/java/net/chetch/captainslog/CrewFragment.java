package net.chetch.captainslog;

import android.content.DialogInterface;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.chetch.webservices.employees.Employee;

public class CrewFragment extends Fragment{

    public Employee crewMember;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View contentView =  inflater.inflate(R.layout.crew_fragment, container, false);

        ImageView iv = contentView.findViewById(R.id.crewProfilePic);
        iv.setImageBitmap(crewMember.profileImage);

        TextView tv = contentView.findViewById(R.id.crewKnownAs);
        tv.setText(crewMember.getKnownAs());

        Log.i("CrewFragment", "Creating fragment view");


        contentView.setOnClickListener((View.OnClickListener)getParentFragment());
        contentView.setTag(crewMember);

        return contentView;
    }

    public void select(boolean selected){
        View view = getView();

        int selectedColour = ContextCompat.getColor(getActivity(), R.color.bluegreen);
        view.setBackgroundColor(selected ? selectedColour : Color.TRANSPARENT);

        Log.i("CF", "Selected: " + selected + " " + crewMember.getKnownAs());
    }
}
