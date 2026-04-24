package com.vmware.vim25.mo;

import com.vmware.vim25.*;

public class AnnotatedMo extends ManagedEntity {
    public AnnotatedMo(ServerConnection sc, ManagedObjectReference mor) {
        super(sc, mor);
    }
    /**
     * @since SDK4.0
     */
    public Tag[] getTag() {
        return (Tag[]) getCurrentProperty("tag");
    }
}
