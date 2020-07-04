package net.chetch.captainslog;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

import net.chetch.utilities.Logger;
import net.chetch.webservices.network.NetworkRepository;

public class CLApplication extends Application {

    static public final String LOG_FILE = "bbcl.log";

    @Override
    public void onCreate() {
        super.onCreate();
        //initialise logger
        Logger.init(this, LOG_FILE);
        //Logger.clear();
        Logger.info("Application started");

        //set default uce handler
        Thread.setDefaultUncaughtExceptionHandler(new UCEHandler(this, LOG_FILE));

        //set default prefs and API Base URL
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String apiBaseURL = sharedPref.getString("api_base_url", null);
        try{
            NetworkRepository.getInstance().setAPIBaseURL(apiBaseURL);
        } catch (Exception e){
            Log.e("Application", e.getMessage());
        }

        //add a network change listener to handle network state changes
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Logger.warning("Wifi network state change");
            }
        }, intentFilter);

    }
}
