package net.chetch.captainslog;

import android.graphics.Bitmap;
import android.media.Image;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import net.chetch.captainslog.data.CaptainsLogRepository;
import net.chetch.captainslog.data.CrewRepository;
import net.chetch.captainslog.data.LogEntry;
import net.chetch.utilities.Utils;
import net.chetch.webservices.LiveDataCache;
import net.chetch.webservices.WebserviceRepository;
import net.chetch.webservices.employees.Employee;
import net.chetch.webservices.employees.Employees;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends GenericActivity {
    CaptainsLogRepository logRepository = new CaptainsLogRepository();
    CrewRepository crewRepository = new CrewRepository();
    Employees crew = null;
    LogEntry currentLogEntry = null;
    HashMap<String, Image> crewProfilePics = new HashMap<>();

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
                        //now prepare UI

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
        LogEntryDialogFragment dialog = new LogEntryDialogFragment();
        dialog.crew = crew;
        dialog.currentLogEntry = currentLogEntry;

        dialog.show(getSupportFragmentManager(), "LogEntryDialog");
    }
}
