package net.chetch.captainslog.data;
import net.chetch.webservices.DataObject;
import net.chetch.webservices.Webservice;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LogEntry extends DataObject {
    static public final String ENTRY_DATE_FORMAT = Webservice.DEFAULT_DATE_FORMAT;

    public enum Event{
        RAISE_ANCHOR,
        SET_ANCHOR,
        DUTY_CHANGE,
        COMMENT,
        ALERT
    }

    public enum State{
        IDLE,
        MOVING
    }

    public enum XSDutyReason{
        CREW_ASLEEP,
        CREW_SICK,
        NEAR_DESTINATION,
        OTHER_REASON
    }

    static public List<Event> getPossibleEvents(State state){
        List<Event> events = new ArrayList<>();
        switch(state){
            case IDLE:
                events.add(Event.RAISE_ANCHOR);
                events.add(Event.DUTY_CHANGE);
                events.add(Event.COMMENT);
                events.add(Event.ALERT);
                break;

            case MOVING:
                events.add(Event.SET_ANCHOR);
                events.add(Event.DUTY_CHANGE);
                events.add(Event.COMMENT);
                events.add(Event.ALERT);
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
            case ALERT:
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

    public boolean isStateChange(){
        State state = getState();
        if(state != null){
            return state != getStateForAfterEvent();
        } else {
            return false;
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

    public boolean requiresRevision(){
        return getInteger("requires_revision") == 1;
    }

    public void setRequiresRevision(boolean mark){
        set("requires_revision", mark ? 1 : 0);
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
