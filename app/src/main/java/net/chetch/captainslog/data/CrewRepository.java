package net.chetch.captainslog.data;

import android.graphics.Bitmap;
import android.util.Log;

import net.chetch.webservices.DataCache;
import net.chetch.webservices.DataObjectCollection;
import net.chetch.webservices.DataStore;
import net.chetch.webservices.ITypeConverter;
import net.chetch.webservices.employees.Employee;
import net.chetch.webservices.employees.EmployeesRepository;

import java.util.ArrayList;
import java.util.HashMap;

public class CrewRepository extends EmployeesRepository {

    static private CrewRepository instance = null;
    static public CrewRepository getInstance(){
        if(instance == null)instance = new CrewRepository();
        return instance;
    }

    final static public int ABK = 1;


    private ITypeConverter<Crew> employees2crewConverter;
    public CrewRepository(){

        super(DataCache.VERY_LONG_CACHE);

        employees2crewConverter = data -> {
            Crew crew =  new Crew();
            crew.read((DataObjectCollection)data);
            return crew;
        };
    }

    public DataStore<Crew> getCrew(){
        DataCache.CacheEntry<Crew> entry = cache.getCacheEntry("crew", employees2crewConverter);

        if(entry.hasExpired()) {
            service.getEmployees(ABK).enqueue(createCallback(entry));
        }

        return entry;
    }

    public DataStore<HashMap<String, Bitmap>> getProfilePics(Crew crew){
        return getProfilePics(new ArrayList<Employee>(crew));
    }
}
