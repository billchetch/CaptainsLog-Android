package net.chetch.captainslog.data;
import net.chetch.webservices.DataObject;

import java.util.ArrayList;
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

    static public State getStateForAfterEvent(Event event, State currentState) throws Exception{
        if(currentState == null)return State.IDLE;

        List<Event> possibleEvents = getPossibleEvents(currentState);
        if(!possibleEvents.contains(event)){
            throw new Exception("Event " + event + " cannot occur in state " + currentState);
        }

        switch(event){
            case SET_ANCHOR:
                return State.IDLE;

            case RAISE_ANCHOR:
                return State.MOVING;

            case COMMENT:
            case DUTY_CHANGE:
                return currentState;
        }

        return null;
    }

    public List<Event> getPossibleEvents(){
        return getPossibleEvents(getState());
    }
    public State getStateForAfterEvent(){
        try {
            return getStateForAfterEvent(getEvent(), getState());
        } catch (Exception e){
            return null;
        }
    }

    public Calendar getCreated(){
        return getCalendar("created");
    }

    public Event getEvent(){
        return (Event)getCasted("event");
    }

    public String getEmployeeID(){ return getString("employee_id"); }

    public Double getLatitude(){ return getDouble("latitude"); }

    public Double getLongitude(){ return getDouble("longitude"); }

    public void setEvent(Event event, State defaultState){
        switch(event){
            case SET_ANCHOR:
                set("state", State.MOVING); break;
            case RAISE_ANCHOR:
                set("state", State.IDLE); break;
            default:
                set("state", defaultState); break;
        }

        set("event", event);
    }

    public State getState(){
        return (State)getCasted("state");
    }

    public void setEmployeeID(String eid){
        set("employee_id", eid);
    }

    public void setComment(String comment){
        set("comment", comment);
    }

    @Override
    public Object getCasted(String fieldName){
        switch(fieldName){
            case "event":
                return containsKey(fieldName) ? Event.valueOf(getString(fieldName)) : null;
            case "state":
                return containsKey(fieldName) ? State.valueOf(getString(fieldName)) : null;
            case "created":
                return getCalendar(fieldName);
            default:
                return super.getCasted(fieldName);
        }
    }
}
