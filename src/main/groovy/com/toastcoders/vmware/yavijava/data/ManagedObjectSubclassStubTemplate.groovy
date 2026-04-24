package com.toastcoders.vmware.yavijava.data

class ManagedObjectSubclassStubTemplate {

    static String render(String moTypeName) {
        return """\
package com.vmware.vim25.mo;

import com.vmware.vim25.ManagedObjectReference;

public class ${moTypeName} extends ${moTypeName}Base {
    public ${moTypeName}(ServerConnection serverConnection, ManagedObjectReference mor) {
        super(serverConnection, mor);
    }
}
"""
    }
}
