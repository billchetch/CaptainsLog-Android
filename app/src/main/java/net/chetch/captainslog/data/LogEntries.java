package net.chetch.captainslog.data;

import net.chetch.webservices.DataObjectCollection;
import net.chetch.webservices.Webservice;

public class LogEntries extends DataObjectCollection<LogEntry> {
    public LogEntries(){
        super(LogEntries.class);
    }
}
