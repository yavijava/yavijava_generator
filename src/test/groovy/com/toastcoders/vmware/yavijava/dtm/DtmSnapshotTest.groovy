package com.toastcoders.vmware.yavijava.dtm

import org.junit.After
import org.junit.Before
import org.junit.Test

class DtmSnapshotTest {

    File tempFile

    @Before void setUp()    { tempFile = File.createTempFile("dtm-snap", ".json") }
    @After  void tearDown() { tempFile.delete() }

    @Test
    void testReadFixtureSnapshot() {
        Map<String, MoTypeInfo> types = new DtmSnapshot().read(
            new File("src/test/resources/dtm/sample-mo-types.json"))
        assert types.keySet() == ["ManagedEntity", "VirtualMachine", "HostSystem"] as Set
        assert types["VirtualMachine"].parent == "ManagedEntity"
        assert types["VirtualMachine"].properties.size() == 3
        def runtime = types["VirtualMachine"].properties.find { it.name == "runtime" }
        assert runtime.type == "VirtualMachineRuntimeInfo"
    }

    @Test
    void testRoundTripIsLossless() {
        Map<String, MoTypeInfo> read = new DtmSnapshot().read(
            new File("src/test/resources/dtm/sample-mo-types.json"))
        new DtmSnapshot().write(tempFile, read, "9.0.0-fixture")
        Map<String, MoTypeInfo> roundtripped = new DtmSnapshot().read(tempFile)
        assert roundtripped.keySet() == read.keySet()
        assert roundtripped["VirtualMachine"].properties.size() == 3
        def m = roundtripped["VirtualMachine"].methods.find { it.name == "PowerOnVM_Task" }
        assert m.faults == ["FileFault", "InvalidState", "TaskInProgress"]
        assert m.referencedReturnMoType == "Task"
    }

    @Test
    void testWriteIsAlphabeticalByTypeName() {
        Map<String, MoTypeInfo> read = new DtmSnapshot().read(
            new File("src/test/resources/dtm/sample-mo-types.json"))
        new DtmSnapshot().write(tempFile, read, "9.0.0-fixture")
        String content = tempFile.text
        int posHost = content.indexOf("\"name\": \"HostSystem\"")
        int posMe   = content.indexOf("\"name\": \"ManagedEntity\"")
        int posVm   = content.indexOf("\"name\": \"VirtualMachine\"")
        assert posHost > 0 && posMe > posHost && posVm > posMe
    }

    @Test(expected = RuntimeException)
    void testRejectsWrongSchemaVersion() {
        tempFile.text = '{"schemaVersion": 999, "types": []}'
        new DtmSnapshot().read(tempFile)
    }

    @Test(expected = RuntimeException)
    void testRejectsCorruptJson() {
        tempFile.text = 'not json {{{'
        new DtmSnapshot().read(tempFile)
    }
}
