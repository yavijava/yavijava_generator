package com.toastcoders.vmware.yavijava.dtm

import org.junit.Test

class TypeMapperTest {

    TypeMapper m = new TypeMapper()

    @Test void testPrimitiveString()  { assert m.toJavaType("xsd:string", false, false)  == "String" }
    @Test void testPrimitiveInt()     { assert m.toJavaType("xsd:int", false, false)     == "int" }
    @Test void testPrimitiveLong()    { assert m.toJavaType("xsd:long", false, false)    == "long" }
    @Test void testPrimitiveBoolean() { assert m.toJavaType("xsd:boolean", false, false) == "boolean" }
    @Test void testPrimitiveShort()   { assert m.toJavaType("xsd:short", false, false)   == "short" }
    @Test void testPrimitiveByte()    { assert m.toJavaType("xsd:byte", false, false)    == "byte" }
    @Test void testPrimitiveFloat()   { assert m.toJavaType("xsd:float", false, false)   == "float" }
    @Test void testPrimitiveDouble()  { assert m.toJavaType("xsd:double", false, false)  == "double" }

    @Test void testDateTimeBecomesCalendar() {
        assert m.toJavaType("xsd:dateTime", false, false) == "Calendar"
    }

    @Test void testAnyTypeBecomesObject() {
        assert m.toJavaType("xsd:anyType", false, false) == "Object"
    }

    @Test void testVim25TypePassesThrough() {
        assert m.toJavaType("VirtualMachineRuntimeInfo", false, false) == "VirtualMachineRuntimeInfo"
    }

    @Test void testArrayAppendsBrackets() {
        assert m.toJavaType("xsd:string", true, false) == "String[]"
        assert m.toJavaType("VirtualMachineRuntimeInfo", true, false) == "VirtualMachineRuntimeInfo[]"
    }

    @Test void testOptionalPrimitiveBoxes() {
        assert m.toJavaType("xsd:int", false, true) == "Integer"
        assert m.toJavaType("xsd:boolean", false, true) == "Boolean"
        assert m.toJavaType("xsd:long", false, true) == "Long"
    }

    @Test void testOptionalNonPrimitiveUnchanged() {
        assert m.toJavaType("xsd:string", false, true) == "String"
        assert m.toJavaType("VirtualMachineRuntimeInfo", false, true) == "VirtualMachineRuntimeInfo"
    }

    @Test void testArrayOfPrimitiveDoesNotBoxElement() {
        assert m.toJavaType("xsd:int", true, false) == "int[]"
    }

    @Test void testTypedHelperLookup() {
        assert m.typedHelperFor("Task") == "getTasks"
        assert m.typedHelperFor("Datastore") == "getDatastores"
        assert m.typedHelperFor("HostSystem") == "getHosts"
        assert m.typedHelperFor("VirtualMachine") == "getVms"
        assert m.typedHelperFor("Network") == "getNetworks"
        assert m.typedHelperFor("ResourcePool") == "getResourcePools"
        assert m.typedHelperFor("ScheduledTask") == "getScheduledTasks"
        assert m.typedHelperFor("View") == "getViews"
        assert m.typedHelperFor("PropertyFilter") == "getFilter"
    }

    @Test void testNoTypedHelperReturnsNull() {
        assert m.typedHelperFor("VirtualApp") == null
        assert m.typedHelperFor("AnyOtherMo") == null
    }

    @Test void testIsPrimitive() {
        assert m.isPrimitive("xsd:int")
        assert m.isPrimitive("xsd:boolean")
        assert !m.isPrimitive("xsd:string")
        assert !m.isPrimitive("VirtualMachine")
    }
}
