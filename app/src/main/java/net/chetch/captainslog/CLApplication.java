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

import net.chetch.appframework.ChetchApplication;
import net.chetch.captainslog.data.CrewMember;
import net.chetch.utilities.Logger;
import net.chetch.webservices.network.NetworkRepository;

import java.util.Calendar;

public class CLApplication extends ChetchApplication {

    private Calendar appStarted;

    @Override
    public void onCreate() {
        LOG_FILE = "cllog";
        appStarted = Calendar.getInstance();

        super.onCreate();

        //set default prefs so application can function
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        try{
            String apiBaseURL = sharedPref.getString("api_base_url", null);
            //String apiBaseURL = "http://192.168.1.103:8001/api/";
            NetworkRepository.getInstance().setAPIBaseURL(apiBaseURL);

            String dutyLimit = sharedPref.getString("on_duty_limit", "240");
            CrewMember.onDutyLimit = 60 * Integer.parseInt(dutyLimit);

            String pollTime = sharedPref.getString("poll_server_time", "10");
            MainActivity.pollServerTime = Integer.parseInt(pollTime);

        } catch (Exception e){
            Log.e("Application", e.getMessage());
        }

        //add a network change listener to handle network state changes
        /*IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Logger.warning("Wifi network state change");
            }
        }, intentFilter);*/
    }

    public long getUpTime(){
        return Calendar.getInstance().getTimeInMillis() - appStarted.getTimeInMillis();
    }
}
