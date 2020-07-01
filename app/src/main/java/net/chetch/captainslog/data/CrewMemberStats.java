package net.chetch.captainslog.data;

import net.chetch.webservices.DataObject;

import java.util.Calendar;

public class CrewMemberStats extends DataObject {

    @Override
    public void init() {
        super.init();

        asEnum("last_state", LogEntry.State.class);
        asEnum("last_event", LogEntry.Event.class);
    }

    public Calendar getStartedDuty(){
        return getCasted("started_duty");
    }

    public Calendar getEndedDuty(){
        return getCasted("ended_duty");
    }

    public LogEntry.State getLastState(){
        return getCasted("last_state");
    }
    public LogEntry.Event getLastEvent(){
        return getCasted("last_event");
    }
}
