package com.toastcoders.vmware.yavijava.pyvmomi

class PyvmomiMethod {
    String name
    String wsdlName
    String version
    List<PyvmomiParam> params = []
    String returnType
    boolean returnIsArray = false
    boolean returnIsManagedObjectReference = false
    List<String> faults = []
    String privilegeId
}
