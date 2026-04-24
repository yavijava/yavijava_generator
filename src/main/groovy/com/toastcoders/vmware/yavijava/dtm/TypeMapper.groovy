package com.toastcoders.vmware.yavijava.dtm

class TypeMapper {

    private static final Map<String, String> PRIMITIVE = [
        "xsd:string" : "String",
        "xsd:int"    : "int",
        "xsd:long"   : "long",
        "xsd:boolean": "boolean",
        "xsd:short"  : "short",
        "xsd:byte"   : "byte",
        "xsd:float"  : "float",
        "xsd:double" : "double",
    ]

    private static final Map<String, String> BOXED = [
        "int"     : "Integer",
        "long"    : "Long",
        "boolean" : "Boolean",
        "short"   : "Short",
        "byte"    : "Byte",
        "float"   : "Float",
        "double"  : "Double",
    ]

    private static final Map<String, String> TYPED_HELPERS = [
        "Task"           : "getTasks",
        "Datastore"      : "getDatastores",
        "HostSystem"     : "getHosts",
        "VirtualMachine" : "getVms",
        "Network"        : "getNetworks",
        "ResourcePool"   : "getResourcePools",
        "ScheduledTask"  : "getScheduledTasks",
        "View"           : "getViews",
        "PropertyFilter" : "getFilter",
    ]

    String toJavaType(String sourceType, boolean isArray, boolean isOptional) {
        String base = mapBase(sourceType)
        if (isArray) return base + "[]"
        if (isOptional && isPrimitiveJavaType(base)) return BOXED[base]
        return base
    }

    String typedHelperFor(String moTypeName) {
        return TYPED_HELPERS[moTypeName]
    }

    boolean isPrimitive(String sourceType) {
        String mapped = PRIMITIVE[sourceType]
        return mapped != null && isPrimitiveJavaType(mapped)
    }

    private String mapBase(String sourceType) {
        if (PRIMITIVE.containsKey(sourceType)) return PRIMITIVE[sourceType]
        if (sourceType == "xsd:dateTime") return "Calendar"
        if (sourceType == "xsd:anyType") return "Object"
        if (sourceType.contains(":")) return sourceType.split(":")[1]
        return sourceType
    }

    private boolean isPrimitiveJavaType(String javaType) {
        return BOXED.containsKey(javaType)
    }
}
