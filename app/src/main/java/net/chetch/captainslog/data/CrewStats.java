package net.chetch.captainslog.data;

import android.util.Log;

import net.chetch.utilities.Utils;

import java.util.HashMap;
import java.util.Calendar;

public class CrewStats extends HashMap<String,HashMap<String,Object>> {


    public HashMap<String, Object> getStats(String employeeID){
        return containsKey(employeeID) ? get(employeeID) : null;
    }

    public Object getEmployeeStat(String employeeID, String statName){
        HashMap<String, Object> stats = getStats(employeeID);
        if(stats != null && stats.containsKey(statName)){
            return stats.get(statName);
        } else {
            return null;
        }
    }

    public Calendar getStartedDuty(String employeeID){
        String sdt = getEmployeeStat(employeeID, "started_duty").toString();
        try {
            return Utils.parseDate(sdt, LogEntry.ENTRY_DATE_FORMAT);
        } catch (Exception e){
            Log.e("CrewStats", e.getMessage());
            return null;
        }
    }
}
