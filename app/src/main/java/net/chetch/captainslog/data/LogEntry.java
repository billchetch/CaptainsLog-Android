package net.chetch.captainslog.data;
import net.chetch.webservices.DataObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class LogEntry extends DataObject {
    public enum Event{
        RAISE_ANCHOR,
        SET_ANCHOR,
        DUTY_CHANGE,
        COMMENT
    }

    public enum State{
        IDLE,
        MOVING
    }

    static public List<Event> getPossibleEvents(State state){
        List<Event> events = new ArrayList<>();
        switch(state){
            case IDLE:
                events.add(Event.RAISE_ANCHOR);
                events.add(Event.COMMENT);
                break;

            case MOVING:
                events.add(Event.SET_ANCHOR);
                events.add(Event.DUTY_CHANGE);
                events.add(Event.COMMENT);
                break;

        }
        return events;
    }

    public List<Event> getPossibleEvents(){
        return getPossibleEvents(getState());
    }

    public Calendar getCreated(){
        return getCalendar("created");
    }

    public Event getEvent(){
        return (Event)getCasted("event");
    }

    public void setEvent(Event event, State defaultState){
        switch(event){
            case SET_ANCHOR:
                set("state", State.IDLE); break;
            case RAISE_ANCHOR:
                set("state", State.MOVING); break;
            default:
                set("state", defaultState); break;
        }

        set("event", event);
    }

    public State getState(){
        return (State)getCasted("state");
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
