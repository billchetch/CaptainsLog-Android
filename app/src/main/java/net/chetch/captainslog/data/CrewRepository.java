package net.chetch.captainslog.data;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import net.chetch.webservices.LiveDataCache;
import net.chetch.webservices.employees.Employee;
import net.chetch.webservices.employees.Employees;
import net.chetch.webservices.employees.EmployeesRepository;

import java.util.List;

public class CrewRepository extends EmployeesRepository{

    final static public int ABK = 1;

    public CrewRepository(){
        super(LiveDataCache.VERY_LONG_CACHE);
    }

    public LiveData<Employees> getCrew(){
        LiveDataCache.CacheEntry entry = cache.<Employees>getCacheEntry("crew");

        if(entry.refreshValue()) {
            service.getEmployees(ABK).enqueue(createCallback(entry));
        }

        return entry.liveData;
    }
}
