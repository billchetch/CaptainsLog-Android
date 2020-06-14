package net.chetch.captainslog.data;

import net.chetch.webservices.DataObjectCollection;

public class LogEntries extends DataObjectCollection<LogEntry> {
    public LogEntries(){
        super(LogEntries.class);
    }
}
