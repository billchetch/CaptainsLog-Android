package net.chetch.captainslog;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.chetch.appframework.ErrorDialogFragment;
import net.chetch.appframework.GenericActivity;
import net.chetch.appframework.GenericDialogFragment;
import net.chetch.appframework.IDialogManager;
import net.chetch.captainslog.data.CrewMember;
import net.chetch.captainslog.data.LogEntries;
import net.chetch.captainslog.data.LogEntry;
import net.chetch.captainslog.models.MainViewModel;
import net.chetch.utilities.Logger;
import net.chetch.utilities.Utils;
import net.chetch.webservices.Webservice;
import net.chetch.webservices.WebserviceViewModel;
import net.chetch.webservices.exceptions.WebserviceException;
import net.chetch.webservices.gps.GPSPosition;
import java.util.Calendar;

public class MainActivity extends GenericActivity implements IDialogManager{
    static public int pollServerTime = 10; //in seconds

    MainViewModel model;
    LogEntryDialogFragment logEntryDialog = null;
    ExcessOnDutyDialogFragment excessOnDutyDialog = null;
    LogEntriesAdapter logEntriesAdapter;

    LogEntry topmostLogEntry = null;
    GPSPosition latestGPS = null;
    boolean initialLoad = true;
    boolean openXSDutyAfterLoad = false;
    boolean raisedXSDutyWarning = false;

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
            LogEntry le = entries.size() > 0 ? entries.get(0) : null;
            if(topmostLogEntry != null && le != null && topmostLogEntry.getID() != le.getID()){
                logEntriesAdapter.flashFirstItemOnBind = true;
            } else {
                logEntriesAdapter.flashFirstItemOnBind = false;
            }

            logEntriesAdapter.setDataset(entries);
            Log.i("Main","First page of entries displayed " + entries.size() + " entries");

            topmostLogEntry = le;
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

        Calendar cal = null;
        if(raisedXSDutyWarning) {
            cal = Calendar.getInstance();
            cal.setTimeInMillis(cal.getTimeInMillis() + 10000);
            Logger.warning("Crew has not handled XS duty warning");
        } else {
            cal = model.getXSOnDutyWarning().getValue();
            if(cal != null && cal.getTimeInMillis() <= Calendar.getInstance().getTimeInMillis()){
                cal.setTimeInMillis(Calendar.getInstance().getTimeInMillis() + 10000);
            }
        }

        if(cal != null){
            setWakeUp(cal);
        } else {
            cancelWakeUp();
        }

        Log.i("Main", "onStop: " + (cal == null ? " no wakeup to set " : "Wakeup set for " + Utils.formatDate(cal, Webservice.DEFAULT_DATE_FORMAT)));
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        cancelWakeUp();

        if(model.isServicesConfigured()) {
            model.getLatestGPSPosition();
            updateOnDuty(true);
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

    @Override
    public void openSettings() {
        super.openSettings();
    }

    public void updateOnDuty(boolean show){
        CrewMember crewMemberOnDuty = model.getCrewMemberOnDuty().getValue();
        if(crewMemberOnDuty == null)return;

        View progressCtn = findViewById(R.id.progressContainer);
        if(!show){
            progressCtn.setVisibility(View.GONE);
            return;
        }

        ImageView progressBar = findViewById(R.id.progressOnDutyBar);
        TextView progressInfo = findViewById(R.id.progressOnDutyInfo);
        GPSPosition pos = model.getGPSPosition().getValue();
        switch(crewMemberOnDuty.getLastState()){
            case MOVING:

                if(progressCtn.getVisibility() == View.GONE) {
                    progressCtn.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);

                    Log.i("Main", "Post delay for updateOnDuty to allow view to draw");
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        updateOnDuty(show);
                    }, 100);
                    return;
                }

                //do some calculations
                long hours  = crewMemberOnDuty.getOnDutyHours();
                long minutes = crewMemberOnDuty.getOnDutyMinutes();
                double dutyCompletion = crewMemberOnDuty.getOnDutyCompletion();
                int percentage = (int)(100*dutyCompletion);
                int age = Math.min((int)Math.floor(dutyCompletion * 5), 5);
                String s = (hours > 0 ? hours + "h " : "") + minutes + "m";
                s += " / " + percentage + "%";
                progressInfo.setText(s);

                //progress bar width
                ImageView progressBorder = findViewById(R.id.progressOnDutyBorder);
                int borderWidth = progressBorder.getWidth();
                progressBar.getLayoutParams().width = (int)(borderWidth*Math.min(dutyCompletion, 1.0));
                progressBar.setVisibility(View.VISIBLE);

                //progress bar color
                try {
                    progressBar.setBackgroundColor(getColorResource("age" + age));
                } catch (Exception e){
                    Log.e("Main", e.getMessage() + " for color " + age);
                }


                //bearing + speed
                if(pos != null && pos.getBearing() != null){
                    ImageView dir = findViewById(R.id.direction);
                    dir.setRotation(pos.getBearing().floatValue());

                    TextView tvHeading = findViewById(R.id.heading);
                    String heading = pos.getBearing().intValue() +  getString(R.string.symbol_degree) +  " @ " + String.format("%.1f", pos.getSpeed(GPSPosition.SpeedUnits.NPH)) + "kts";
                    tvHeading.setText(heading);
                    findViewById(R.id.headingLayout).setVisibility(View.VISIBLE);
                }
                break;

            case IDLE:
                raisedXSDutyWarning = false; //so it doesn't set an alarm
                progressCtn.setVisibility(View.GONE);
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
        raisedXSDutyWarning = true; //this will only get set back to false if the crew member responds positively to the dialog

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
                RecyclerView logEntriesRecyclerView = findViewById(R.id.logRecyclerView);
                logEntriesRecyclerView.smoothScrollToPosition(0);
                model.saveLogEntry(logEntry, dataLoadProgress);
                showProgress();
            } catch (Exception e){
                showError(e);
            }

            Log.i("Main", "Saving log entry");
        } else if(dialog instanceof ExcessOnDutyDialogFragment){
            String reason = ((ExcessOnDutyDialogFragment)dialog).reason;
            try {
                RecyclerView logEntriesRecyclerView = findViewById(R.id.logRecyclerView);
                logEntriesRecyclerView.smoothScrollToPosition(0);
                model.addXSDutyReason(reason, dataLoadProgress);
                showProgress();
                raisedXSDutyWarning = false; //means that the current duty warning was processed

                Log.i("Main", "Saving xs on duty reason");
            } catch (Exception e){
                showError(e);
            }
        } else if(dialog instanceof ErrorDialogFragment){
            Throwable t = ((ErrorDialogFragment)dialog).throwable;
            if(t instanceof WebserviceException){
                WebserviceException wsex = (WebserviceException)t;
                if(!wsex.isServiceAvailable()) {
                    model.loadData(dataLoadProgress);
                    showProgress();
                }
                String s = wsex.getMessage();
                if(wsex.getThrowable() != null){
                    s += " ... " + wsex.getThrowable().getClass().getCanonicalName() + ": " + wsex.getThrowable().getMessage();
                }
                Logger.exception(s);
                Log.e("Main", s);
            }

            Log.i("Main", "Error dialog onDialogPositiveClick");
        }
    }
}
