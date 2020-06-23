package net.chetch.captainslog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.chetch.captainslog.data.CaptainsLogRepository;
import net.chetch.captainslog.data.CrewRepository;
import net.chetch.captainslog.data.CrewStats;
import net.chetch.captainslog.data.LogEntry;
import net.chetch.utilities.Utils;
import net.chetch.webservices.DataObjectCollection;
import net.chetch.webservices.Webservice;
import net.chetch.webservices.WebserviceRepository;
import net.chetch.webservices.employees.Employee;
import net.chetch.webservices.employees.Employees;
import net.chetch.webservices.exceptions.WebserviceException;
import net.chetch.webservices.gps.GPSPosition;
import net.chetch.webservices.network.NetworkRepository;
import net.chetch.webservices.network.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends GenericActivity {
    static public final String CAPTAINS_LOG_SERVICE_NAME = "Captains Log";
    static public final String EMPLOYEES_SERVICE_NAME = "Employees";

    CaptainsLogRepository logRepository = CaptainsLogRepository.getInstance();
    CrewRepository crewRepository = CrewRepository.getInstance();
    Employees crew = null;
    Employee crewOnDuty = null;
    LogEntry latestLogEntry = null;
    LogEntryDialogFragment logEntryDialog = null;
    Employees.FieldMap<String> eidMap;
    CrewStats crewStats = null;
    GPSPosition latestGPS = null;
    Calendar startedDuty = null;
    Calendar warnOfExcessDuty = null;
    int excessDutyWarningCount = 0;
    boolean showOnDuty = false;
    boolean canLoadData = false;

    int onDutytMovingTimeLimit = 60 * 5; //in seconds
    ExcessOnDutyDialogFragment excessOnDutyDialog = null;

    //GPSRepository = GPSRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        includeOptionsMenu();
        startTimer(10);
        showProgress();

        if(!canLoadData){
            configureServices();
        }
    }

    protected void configureServices(){
        try {
            //configure log repo

            String apiBaseURL = "http://192.168.43.123:8002/api";
            NetworkRepository networkRepository = NetworkRepository.getInstance();
            networkRepository.setAPIBaseURL(apiBaseURL);
            networkRepository.getError().observe(this, t->{
                showError(t);
            });

            networkRepository.getServices().observe(this, services ->{
                try {
                    Service service = services.getService(CAPTAINS_LOG_SERVICE_NAME);
                    if (service != null) {
                        logRepository.setAPIBaseURL(service.getLocalEndpoint());
                        logRepository.synchronise(networkRepository);
                        logRepository.getError().observe(this, t -> {
                            showError(t);
                        });
                    } else {
                        throw new Exception("Could not find service " + CAPTAINS_LOG_SERVICE_NAME);
                    }

                    //configure employee repo
                    service = services.getService(EMPLOYEES_SERVICE_NAME);
                    if(service != null) {
                        crewRepository.setAPIBaseURL(service.getLocalEndpoint());
                        crewRepository.synchronise(networkRepository);
                        crewRepository.getError().observe(this, t -> {
                            showError(t);
                        });
                    } else {
                        throw new Exception("Could not find service " + EMPLOYEES_SERVICE_NAME);
                    }
                    canLoadData = true;
                    loadData();

                    Log.i("Main", "Services loaded");
                } catch (Exception e){
                    showError(e);
                }

            });

        } catch (Exception e){
            showError(e);
        }
    }

    protected void loadData(){
        if(!canLoadData)return;

        try {
            showProgress();

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
                    url2crew.put(baseURL + emp.getKnownAs().replace(' ', '_') + ".jpg", emp);
                }

                Utils.downloadImages(url2crew.keySet()).observe(this, bms->{
                    Log.i("Main", "Images downloaded");

                    for(Map.Entry<String, Bitmap> entry : bms.entrySet()){
                        Employee emp = url2crew.get(entry.getKey());
                        emp.profileImage = entry.getValue();
                    }

                    logRepository.getLogEntriesFirstPage().observe(this, entries->{
                        entries.sort("created", DataObjectCollection.SortOptions.DESC);
                        if(entries.size() > 0){
                            setLatestLogEntry(entries.get(0));
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
            showError(e);
            Log.e("Main", e.getMessage());
        }
    }

    @Override
    protected void onTimer(){

        if(startedDuty != null && showOnDuty && crewOnDuty != null && warnOfExcessDuty != null && (excessOnDutyDialog == null || !excessOnDutyDialog.isShowing())) {
            Calendar now = Calendar.getInstance();
            if(now.getTimeInMillis() > warnOfExcessDuty.getTimeInMillis()){
                openExcessOnDuty();
                excessDutyWarningCount++;
                double coeff = Math.min(excessDutyWarningCount*0.2, 0.5);
                warnOfExcessDuty.setTimeInMillis(now.getTimeInMillis() + (long)(coeff*onDutytMovingTimeLimit*1000));
            }
        }

        updateOnDuty(showOnDuty);
    }

    protected void setLatestLogEntry(LogEntry logEntry){
        if(latestLogEntry != null && latestLogEntry.getID() == logEntry.getID())return;

        Employee emp = eidMap.get(logEntry.getEmployeeID());

        ImageView iv = findViewById(R.id.profilePicCrewOnDuty);
        iv.setImageBitmap(emp.profileImage);

        //known as
        TextView tv = findViewById(R.id.crewOnDutyKnownAs);
        tv.setText(emp.getKnownAs());

        //state
        LogEntry.State state = logEntry.getStateForAfterEvent();
        int resource = getResourceID("log_entry.state." + state, "string");
        tv = findViewById(R.id.state);
        tv.setText(getString(resource));

        //position lat/lon
        tv = findViewById(R.id.latLon);
        String latLon = "0.324333, -1234222";
        tv.setText(latLon);

        //change of on duty
        boolean dutyChange = crewOnDuty == null || (crewOnDuty.getID() != emp.getID());
        if(dutyChange || logEntry.isStateChange()){
            crewOnDuty = emp;
            startedDuty = null;
            excessDutyWarningCount = 0;
            warnOfExcessDuty = null;
        }
        latestLogEntry = logEntry;

        updateOnDuty(false);
        logRepository.getCrewStats().observe(this, stats->{
            crewStats = stats; //keep a record
            String eid = latestLogEntry.getEmployeeID();
            if(startedDuty == null) {
                startedDuty = crewStats.getStartedDuty(eid);
                if(latestLogEntry.getStateForAfterEvent() == LogEntry.State.MOVING) {

                    //set the time for the excess duty wakeup
                    long millis = startedDuty.getTimeInMillis() + (long)(1.1*onDutytMovingTimeLimit*1000); //10% extra
                    warnOfExcessDuty = Calendar.getInstance();
                    warnOfExcessDuty.setTimeInMillis(millis);

                    //create an app wakeup
                    Context ctx = getApplicationContext();
                    Intent intent = new Intent(ctx, MainActivity.class);
                    intent.putExtra("wakeup", true);
                    PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    AlarmManager mgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                    mgr.cancel(pi);    // Cancel any previously-scheduled wakeups
                    mgr.set(AlarmManager.RTC_WAKEUP, warnOfExcessDuty.getTimeInMillis(), pi);

                }
            }


            //update on duty
            updateOnDuty(true);
        });
    }

    private double getDutyCompletion(){
        Calendar now = logRepository.getServerTime();
        long secondsTotal = Utils.dateDiff(now, startedDuty, TimeUnit.SECONDS);
        return (double)secondsTotal / (double)onDutytMovingTimeLimit;
    }

    public void updateOnDuty(boolean show){
        if(latestLogEntry == null)return;

        showOnDuty = show;
        ImageView progressBar = findViewById(R.id.progressOnDutyBar);
        TextView progressInfo = findViewById(R.id.progressOnDutyInfo);
        if(!show){
            progressInfo.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
            return;
        }


        switch(latestLogEntry.getStateForAfterEvent()){
            case MOVING:
                Calendar now = logRepository.getServerTime(); //Calendar.getInstance();
                long hours  = Utils.hoursDiff(now, startedDuty);
                long minutes = Utils.dateDiff(now, startedDuty, TimeUnit.MINUTES) - 60*hours;
                Log.i("Main", "Now: " + Utils.formatDate(now, Webservice.DEFAULT_DATE_FORMAT) + ", Started: " + Utils.formatDate(startedDuty, Webservice.DEFAULT_DATE_FORMAT));
                double dutyCompletion = getDutyCompletion();
                int percentage = (int)(100*dutyCompletion);
                int age = Math.min((int)Math.floor(dutyCompletion * 5), 5);

                String s = (hours > 0 ? hours + "h " : "") + minutes + "m";
                s += " / " + percentage + "%";
                progressInfo.setText(s);
                progressInfo.setVisibility(View.VISIBLE);

                //progress bar width
                ImageView progressBorder = findViewById(R.id.progressOnDutyBorder);
                int borderWidth = progressBorder.getWidth();
                progressBar.getLayoutParams().width = (int)(borderWidth*Math.min(dutyCompletion, 1.0));

                //progress bar color
                try {
                    progressBar.setBackgroundColor(getColorResource("age" + age));
                } catch (Exception e){
                    Log.e("Main", e.getMessage() + " for color " + age);
                }

                progressBar.setVisibility(View.VISIBLE);
                progressBar.requestLayout();

                break;

            case IDLE:
                progressBar.setVisibility(View.INVISIBLE);
                progressInfo.setVisibility(View.INVISIBLE);
                break;
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

    public void openExcessOnDuty(){
        if(excessOnDutyDialog != null){
            excessOnDutyDialog.dismiss();
        }
        excessOnDutyDialog = new ExcessOnDutyDialogFragment();
        excessOnDutyDialog.latestLogEntry = latestLogEntry;

        excessOnDutyDialog.show(getSupportFragmentManager(), "ExcessOnDutyDialog");
    }

    @Override
    public void onDialogPositiveClick(GenericDialogFragment dialog){
        LogEntry logEntry = null;
        if(dialog instanceof LogEntryConfirmationDialogFragment){
            logEntry = ((LogEntryConfirmationDialogFragment)dialog).logEntry;
            logEntryDialog.dismiss();

            Log.i("Main", "Saving log entry");
        } else if(dialog instanceof ExcessOnDutyDialogFragment){
            logEntry = ((ExcessOnDutyDialogFragment)dialog).logEntry;

            Log.i("Main", "Saving xs on duty reason");
        } else if(dialog instanceof ErrorDialogFragment){
            Throwable t = ((ErrorDialogFragment)dialog).throwable;
            if(t instanceof WebserviceException && !((WebserviceException)t).isServiceAvailable()){
                loadData();
            }
            Log.i("Main", "Error");
        }

        if(logEntry != null){
            logRepository.saveLogEntry(logEntry).observe(this, le -> {
                showProgress();
                setLatestLogEntry(le);
                logRepository.getLogEntriesFirstPage();
            });
        }

    }
}
