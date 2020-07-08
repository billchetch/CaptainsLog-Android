package net.chetch.captainslog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import net.chetch.appframework.SettingsActivityBase;
import net.chetch.captainslog.data.CrewMember;
import net.chetch.webservices.network.NetworkRepository;

public class SettingsActivity extends SettingsActivityBase{
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("api_base_url")){
            restartMainActivityOnFinish = true;
            try{
                String apiBaseURL = sharedPreferences.getString(key, null);
                NetworkRepository.getInstance().setAPIBaseURL(apiBaseURL);
            } catch (Exception e){
                Log.e("Settings", e.getMessage());
            }
        }
        if(key.equals("on_duty_limit")){
            restartMainActivityOnFinish = true;
            String dutyLimit = sharedPreferences.getString(key, "240");
            CrewMember.onDutyLimit = 60 * Integer.parseInt(dutyLimit);
        }
        if(key.equals("poll_server_time")){
            restartMainActivityOnFinish = true;
            String pollTime = sharedPreferences.getString(key, "10");
            MainActivity.pollServerTime = Integer.parseInt(pollTime);
        }

    }
}
