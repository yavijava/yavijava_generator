package com.vmware.vim25.mo;

import com.vmware.vim25.*;
import java.rmi.RemoteException;

public class CustomHelperMo extends ManagedEntity {
    public CustomHelperMo(ServerConnection sc, ManagedObjectReference mor) {
        super(sc, mor);
    }
    public String getName() {
        return (String) getCurrentProperty("name");
    }
    public Task[] getRecentTasks() {
        return getTasks("recentTask");
    }
    public boolean getAlarmActionEabled() {
        Boolean aae = (Boolean) getCurrentProperty("alarmActionsEnabled");
        return aae == null ? false : aae.booleanValue();
    }
}
