package net.chetch.captainslog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import net.chetch.captainslog.data.CaptainsLogRepository;
import net.chetch.captainslog.data.CrewMember;
import net.chetch.captainslog.data.CrewRepository;
import net.chetch.captainslog.data.CrewStats;
import net.chetch.captainslog.data.LogEntry;
import net.chetch.captainslog.models.MainViewModel;
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
import net.chetch.webservices.network.Services;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends GenericActivity {
    MainViewModel model;
    LogEntryDialogFragment logEntryDialog = null;
    ExcessOnDutyDialogFragment excessOnDutyDialog = null;

    GPSPosition latestGPS = null;

    Calendar lastXSDutyWarning = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);*/

        //this should be moved to application or in preferences or something
        try {
            String apiBaseURL = "http://192.168.43.123:8002/api/";
            NetworkRepository.getInstance().setAPIBaseURL(apiBaseURL);
        } catch (Exception e) {
            Log.e("MVM", e.getMessage());
            return;
        }


        includeOptionsMenu();
        //startTimer(10);
        showProgress();

        model = ViewModelProviders.of(this).get(MainViewModel.class);
        model.getError().observe(this, t ->{
            showError(t);
            Log.e("Main", "Error: " + t.getMessage());
        });

        model.loadData(data->{
            hideProgress();
            findViewById(R.id.bodyLinearLayout).setVisibility(View.VISIBLE);
            Log.i("Main","Model data has loaded");
        });

        //set up data responsive UI
        model.getEntriesFirstPage().observe(this, entries->{

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            ((ViewGroup)findViewById(R.id.logLinearLayout)).removeAllViews();

            for(LogEntry entry : entries){
                LogEntryFragment lef = new LogEntryFragment();
                lef.logEntry = entry;
                lef.crewMember = model.getCrewMember(entry.getEmployeeID());
                fragmentTransaction.add(R.id.logLinearLayout, lef);
            }
            fragmentTransaction.commit();

            Log.i("Main","First page of entries displayed");
            hideProgress();
        });

        model.getCrewMemberOnDuty().observe(this, crewMember -> {

            LogEntry logEntry = model.getLatestLogEntry();

            ImageView iv = findViewById(R.id.profilePicCrewOnDuty);
            iv.setImageBitmap(crewMember.profileImage);

            //known as
            TextView tv = findViewById(R.id.crewOnDutyKnownAs);
            tv.setText(crewMember.getKnownAs());

            //state
            LogEntry.State state = crewMember.getLastState();
            int resource = getResourceID("log_entry.state." + state, "string");
            tv = findViewById(R.id.state);
            tv.setText(getString(resource));

            //position lat/lon
            tv = findViewById(R.id.latLon);
            String latLon = logEntry.getLatitude() + "," + logEntry.getLongitude();
            tv.setText(latLon);

            updateOnDuty(true);

            Log.i("Main", "Displayed crew member on duty");
        });

        Log.i("Main", "onCreate");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getBooleanExtra("wakeup", false)) {
            // We were woken up by the alarm manager, but were already running

        }
        Log.i("Main", "onNewIntent");
    }


    @Override
    protected void onTimer(){

        CrewMember crewMemberOnDuty = model.getCrewMemberOnDuty().getValue();
        if(crewMemberOnDuty != null) {
            Calendar cal = crewMemberOnDuty.getPrevXSOnDutyWarning();
            boolean requiresWarning = cal != null && (lastXSDutyWarning == null || lastXSDutyWarning.getTimeInMillis() < cal.getTimeInMillis());
            if(requiresWarning && (excessOnDutyDialog == null || !excessOnDutyDialog.isShowing())) {
                openExcessOnDuty();
                Log.i("Main", "Now: " + Utils.formatDate(Calendar.getInstance(), Webservice.DEFAULT_DATE_FORMAT) + ", prev warn: " + Utils.formatDate(cal, Webservice.DEFAULT_DATE_FORMAT));
            }
        }
    }


    protected void setWakeUp(Calendar wakeUp){

        //create an app wakeup
        Context ctx = getApplicationContext();
        Intent intent = new Intent(ctx, this.getClass());
        intent.putExtra("wakeup", true);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager mgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(pi);    // Cancel any previously-scheduled wakeups
        mgr.set(AlarmManager.RTC_WAKEUP, wakeUp.getTimeInMillis(), pi);

    }

    public void updateOnDuty(boolean show){
        CrewMember crewMemberOnDuty = model.getCrewMemberOnDuty().getValue();
        if(crewMemberOnDuty == null)return;

        ImageView progressBar = findViewById(R.id.progressOnDutyBar);
        TextView progressInfo = findViewById(R.id.progressOnDutyInfo);
        if(!show){
            progressInfo.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
            return;
        }


        switch(crewMemberOnDuty.getLastState()){
            case MOVING:
                //do some calculations
                long hours  = crewMemberOnDuty.getOnDutyHours();
                long minutes = crewMemberOnDuty.getOnDutyMinutes();
                double dutyCompletion = crewMemberOnDuty.getOnDutyCompletion();
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
        Calendar momentLater = Calendar.getInstance();
        momentLater.setTimeInMillis(momentLater.getTimeInMillis() + 5000);
        setWakeUp(momentLater);

        /*if(logEntryDialog != null){
            logEntryDialog.dismiss();
        }
        logEntryDialog = new LogEntryDialogFragment();
        logEntryDialog.crew = model.getCrew().getValue();
        logEntryDialog.latestLogEntry = model.getLatestLogEntry();

        logEntryDialog.show(getSupportFragmentManager(), "LogEntryDialog");*/
    }

    public void openExcessOnDuty(){
        if(excessOnDutyDialog != null){
            excessOnDutyDialog.dismiss();
        }
        excessOnDutyDialog = new ExcessOnDutyDialogFragment();
        excessOnDutyDialog.show(getSupportFragmentManager(), "ExcessOnDutyDialog");
    }

    @Override
    public void onDialogPositiveClick(GenericDialogFragment dialog){
        if(dialog instanceof LogEntryConfirmationDialogFragment){
            LogEntry logEntry = ((LogEntryConfirmationDialogFragment)dialog).logEntry;
            logEntryDialog.dismiss();
            model.saveLogEntry(logEntry);
            showProgress();

            Log.i("Main", "Saving log entry");
        } else if(dialog instanceof ExcessOnDutyDialogFragment){
            String reason = ((ExcessOnDutyDialogFragment)dialog).reason;
            try {
                model.addXSDutyReaon(reason);
                lastXSDutyWarning = Calendar.getInstance();
                showProgress();
                Log.i("Main", "Saving xs on duty reason");
            } catch (Exception e){
                showError(e);
            }
        } else if(dialog instanceof ErrorDialogFragment){
            Throwable t = ((ErrorDialogFragment)dialog).throwable;
            if(t instanceof WebserviceException && !((WebserviceException)t).isServiceAvailable()){

            }
            Log.i("Main", "Error");
        }
    }
}
