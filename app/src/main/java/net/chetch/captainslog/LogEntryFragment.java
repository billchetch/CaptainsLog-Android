package net.chetch.captainslog;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class LogEntryFragment extends Fragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.crew_fragment, container, false);

        rootView.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
       Log.i("Log Entry Fragment", "Clicked");
    }
}
