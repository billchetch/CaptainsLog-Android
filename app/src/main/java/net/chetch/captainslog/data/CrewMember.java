package net.chetch.captainslog.data;

import android.util.Log;

import net.chetch.utilities.Utils;
import net.chetch.webservices.employees.Employee;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class CrewMember extends Employee {

    static public int onDutyLimit = 60*60*1; //in seconds

    public transient CrewMemberStats stats;
    private transient Calendar lastWarnedOn = null;

    public double getOnDutyCompletion(){
        if(hasOnDutyLimit() && isOnDuty()){
            Calendar now = Calendar.getInstance();
            long elapsed = now.getTimeInMillis() - stats.getStartedDuty().getTimeInMillis();
            return Math.max(0, (double)(elapsed) / (double)(onDutyLimit*1000));
        } else {
            return 0;
        }
    }

    public int getOnDutyHours(){
        Calendar now = Calendar.getInstance();
        int hours = (int)Utils.hoursDiff(now, stats.getStartedDuty());
        return Math.max(0, hours);
    }

    public int getOnDutyMinutes(){
        Calendar now = Calendar.getInstance();
        int hours = getOnDutyHours();
        int minutes = (int)Utils.dateDiff(now, stats.getStartedDuty(), TimeUnit.MINUTES) - 60*hours;
        return Math.max(0, minutes);
    }

    public boolean isOnDuty(){
        return stats.getStartedDuty() != null && stats.getEndedDuty() == null;
    }

    public boolean hasOnDutyLimit(){
        return stats.getLastState() == LogEntry.State.MOVING && onDutyLimit > 0;
    }

    public LogEntry.State getLastState(){
        return stats.getLastState();
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

    private double getXSOnDutyWarningRatio(double intervalAsRatio, int shiftDirection){
        double dutyCompletion = Math.max(getOnDutyCompletion(), 1);
        double shift = intervalAsRatio / 2;
        double ratio = Math.ceil((dutyCompletion - shift)/intervalAsRatio)*intervalAsRatio + shiftDirection*shift;
        return ratio;
    }

    public Calendar getNextXSOnDutyWarning(){
        if(hasOnDutyLimit()){
            double nextWarning = getXSOnDutyWarningRatio(0.1, 1);
            Log.i("CrewMember", "duty completion " + getOnDutyCompletion() + " gives next warning ratio " + nextWarning);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(stats.getStartedDuty().getTimeInMillis() + (long)(onDutyLimit*1000*nextWarning));
            return cal;
        } else {
            return null;
        }
    }

    public Calendar getPrevXSOnDutyWarning(){
        if(hasOnDutyLimit()){
            double prevWarning = getXSOnDutyWarningRatio(0.1, -1);
            Log.i("CrewMember", "duty completion " + getOnDutyCompletion() + " prev warning ratio " + prevWarning);
            if(prevWarning < 1)return null;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(stats.getStartedDuty().getTimeInMillis() + (long)(onDutyLimit*1000*prevWarning));
            return cal;
        } else {
            return null;
        }
    }

}
