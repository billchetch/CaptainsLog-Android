package net.chetch.captainslog;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

public class GenericActivity  extends AppCompatActivity implements IDialogManager{

    private ErrorDialogFragment errorDialog;

    private boolean includeOptionsMenu = false;

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
        showError(0, t.getMessage());
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
        alertDialog.setTitle("Warning");
        alertDialog.setMessage(warning);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
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
