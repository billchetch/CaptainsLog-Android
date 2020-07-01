package net.chetch.captainslog.data;

import android.util.Log;

import net.chetch.utilities.Utils;
import net.chetch.webservices.DataObject;

import java.util.HashMap;
import java.util.Calendar;

public class CrewStats extends HashMap<String, CrewMemberStats> {

    public boolean hasStats(String employeeID){
        return containsKey(employeeID);
    }

    public CrewMemberStats getStats(String employeeID){
        return containsKey(employeeID) ? get(employeeID) : null;
    }

    public Object getEmployeeStat(String employeeID, String statName){
        CrewMemberStats stats = getStats(employeeID);
        if(stats != null && stats.hasField(statName)){
            return stats.getValue(statName);
        } else {
            return null;
        }
    }

    public Calendar getStartedDuty(String employeeID){
        return (Calendar)getEmployeeStat(employeeID, "started_duty");
    }

    public Calendar getEndedDuty(String employeeID){
        return (Calendar)getEmployeeStat(employeeID, "started_duty");
    }
}
