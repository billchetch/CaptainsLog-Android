package net.chetch.captainslog.data;

import net.chetch.webservices.employees.Employee;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ICaptainsLogService {
    @GET("entries")
    Call<LogEntries> getEntries();

    @GET("entries")
    Call<LogEntries> getEntriesByPage(@Query("pagenumber") int pageNumber, @Query("pagesize") int pageSize);

    @PUT("entry/{id}")
    Call<LogEntry> putEntry(@Body LogEntry logEntry, @Path("id") int entryID);

    @DELETE("entry/{id}")
    Call<Integer> deleteEntry(@Path("id") int entryID);
}
