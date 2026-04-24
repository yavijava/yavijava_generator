package com.toastcoders.vmware.yavijava.dtm

class MoMethod {
    String name
    List<MoMethodParam> params = []
    String returnType
    String referencedReturnMoType
    boolean returnIsArray = false
    List<String> faults = []
}
