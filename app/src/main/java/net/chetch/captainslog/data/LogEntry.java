package net.chetch.captainslog.data;
import net.chetch.webservices.DataField;
import net.chetch.webservices.DataObject;
import net.chetch.webservices.Webservice;
import net.chetch.webservices.gps.GPSPosition;

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


    @Override
    public void init() {
        super.init();

        asEnum("state", State.class);
        asEnum("event", Event.class);
        asString("employee_id");
        //asDouble("longitude");
        //asDouble("latitdue");
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
        return getCasted("created");
    }

    public Event getEvent(){
        return getCasted("event");
    }

    public String getEmployeeID(){ return getValue("employee_id").toString(); }

    public Double getLatitude(){ return getCasted("latitude"); }
    public void setLatitude(double l){ setValue("latitude", l); }

    public Double getLongitude(){ return getCasted("longitude"); }
    public void setLongitude(double l){ setValue("longitude", l); }

    public void setGPSPosition(GPSPosition pos){
        setLongitude(pos.getLongitude());
        setLatitude(pos.getLatitude());
    }

    public void setEvent(Event event, State defaultState){
        switch(event){
            case SET_ANCHOR:
                setValue("state", State.MOVING); break;
            case RAISE_ANCHOR:
                setValue("state", State.IDLE); break;
            default:
                setValue("state", defaultState); break;
        }

        setValue("event", event);
    }

    public State getState(){
        return getCasted("state");
    }

    public boolean requiresRevision(){
        return this.<Integer>getCasted("requires_revision") == 1;
    }

    public void setRequiresRevision(boolean mark){
        setValue("requires_revision", mark ? 1 : 0);
    }

    public void setEmployeeID(String eid){
        setValue("employee_id", eid);
    }

    public void setComment(String comment){
        setValue("comment", comment);
    }
}
