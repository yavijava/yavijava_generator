package com.vmware.vim25.mo;

import com.vmware.vim25.*;
import java.rmi.RemoteException;

public class PureBoilerplateMo extends ManagedEntity {
    public PureBoilerplateMo(ServerConnection sc, ManagedObjectReference mor) {
        super(sc, mor);
    }
    public String getName() {
        return (String) getCurrentProperty("name");
    }
    public ManagedEntity getParent() {
        return (ManagedEntity) this.getManagedObject("parent");
    }
    public void reload() throws RuntimeFault, RemoteException {
        getVimService().reload(getMOR());
    }
}
