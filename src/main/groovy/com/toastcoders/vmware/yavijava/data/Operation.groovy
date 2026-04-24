package com.toastcoders.vmware.yavijava.data

class Operation {
    String name
    List<OpParam> params = []
    String returnType
    boolean returnIsArray = false
    List<String> faults = []
}

class OpParam {
    String name
    String type
    boolean isArray = false
    boolean isOptional = false
}
