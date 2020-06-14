package net.chetch.captainslog;

import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import net.chetch.captainslog.data.CaptainsLogRepository;
import net.chetch.captainslog.data.CrewRepository;
import net.chetch.utilities.Utils;
import net.chetch.webservices.LiveDataCache;
import net.chetch.webservices.WebserviceRepository;
import net.chetch.webservices.employees.Employee;
import net.chetch.webservices.employees.Employees;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends GenericActivity {
    CaptainsLogRepository logRepository = new CaptainsLogRepository();
    CrewRepository crewRepository = new CrewRepository();
    Employees crew = null;

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
                List<String>  imageURLs = new ArrayList<>();
                for(Employee emp : crew) {
                    imageURLs.add(baseURL + emp.getKnownAs() + ".jpg");
                }
                Utils.downloadImages(imageURLs).observe(this, bms->{
                    Log.i("Main", "Images downloaded");
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
        boolean newEntry = view.getId() == R.id.addLogEntry;

        DialogFragment dialog = new LogEntryDialogFragment(crew);
        dialog.show(getSupportFragmentManager(), "LogEntryDialog");
    }
}
