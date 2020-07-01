package net.chetch.captainslog.data;

import android.util.Log;

import net.chetch.webservices.employees.Employee;

import java.util.Calendar;

public class CrewMember extends Employee {

    public transient Calendar startedDuty;
    public transient Calendar endedDuty;
    public transient LogEntry.State currentState;
    public transient long onDutyLimit = -1; //in seconds


    public double getOnDutyCompletion() throws Exception{
        if(hasOnDutyLimit() && startedDuty != null && endedDuty == null){
            Calendar now = Calendar.getInstance();
            long elapsed = now.getTimeInMillis() - startedDuty.getTimeInMillis();
            return (double)onDutyLimit*1000 / (double)(elapsed);
        } else {
            throw new Exception("Crew is not on relevant duty");
        }
    }

    public boolean isOnDuty(){
        return startedDuty != null && endedDuty == null;
    }

    public boolean hasOnDutyLimit(){
        return currentState == LogEntry.State.MOVING && onDutyLimit > 0;
    }

    public boolean hasExceededOnDutyLimit(){
        if(hasOnDutyLimit()){
            try{
                return getOnDutyCompletion() > 1;
            } catch (Exception e){
                return false;
            }

        } else {
            return false;
        }
    }

}
