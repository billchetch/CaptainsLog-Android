package net.chetch.captainslog.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import android.util.Log;

import net.chetch.captainslog.data.CaptainsLogRepository;
import net.chetch.captainslog.data.Crew;
import net.chetch.captainslog.data.CrewMember;
import net.chetch.captainslog.data.CrewRepository;
import net.chetch.captainslog.data.LogEntries;
import net.chetch.captainslog.data.LogEntry;
import net.chetch.webservices.DataStore;
import net.chetch.webservices.WebserviceViewModel;
import net.chetch.webservices.gps.GPSPosition;
import net.chetch.webservices.gps.GPSRepository;

import java.util.Calendar;

public class GenericViewModel extends WebserviceViewModel {
    CaptainsLogRepository logRepository = CaptainsLogRepository.getInstance();
    CrewRepository crewRepository = CrewRepository.getInstance();
    GPSRepository gpsRepository = GPSRepository.getInstance();

    Crew.FieldMap<String> eidMap;
    LogEntry latestLogEntry;
    CrewMember crewMemberOnDuty;

    MutableLiveData<Crew> liveDataCrew = new MutableLiveData<>();
    MutableLiveData<LogEntries> liveDataEntriesFirstPage = new MutableLiveData<>();
    MutableLiveData<CrewMember> liveDataCrewMemberOnDuty = new MutableLiveData<>();
    MutableLiveData<GPSPosition> liveDataGPSPosition = new MutableLiveData<>();
    MutableLiveData<Calendar> liveDataXSOnDutyWarning = new MutableLiveData<>();

    public GenericViewModel(){
        addRepo(crewRepository);
        addRepo(logRepository);
        addRepo(gpsRepository);

        serverTimeDisparityOption = ServerTimeDisparityOptions.LOG_WARNING;

        permissableServerTimeDifference = 0;

        liveDataCrew.observeForever(employees->{
            eidMap = employees.employeeIDMap();
        });
    }


    @Override
    protected ServerTimeDisparityOptions getServerTimeDisparityOption(long serverTimeDifference) {
        ServerTimeDisparityOptions defaultOption = super.getServerTimeDisparityOption(serverTimeDifference);

        Log.i("GVM", "Server time difference exceeding " + permissableServerTimeDifference);
        return defaultOption;
    }

    @Override
    public DataStore loadData(Observer observer){
        DataStore<?> dataStore = super.loadData(observer);
        dataStore.observe(data ->{
            notifyLoading(observer, "Crew");
            crewRepository.getCrew().add(liveDataCrew).observe(crew->{
                notifyLoading(observer, "Profile pics", crew);
                crewRepository.getProfilePics(crew).observe( bms ->{
                    notifyLoading(observer, "GPS", bms);
                    //get latest gps position
                    gpsRepository.getLatestPosition().add(liveDataGPSPosition).observe(gps->{
                        notifyLoaded(observer, gps);

                        //get log entries
                        loadEntries(observer);
                    });
                });
            }); //end getting the crew
        }); //end generic data load (service config)
        return dataStore;
    }


    protected void loadEntries(){
        loadEntries(null);
    }

    protected void loadEntries(Observer observer){
        notifyLoading(observer, "Crew stats");
        logRepository.getCrewStats().observe(stats->{
            notifyLoading(observer, "Log entries", stats);

            logRepository.getLogEntriesFirstPage().add(liveDataEntriesFirstPage).observe(entries->{
                //update all crew with stats
                for(LogEntry entry : entries){
                    String eid = entry.getEmployeeID();
                    if(eidMap.containsKey(eid) && stats.hasStats(eid)){
                        CrewMember cm = eidMap.get(eid);
                        cm.stats = stats.getStats(eid);
                    }
                }

                //ensure entries are in chron order and then set latest log entry
                if(entries.size() > 0) {
                    entries.sortLatestFirst();
                    setLatestLogEntry(entries.get(0));
                }

                notifyLoaded(observer, entries);
            });
        });

    }

    public LiveData<LogEntries> getEntriesFirstPage(){
        return liveDataEntriesFirstPage;
    }

    public LiveData<Crew> getCrew(){
        return liveDataCrew;
    }

    public LiveData<CrewMember> getCrewMemberOnDuty(){
        return liveDataCrewMemberOnDuty;
    }

    private void setLatestLogEntry(LogEntry logEntry){

        CrewMember cm = eidMap.get(logEntry.getEmployeeID());
        Calendar xsOnDutyWarning = cm.getNextXSOnDutyWarning();
        liveDataXSOnDutyWarning.setValue(xsOnDutyWarning);
        latestLogEntry = logEntry;
        crewMemberOnDuty = cm;
        liveDataCrewMemberOnDuty.setValue(crewMemberOnDuty);
        Log.i("GVM", "Set lates entry");
    }

    public LogEntry getLatestLogEntry(){
        return latestLogEntry;
    }

    public CrewMember getCrewMember(String employeeID){
        return eidMap.containsKey(employeeID) ? eidMap.get(employeeID) : null;
    }

    public DataStore<LogEntry> saveLogEntry(LogEntry logEntry, Observer observer) throws Exception{
        if(logEntry.isNew()){ //is a new entry we
            GPSPosition pos = getGPSPosition().getValue();
            if(pos == null){
                throw new Exception("Cannot save log entry as no GPS position available");
            }
            logEntry.setGPSPosition(pos);
        }

        return logRepository.saveLogEntry(logEntry).observe(le ->{
            Log.i("GVM", "Entry " + le.getID() + " saved");
            if(logEntry.isNew()) {
                loadProgress.reset();
                loadEntries(observer);
            }
        });
    }

    public DataStore<LogEntry> addXSDutyReason(String reason, Observer observer) throws Exception{
        LogEntry latestLogEntry = getLatestLogEntry();
        if(latestLogEntry == null){
            throw new Exception("No previous log entry");
        }
        LogEntry logEntry = new LogEntry();
        logEntry.setEmployeeID(latestLogEntry.getEmployeeID());
        logEntry.setEvent(LogEntry.Event.ALERT, latestLogEntry.getStateForAfterEvent());
        logEntry.setComment(reason);

        liveDataXSOnDutyWarning.setValue(null);
        return saveLogEntry(logEntry, observer);
    }

    public LiveData<GPSPosition> getGPSPosition(){
        return liveDataGPSPosition;
    }

    public LiveData<GPSPosition> getLatestGPSPosition(){
        gpsRepository.getLatestPosition().add(liveDataGPSPosition);
        return liveDataGPSPosition;
    }

    public LiveData<Calendar> getXSOnDutyWarning(){
        return liveDataXSOnDutyWarning;
    }
}
