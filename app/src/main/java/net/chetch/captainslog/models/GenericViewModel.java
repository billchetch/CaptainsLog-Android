package net.chetch.captainslog.models;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
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

public class GenericViewModel extends WebserviceViewModel {
    static public final String CAPTAINS_LOG_SERVICE_NAME = "Captains Log";
    static public final String EMPLOYEES_SERVICE_NAME = "Employees";

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
    public void loadData(Observer observer){

        super.loadData(data->{
            crewRepository.getCrew().add(liveDataCrew).observe(crew->{
                crewRepository.getProfilePics(crew).observe( bms ->{
                    loadEntries(observer);
                    gpsRepository.getLatestPosition(); //get latest gps position
                });
            }); //end getting the crew

        }); //end generic data load (service config)
    }

    protected void loadEntries(){
        loadEntries(null);
    }

    protected void loadEntries(Observer observer){
        logRepository.getCrewStats().observe(stats->{
            Log.i("GVM", "Loaded crew stats");

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

                Log.i("GVM", "loadData: Entries loaded");
                notifyObserver(observer, entries);
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
        latestLogEntry = logEntry;
        CrewMember cm = eidMap.get(logEntry.getEmployeeID());
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

    public DataStore<LogEntry> saveLogEntry(LogEntry logEntry) throws Exception{
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
                loadEntries();
            }
        });
    }

    public DataStore<LogEntry> addXSDutyReaon(String reason) throws Exception{
        LogEntry latestLogEntry = getLatestLogEntry();
        if(latestLogEntry == null){
            throw new Exception("No previous log entry");
        }
        LogEntry logEntry = new LogEntry();
        logEntry.setEmployeeID(latestLogEntry.getEmployeeID());
        logEntry.setEvent(LogEntry.Event.ALERT, latestLogEntry.getStateForAfterEvent());
        logEntry.setComment(reason);
        return saveLogEntry(logEntry);
    }

    public LiveData<GPSPosition> getGPSPosition(){
        return liveDataGPSPosition;
    }

    public LiveData<GPSPosition> getLatestGPSPosition(){
        gpsRepository.getLatestPosition().add(liveDataGPSPosition);
        return liveDataGPSPosition;
    }
}
