package com.toastcoders.vmware.yavijava.dtm

import org.junit.Test

class MoTypeInfoParserTest {

    private MoTypeInfo parseFixture() {
        File xml = new File("src/test/resources/dtm/sample-querytypeinfo-response.xml")
        return new MoTypeInfoParser().parseSingle(xml.text)
    }

    @Test
    void testTypeNameAndParent() {
        MoTypeInfo t = parseFixture()
        assert t.name == "VirtualMachine"
        assert t.parent == "ManagedEntity"
    }

    @Test
    void testParsesAllProperties() {
        MoTypeInfo t = parseFixture()
        assert t.properties*.name as Set == ["runtime", "parent", "recentTask"] as Set
    }

    @Test
    void testRuntimePropertyIsDataObject() {
        MoTypeInfo t = parseFixture()
        def p = t.properties.find { it.name == "runtime" }
        assert p.type == "VirtualMachineRuntimeInfo"
        assert p.referencedMoType == null
        assert !p.isArray
        assert !p.isOptional
    }

    @Test
    void testParentPropertyIsSingleMor() {
        MoTypeInfo t = parseFixture()
        def p = t.properties.find { it.name == "parent" }
        assert p.type == "ManagedObjectReference"
        assert p.referencedMoType == "ManagedEntity"
        assert !p.isArray
        assert p.isOptional
    }

    @Test
    void testRecentTaskPropertyIsArrayOfMor() {
        MoTypeInfo t = parseFixture()
        def p = t.properties.find { it.name == "recentTask" }
        assert p.type == "ManagedObjectReference"
        assert p.referencedMoType == "Task"
        assert p.isArray
    }

    @Test
    void testParsesAllMethods() {
        MoTypeInfo t = parseFixture()
        assert t.methods*.name as Set == ["PowerOnVM_Task", "Reload"] as Set
    }

    @Test
    void testReloadMethodIsVoidNoFaults() {
        MoTypeInfo t = parseFixture()
        def m = t.methods.find { it.name == "Reload" }
        assert m.returnType == "void"
        assert m.params == []
        assert m.faults == []
    }

    @Test
    void testPowerOnVmHasParamReturnAndFaults() {
        MoTypeInfo t = parseFixture()
        def m = t.methods.find { it.name == "PowerOnVM_Task" }
        assert m.params.size() == 1
        assert m.params[0].name == "host"
        assert m.params[0].type == "ManagedObjectReference"
        assert m.params[0].referencedMoType == "HostSystem"
        assert m.params[0].isOptional
        assert m.returnType == "ManagedObjectReference"
        assert m.referencedReturnMoType == "Task"
        assert m.faults as Set == ["TaskInProgress", "InvalidState", "FileFault"] as Set
    }
}
