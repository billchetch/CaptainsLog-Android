package net.chetch.captainslog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import net.chetch.appframework.ErrorDialogFragment;
import net.chetch.appframework.GenericActivity;
import net.chetch.appframework.GenericDialogFragment;
import net.chetch.appframework.IDialogManager;
import net.chetch.captainslog.data.CaptainsLogRepository;
import net.chetch.captainslog.data.Crew;
import net.chetch.captainslog.data.CrewMember;
import net.chetch.captainslog.data.CrewRepository;
import net.chetch.captainslog.data.CrewStats;
import net.chetch.captainslog.data.LogEntries;
import net.chetch.captainslog.data.LogEntry;
import net.chetch.captainslog.models.MainViewModel;
import net.chetch.utilities.Logger;
import net.chetch.utilities.Utils;
import net.chetch.webservices.DataObjectCollection;
import net.chetch.webservices.Webservice;
import net.chetch.webservices.WebserviceRepository;
import net.chetch.webservices.WebserviceViewModel;
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

public class MainActivity extends GenericActivity implements IDialogManager{
    static public int pollServerTime = 10; //in seconds

    MainViewModel model;
    LogEntryDialogFragment logEntryDialog = null;
    ExcessOnDutyDialogFragment excessOnDutyDialog = null;
    LogEntriesAdapter logEntriesAdapter;

    GPSPosition latestGPS = null;
    boolean initialLoad = true;
    boolean openXSDutyAfterLoad = false;

