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
}
