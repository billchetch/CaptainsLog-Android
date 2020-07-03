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

import static android.view.View.GONE;

public class MainActivity extends GenericActivity {
    MainViewModel model;
    LogEntryDialogFragment logEntryDialog = null;
    ExcessOnDutyDialogFragment excessOnDutyDialog = null;

    GPSPosition latestGPS = null;

    Calendar lastXSDutyWarning = null;

    Observer onDataLoaded  = data -> {
        hideProgress();
        findViewById(R.id.bodyLinearLayout).setVisibility(View.VISIBLE);
        Log.i("Main","Model data has loaded");
        findViewById(R.id.btnAddLogEntry).setEnabled(true);
        startTimer(10);
        updateOnDuty(true);
    };

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

        //set up some initia stuff
        includeOptionsMenu();
        showProgress();
        findViewById(R.id.btnAddLogEntry).setEnabled(false);
        findViewById(R.id.headingLayout).setVisibility(View.GONE);

        //get the model, load data and add data observers
        model = ViewModelProviders.of(this).get(MainViewModel.class);

        //observe errors
        model.getError().observe(this, t ->{
            showError(t);
            Log.e("Main", "Error: " + t.getMessage());
        });

        //load data
        model.loadData(onDataLoaded);

        //gps updates
        model.getGPSPosition().observe(this, pos->{
            updateOnDuty(true);
            Log.i("Main", "lobserving latest gps position ");
        });

        //entries
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

        //update UI with crew member on duty
        model.getCrewMemberOnDuty().observe(this, crewMember -> {

            LogEntry logEntry = model.getLatestLogEntry();

            ImageView iv = findViewById(R.id.profilePicCrewOnDuty);
            iv.setImageBitmap(crewMember.profileImage);

            //known as
            TextView tv = findViewById(R.id.crewOnDutyKnownAs);
            tv.setText(crewMember.getKnownAs());

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
        model.getLatestGPSPosition();
        updateOnDuty(true);
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

        Log.i("Main", "Setting wakeup for " + Utils.formatDate(wakeUp, Webservice.DEFAULT_DATE_FORMAT));

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

        GPSPosition pos = model.getGPSPosition().getValue();
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

                //bearing + speed
                if(pos != null && pos.getBearing() != null){
                    ImageView dir = findViewById(R.id.direction);
                    dir.setRotation(pos.getBearing());

                    TextView tvHeading = findViewById(R.id.heading);
                    String heading = pos.getBearing() +  getString(R.string.symbol_degree) +  " @ " + String.format("%.1f", pos.getSpeed(GPSPosition.SpeedUnits.NPH)) + "kts";
                    tvHeading.setText(heading);
                    findViewById(R.id.headingLayout).setVisibility(View.VISIBLE);
                }

                break;

            case IDLE:
                progressBar.setVisibility(View.INVISIBLE);
                int resource = getResourceID("log_entry.state." + crewMemberOnDuty.getLastState(), "string");
                progressInfo.setText(getString(resource));
                progressInfo.setVisibility(View.VISIBLE);
                findViewById(R.id.headingLayout).setVisibility(View.GONE);
                break;
        }

        //moving or idle we update the latest lat/lon position
        if(pos != null){
            TextView gpstv = findViewById(R.id.gps);
            String lat = String.format("%.5f", pos.getLatitude());
            String lon = String.format("%.5f", pos.getLongitude());
            gpstv.setText(lat + "/" + lon);
        }
    }

    public void openLogEntry(View view){
        /*Calendar momentLater = Calendar.getInstance();
        momentLater.setTimeInMillis(momentLater.getTimeInMillis() + 5000);
        setWakeUp(momentLater);*/

        if(logEntryDialog != null){
            logEntryDialog.dismiss();
        }
        logEntryDialog = new LogEntryDialogFragment();
        logEntryDialog.crew = model.getCrew().getValue();
        logEntryDialog.latestLogEntry = model.getLatestLogEntry();

        logEntryDialog.show(getSupportFragmentManager(), "LogEntryDialog");
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

            try {
                model.saveLogEntry(logEntry);
                showProgress();
            } catch (Exception e){
                showError(e);
            }

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
                showProgress();
                model.loadData(onDataLoaded);
            }
            Log.i("Main", "Error");
        }
    }
}
