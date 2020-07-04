package net.chetch.captainslog.models;

import net.chetch.captainslog.data.CrewMember;

public class MainViewModel extends GenericViewModel {

    public MainViewModel(){
        super();

        CrewMember.onDutyLimit = 60*10; //60*2; //in secos
    }
}
