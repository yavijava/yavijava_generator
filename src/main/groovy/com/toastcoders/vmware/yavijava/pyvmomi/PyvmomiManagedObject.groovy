package com.toastcoders.vmware.yavijava.pyvmomi

class PyvmomiManagedObject {
    String name
    String wsdlName
    String qualifiedName
    String parent
    String version
    String introducedIn
    List<PyvmomiProperty> properties = []
    List<PyvmomiMethod> methods = []
}