    Observer dataLoadProgress  = obj -> {
        WebserviceViewModel.LoadProgress progress = (WebserviceViewModel.LoadProgress)obj;
        String state = progress.startedLoading ? "Loading" : "Loaded";
        String progressInfo = state + " " + progress.info.toLowerCase();
        setProgressInfo(progressInfo);

        Log.i("Main", "load observer " + state + " " + progress.info);

        if(initialLoad && progress.dataLoaded instanceof LogEntries){
            initialLoad = false;

            updateOnDuty(true);
            Log.i("Main", "Finished initial load");

            if(openXSDutyAfterLoad && (excessOnDutyDialog == null || !excessOnDutyDialog.isShowing())){
                openExcessOnDuty();
                Log.i("Main", "Open XS on duty dialog after load");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set up some generic stuff
        includeActionBar(SettingsActivity.class);
        enableDeviceWakeup();

        RecyclerView logEntriesRecyclerView = findViewById(R.id.logRecyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        logEntriesRecyclerView.setHasFixedSize(true);
        logEntriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // specify an adapter
        logEntriesAdapter = new LogEntriesAdapter(this);
        logEntriesRecyclerView.setAdapter(logEntriesAdapter);


        //get the model, load data and add data observers
        model = ViewModelProviders.of(this).get(MainViewModel.class);

        //observe errors
        model.getError().observe(this, t ->{
            showError(t);
            Log.e("Main", "Error: " + t.getMessage());
        });

        //entries
        model.getEntriesFirstPage().observe(this, entries->{

            logEntriesAdapter.setDataset(entries);
            Log.i("Main","First page of entries displayed " + entries.size() + " entries");

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

        model.getXSOnDutyWarning().observe(this, cal->{
            String s = cal == null ? " null " : Utils.formatDate(cal, Webservice.DEFAULT_DATE_FORMAT) + " (now is " + Utils.formatDate(Calendar.getInstance(), Webservice.DEFAULT_DATE_FORMAT) + ")";
            Log.i("Main", "Set on duty warning " + s);
        });


        //load data
        Intent intent = getIntent();
        if(intent != null && intent.getBooleanExtra("wakeup", false)){
            openXSDutyAfterLoad = true;
        } else {
            openXSDutyAfterLoad = false;
        }

        findViewById(R.id.headingLayout).setVisibility(View.GONE);
        showProgress();
        model.loadData(dataLoadProgress);

        //gps updates
        model.getGPSPosition().observe(this, pos->{
            updateOnDuty(true);
        });

        Log.i("Main", "onCreate... on duty limit set to " + CrewMember.onDutyLimit + " secs");
    }

    @Override
    protected void onStart() {
        super.onStart();

        startTimer(pollServerTime);
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopTimer();

        Calendar cal = model.getXSOnDutyWarning().getValue();
        if(cal != null){
            if(cal.getTimeInMillis() <= Calendar.getInstance().getTimeInMillis() + 1000){
                cal = Calendar.getInstance();
                cal.setTimeInMillis(cal.getTimeInMillis() + 15000);
            }

            setWakeUp(cal);
            Logger.info("Setting wake up for " + Utils.formatDate(cal, Webservice.DEFAULT_DATE_FORMAT));
        } else {
            cancelWakeUp();
        }
        Log.i("Main", "onStop: " + (cal == null ? " no wakeup to set " : Utils.formatDate(cal, Webservice.DEFAULT_DATE_FORMAT)));
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        cancelWakeUp();

        if(model.isServicesConfigured()) {
            model.getLatestGPSPosition();
        }
    }

    @Override
    protected int onTimer(){

        CrewMember crewMemberOnDuty = model.getCrewMemberOnDuty().getValue();
        if(crewMemberOnDuty != null) {
            Calendar xsOnDutyWarning = model.getXSOnDutyWarning().getValue();
            boolean requiresWarning =  xsOnDutyWarning != null && xsOnDutyWarning.getTimeInMillis() < Calendar.getInstance().getTimeInMillis();

            if(requiresWarning && (excessOnDutyDialog == null || !excessOnDutyDialog.isShowing())) {
                openExcessOnDuty();
                Log.i("Main", "Now: " + Utils.formatDate(Calendar.getInstance(), Webservice.DEFAULT_DATE_FORMAT) + ", Warning set for: " + Utils.formatDate(xsOnDutyWarning, Webservice.DEFAULT_DATE_FORMAT));
            }
        }
        if(model.isServicesConfigured()) {
            model.getLatestGPSPosition();
        }
        updateOnDuty(true);

        return super.onTimer();
    }


    @Override
    public void showProgress() {
        super.showProgress();
        enableTouchEvents(false);
        findViewById(R.id.btnAddLogEntry).setEnabled(false);
        findViewById(R.id.mainLayout).setAlpha(0.2f);
    }

    @Override
    public void hideProgress() {
        super.hideProgress();
        enableTouchEvents(true);
        findViewById(R.id.btnAddLogEntry).setEnabled(true);
        findViewById(R.id.mainLayout).setAlpha(1.0f);
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
                    dir.setRotation(pos.getBearing().floatValue());

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
        if(logEntryDialog != null){
            logEntryDialog.dismiss();
        }
        logEntryDialog = new LogEntryDialogFragment();
        logEntryDialog.crew = model.getCrew().getValue();
        logEntryDialog.latestLogEntry = model.getLatestLogEntry();

        logEntryDialog.show(getSupportFragmentManager(), "LogEntryDialog");
    }

    public void openExcessOnDuty(){
        Logger.warning("Excess duty dialog opened");

        if(excessOnDutyDialog != null){
            excessOnDutyDialog.dismiss();
        }
        excessOnDutyDialog = new ExcessOnDutyDialogFragment();
        excessOnDutyDialog.show(getSupportFragmentManager(), "ExcessOnDutyDialog");

        //play a notification sound
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDialogPositiveClick(GenericDialogFragment dialog){
        if(dialog instanceof LogEntryConfirmationDialogFragment){
            LogEntry logEntry = ((LogEntryConfirmationDialogFragment)dialog).logEntry;
            logEntryDialog.dismiss();

            try {
                model.saveLogEntry(logEntry, dataLoadProgress);
                showProgress();
            } catch (Exception e){
                showError(e);
            }

            Log.i("Main", "Saving log entry");
        } else if(dialog instanceof ExcessOnDutyDialogFragment){
            String reason = ((ExcessOnDutyDialogFragment)dialog).reason;
            try {
                model.addXSDutyReason(reason, dataLoadProgress);
                showProgress();
                Log.i("Main", "Saving xs on duty reason");
            } catch (Exception e){
                showError(e);
            }
        } else if(dialog instanceof ErrorDialogFragment){
            Throwable t = ((ErrorDialogFragment)dialog).throwable;
            if(t instanceof WebserviceException && !((WebserviceException)t).isServiceAvailable()){
                model.loadData(dataLoadProgress);
                showProgress();
            }
            Log.i("Main", "Error dialog onDialogPositiveClick");
        }
    }
}
