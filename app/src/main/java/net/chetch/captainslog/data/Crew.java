package net.chetch.captainslog.data;

import net.chetch.webservices.employees.Employees;

public class Crew extends Employees<CrewMember> {
    public Crew(){
        super(Crew.class, CrewMember.class);
    }

}
