package net.chetch.captainslog.data;
import net.chetch.webservices.DataObject;

import java.util.Calendar;

public class LogEntry extends DataObject {
    public enum Event{
        RAISE_ANCHOR,
        SET_ANCHOR,
        DUTY_CHANGE,
        COMMENT
    }

    public Calendar getCreated(){
        return getCalendar("created");
    }

    public Event getEvent(){
        return (Event)getCasted("event");
    }

    @Override
    public Object getCasted(String fieldName){
        switch(fieldName){
            case "event":
                return Event.valueOf(getString(fieldName));
            case "created":
                return getCalendar(fieldName);
            default:
                return super.getCasted(fieldName);
        }
    }
}
