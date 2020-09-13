package net.chetch.captainslog.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import net.chetch.webservices.AboutService;
import net.chetch.webservices.DataCache;
import net.chetch.webservices.DataStore;
import net.chetch.webservices.WebserviceRepository;

public class CaptainsLogRepository extends WebserviceRepository<ICaptainsLogService> {
    static public int ENTRIES_PAGE_SIZE = 50;

    static private CaptainsLogRepository instance = null;
    static public CaptainsLogRepository getInstance(){
        if(instance == null)instance = new CaptainsLogRepository();
        return instance;
    }

    public CaptainsLogRepository(){
        this(DataCache.VERY_SHORT_CACHE);
    }
    public CaptainsLogRepository(int defaultCacheTime){
        super(ICaptainsLogService.class, defaultCacheTime);
    }

    public DataStore<AboutService> getAbout(){
        DataCache.CacheEntry<AboutService> entry = cache.getCacheEntry("about-service");

        if(entry.requiresUpdating()) {
            service.getAbout().enqueue(createCallback(entry));
        }

        return entry;
    }

    public DataStore<LogEntries> getLogEntriesFirstPage(){
        DataCache.CacheEntry<LogEntries> entry = cache.getCacheEntry("entries-first-page");

        if(entry.requiresUpdating()) {
            service.getEntriesByPage(1, ENTRIES_PAGE_SIZE).enqueue(createCallback(entry));
        }

        return entry;
    }

    public DataStore<LogEntry> saveLogEntry(LogEntry logEntry){
        final DataStore<LogEntry> dse = new DataStore<>();

        service.putEntry(logEntry, logEntry.getID()).enqueue(createCallback(dse));

        cache.forceExpire("entries-first-page");

        return dse;
    }

    public DataStore<CrewStats> getCrewStats(){
        DataCache.CacheEntry<CrewStats> entry = cache.getCacheEntry("crew-stats");

        if(entry.requiresUpdating()) {
            service.getCrewStats().enqueue(createCallback(entry));
        }

        return entry;
    }
}
