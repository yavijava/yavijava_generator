package com.toastcoders.vmware.yavijava.pyvmomi

import org.junit.After
import org.junit.Before
import org.junit.Test

class PyvmomiSchemaTest {

    File tempFile
    File fixture = new File("src/test/resources/pyvmomi/sample-schema.json")

    @Before void setUp()    { tempFile = File.createTempFile("pv-schema", ".json") }
    @After  void tearDown() { tempFile.delete() }

    @Test
    void testLoadsAllManagedObjects() {
        Map<String, PyvmomiManagedObject> mos = new PyvmomiSchema().read(fixture)
        assert mos.keySet() == ["HostSystem", "ManagedEntity", "VirtualMachine"] as Set
    }

    @Test
    void testManagedEntityHasCorrectParent() {
        Map<String, PyvmomiManagedObject> mos = new PyvmomiSchema().read(fixture)
        assert mos["VirtualMachine"].parent == "ManagedEntity"
    }

    @Test
    void testParsesPropertyFlags() {
        PyvmomiProperty parent = new PyvmomiSchema().read(fixture)["VirtualMachine"].properties.find { it.name == "parent" }
        assert parent.type == "ManagedEntity"
        assert parent.isManagedObjectReference
        assert parent.isOptional
        assert !parent.isArray
    }

    @Test
    void testParsesArrayMorProperty() {
        PyvmomiProperty rt = new PyvmomiSchema().read(fixture)["VirtualMachine"].properties.find { it.name == "recentTask" }
        assert rt.isArray
        assert rt.isManagedObjectReference
        assert rt.type == "Task"
    }

    @Test
    void testParsesMethodWithMorReturnAndOptionalParam() {
        PyvmomiMethod m = new PyvmomiSchema().read(fixture)["VirtualMachine"].methods.find { it.name == "powerOnVM_Task" }
        assert m.wsdlName == "PowerOnVM_Task"
        assert m.returnType == "Task"
        assert m.returnIsManagedObjectReference
        assert !m.returnIsArray
        assert m.params.size() == 1
        assert m.params[0].name == "host"
        assert m.params[0].isOptional
        assert m.params[0].isManagedObjectReference
        assert m.faults as Set == ["FileFault", "InvalidState", "TaskInProgress"] as Set
    }

    @Test
    void testParsesVoidMethodWithNoParams() {
        PyvmomiMethod m = new PyvmomiSchema().read(fixture)["VirtualMachine"].methods.find { it.name == "reload" }
        assert m.returnType == "void"
        assert m.params.size() == 0
        assert m.faults == []
    }

    @Test(expected = RuntimeException)
    void testRejectsWrongSchemaVersion() {
        tempFile.text = '{"schemaVersion": 999, "managedObjects": []}'
        new PyvmomiSchema().read(tempFile)
    }

    @Test(expected = RuntimeException)
    void testRejectsCorruptJson() {
        tempFile.text = 'not json {{{'
        new PyvmomiSchema().read(tempFile)
    }
}
