package net.chetch.captainslog.data;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import net.chetch.webservices.LiveDataCache;
import net.chetch.webservices.WebserviceRepository;
import net.chetch.webservices.employees.Employees;

import java.util.List;

public class CaptainsLogRepository extends WebserviceRepository<ICaptainsLogService> {
    static public int ENTRIES_PAGE_SIZE = 250;

    public CaptainsLogRepository(){
        this(LiveDataCache.SHORT_CACHE);
    }
    public CaptainsLogRepository(int defaultCacheTime){
        super(ICaptainsLogService.class, defaultCacheTime);
    }

    public LiveData<LogEntries> getLogEntriesByPage(int pageNumber, int pageSize){
        final MutableLiveData<LogEntries> entries = new MutableLiveData<>();

        service.getEntriesByPage(pageNumber, pageSize).enqueue(createCallback(entries));

        return entries;
    }

    public LiveData<LogEntries> getLogEntriesByPage(int pageNumber){
        return getLogEntriesByPage(pageNumber, ENTRIES_PAGE_SIZE);
    }

    public LiveData<LogEntries> getLogEntriesFirstPage(){
        LiveDataCache.CacheEntry entry = cache.<LogEntries>getCacheEntry("entries-first-page");

        if(entry.refreshValue()) {
            return getLogEntriesByPage(1, ENTRIES_PAGE_SIZE);
        }

        return entry.liveData;
    }
}
