package net.chetch.captainslog;

import android.graphics.Bitmap;
import android.media.Image;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.chetch.captainslog.data.CaptainsLogRepository;
import net.chetch.captainslog.data.CrewRepository;
import net.chetch.captainslog.data.LogEntry;
import net.chetch.utilities.Utils;
import net.chetch.webservices.DataObjectCollection;
import net.chetch.webservices.LiveDataCache;
import net.chetch.webservices.WebserviceRepository;
import net.chetch.webservices.employees.Employee;
import net.chetch.webservices.employees.Employees;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends GenericActivity {
    CaptainsLogRepository logRepository = CaptainsLogRepository.getInstance();
    CrewRepository crewRepository = CrewRepository.getInstance();
    Employees crew = null;
    LogEntry latestLogEntry = null;
    LogEntryDialogFragment logEntryDialog = null;
    HashMap<String, Image> crewProfilePics = new HashMap<>();
    Employees.FieldMap<String> eidMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        includeOptionsMenu();
        showProgress();
        try {
            //configure log repo
            String apiBaseURL = "http://192.168.43.123:8005/api";
            logRepository.setAPIBaseURL(apiBaseURL);
            logRepository.getError().observe(this, t->{
                showError(t);
            });

            //configure employee repo
            apiBaseURL = "http://192.168.43.123:8004/api";
            crewRepository.setAPIBaseURL(apiBaseURL);
            crewRepository.getError().observe(this, t->{
                showError(t);
            });

            //start data loading sequence
            crewRepository.getCrew().observe(this, c -> {
                //download profile images
                crew = c; //keep a record of crew
                if(c.size() == 0){
                    showError(0, "No crew");
                    return;
                }

                eidMap = crew.employeeIDMap();

                String baseURL = crewRepository.getAPIBaseURL() + "/resource/image/profile-pics/";
                HashMap<String, Employee> url2crew = new HashMap<>();
                for(Employee emp : crew) {
                    url2crew.put(baseURL + emp.getKnownAs() + ".jpg", emp);
                }

                Utils.downloadImages(url2crew.keySet()).observe(this, bms->{
                    Log.i("Main", "Images downloaded");

                    for(Map.Entry<String, Bitmap> entry : bms.entrySet()){
                        Employee emp = url2crew.get(entry.getKey());
                        emp.profileImage = entry.getValue();
                    }

                    logRepository.getLogEntriesFirstPage().observe(this, entries->{
                        //entries.sort("created", DataObjectCollection.SortOptions.DESC);

                        if(entries.size() > 0){
                            latestLogEntry = entries.get(0);
                            Employee emp = eidMap.get(entries.get(0).getEmployeeID());
                            ImageView iv = findViewById(R.id.profilePicCrewOnDuty);
                            iv.setImageBitmap(emp.profileImage);

                            //known as
                            TextView tv = findViewById(R.id.crewOnDutyKnownAs);
                            tv.setText(emp.getKnownAs());

                            //state
                            LogEntry.State state = latestLogEntry.getStateForAfterEvent();
                            int resource = getResourceID("log_entry.state." + state, "string");
                            tv = findViewById(R.id.state);
                            tv.setText(getString(resource));

                            tv = findViewById(R.id.latLon);
                            String latLon = "0.324333, -1234222";
                            tv.setText(latLon);
                        }

                        FragmentManager fragmentManager = getSupportFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        ((ViewGroup)findViewById(R.id.logLinearLayout)).removeAllViews();

                        for(LogEntry entry : entries){
                            LogEntryFragment lef = new LogEntryFragment();
                            lef.logEntry = entry;
                            if(eidMap.containsKey(entry.getEmployeeID())){
                                lef.crewMember = eidMap.get(entry.getEmployeeID());
                            }
                            fragmentTransaction.add(R.id.logLinearLayout, lef);
                        }
                        fragmentTransaction.commit();

                        Log.i("Main", "Retreived " + entries.size() + " entries");
                        hideProgress();
                        findViewById(R.id.bodyLinearLayout).setVisibility(View.VISIBLE);
                    });
                });

            });



        } catch (Exception e){
            Log.e("Main", e.getMessage());
        }
    }

    public void openLogEntry(View view){
        if(logEntryDialog != null){
            logEntryDialog.dismiss();
        }
        logEntryDialog = new LogEntryDialogFragment();
        logEntryDialog.crew = crew;
        logEntryDialog.latestLogEntry = latestLogEntry;

        logEntryDialog.show(getSupportFragmentManager(), "LogEntryDialog");
    }

    @Override
    public void onDialogPositiveClick(GenericDialogFragment dialog){
        if(dialog instanceof LogEntryConfirmationDialogFragment){
            LogEntry logEntry = ((LogEntryConfirmationDialogFragment)dialog).logEntry;
            logEntryDialog.dismiss();

            logRepository.addLogEntry(logEntry).observe(this, le -> {
                showProgress();
                logRepository.getLogEntriesFirstPage();
            });
        }
        Log.i("Main", "Saving log entry");
    }
}
