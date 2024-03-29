package net.chetch.captainslog;

import android.graphics.Color;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
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

        Log.i("CrewFragment", "Creating fragment view for " + crewMember.getFullName());


        contentView.setOnClickListener((View.OnClickListener)getParentFragment());
        contentView.setTag(crewMember);

        return contentView;
    }

    public void select(boolean selected){
        View view = getView();

        int selectedColour = ContextCompat.getColor(getActivity(), net.chetch.appresources.R.color.bluegreen2);
        view.setBackgroundColor(selected ? selectedColour : Color.TRANSPARENT);

        Log.i("CF", "Selected: " + selected + " " + crewMember.getKnownAs());
    }
}
