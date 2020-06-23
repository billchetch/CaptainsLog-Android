package net.chetch.captainslog;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import net.chetch.webservices.exceptions.WebserviceException;

public class GenericActivity  extends AppCompatActivity implements IDialogManager{

    private ErrorDialogFragment errorDialog;

    private boolean includeOptionsMenu = false;

    //timer stuff
    protected int timerDelay = 30;
    boolean timerStarted = false;
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            onTimer();
            timerHandler.postDelayed(this, timerDelay*1000);
        }
    };

    protected void onTimer(){

    }

    protected void startTimer(int timerDelay){
        if(timerStarted)return;
        this.timerDelay = timerDelay;
        timerHandler.postDelayed(timerRunnable, timerDelay*1000);
        timerStarted = true;
    }

    protected void stopTimer(){
        timerHandler.removeCallbacks(timerRunnable);
        timerStarted = false;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void includeOptionsMenu(){
        includeOptionsMenu = true;
        setSupportActionBar((Toolbar) findViewById(R.id.main_toolbar));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(includeOptionsMenu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.options_menu, menu);
        }
        return true;
    }


    protected int getResourceID(String resourceName, String resourceType){
        return getResources().getIdentifier(resourceName,resourceType, getPackageName());
    }

    protected int getColorResource(String resourceName){
        int resource = getResourceID(resourceName, "color");
        return ContextCompat.getColor(getApplicationContext(), resource);
    }

    public void openSettings(MenuItem menuItem){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void openHelp(MenuItem menuItem){
        /*Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);*/
    }

    public void openAbout(MenuItem menuItem){
        DialogFragment dialog = new AboutDialogFragment();
        dialog.show(getSupportFragmentManager(), "AboutDialog");
    }

    public void showError(int errorCode, String errorMessage){
        Log.e("GAERROR", errorMessage);
        hideProgress();
        dismissError();

        errorDialog = new ErrorDialogFragment(errorCode, errorMessage);
        errorDialog.show(getSupportFragmentManager(), "ErrorDialog");
    }

    public void showError(Throwable t){
        if(t instanceof WebserviceException){
            showError(((WebserviceException)t).getErrorCode(), t.getMessage());
        } else {
            showError(0, t.getMessage());
        }
        errorDialog.throwable = t;
    }
    public boolean isErrorShowing(){
        return errorDialog == null ? false : errorDialog.isShowing();
    }

    public void dismissError(){
        if(errorDialog != null)errorDialog.dismiss();
    }

    public void showProgress(int visibility){
        ProgressBar pb = findViewById(R.id.progressBar);
        if(pb != null){
            pb.setVisibility(visibility);
        }
    }

    public void showProgress(){ showProgress(View.VISIBLE);}
    public void hideProgress(){ showProgress(View.INVISIBLE); }


    public void showWarningDialog(String warning){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.dialog_warning_title));
        alertDialog.setMessage(warning);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.button_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public void showConfirmationDialog(String message, DialogInterface.OnClickListener okListener){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.dialog_confirmation_title));
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.button_ok), okListener);

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.button_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public void onDialogPositiveClick(GenericDialogFragment dialog){

    }

    public void onDialogNegativeClick(GenericDialogFragment dialog){

    }
}
