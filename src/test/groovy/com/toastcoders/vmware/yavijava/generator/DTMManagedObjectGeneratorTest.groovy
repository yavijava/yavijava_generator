package com.toastcoders.vmware.yavijava.generator

import org.junit.After
import org.junit.Before
import org.junit.Test

class DTMManagedObjectGeneratorTest {

    File tempDir
    String dest
    String snapshotPath = "src/test/resources/dtm/sample-mo-types.json"

    @Before void setUp() {
        tempDir = File.createTempDir()
        dest = tempDir.absolutePath + File.separator
    }

    @After void tearDown() { tempDir.deleteDir() }

    private void run() {
        new DTMManagedObjectGenerator(snapshotPath, dest, null, null, null, false).generate(true, "com.vmware.vim25.mo", [:])
    }

    @Test
    void testEmitsBaseFilesForEachType() {
        run()
        assert !new File(dest, "ManagedEntityBase.java").exists()
        assert new File(dest, "VirtualMachineBase.java").exists()
        assert new File(dest, "HostSystemBase.java").exists()
    }

    @Test
    void testEmitsSubclassStubWhenMissing() {
        run()
        assert new File(dest, "VirtualMachine.java").exists()
        assert new File(dest, "VirtualMachine.java").text.contains(
            "public class VirtualMachine extends VirtualMachineBase {")
    }

    @Test
    void testDoesNotOverwriteExistingSubclass() {
        File existing = new File(dest, "VirtualMachine.java")
        existing.text = "public class VirtualMachine /* hand-written */ {}"
        run()
        assert existing.text == "public class VirtualMachine /* hand-written */ {}"
    }

    @Test
    void testBaseClassExtendsParentDirectly() {
        run()
        String content = new File(dest, "VirtualMachineBase.java").text
        assert content.contains("public class VirtualMachineBase extends ManagedEntity {")
    }

    @Test
    void testBaseHasMarker() {
        run()
        assert new File(dest, "VirtualMachineBase.java").text.contains(
            "auto generated using yavijava_generator")
    }

    @Test
    void testStubHasNoMarker() {
        run()
        assert !new File(dest, "VirtualMachine.java").text.contains(
            "auto generated using yavijava_generator")
    }

    @Test
    void testPropertyAccessorsAreAlphabeticalAndCorrectShape() {
        run()
        String c = new File(dest, "VirtualMachineBase.java").text
        int posParent  = c.indexOf("public ManagedEntity getParent()")
        int posRecent  = c.indexOf("public Task[] getRecentTask()")
        int posRuntime = c.indexOf("public VirtualMachineRuntimeInfo getRuntime()")
        assert posParent > 0 && posRecent > 0 && posRuntime > 0
        assert posParent < posRecent && posRecent < posRuntime
        assert c.contains("return (ManagedEntity) this.getManagedObject(\"parent\");")
        assert c.contains("return getTasks(\"recentTask\");")
        assert c.contains("return (VirtualMachineRuntimeInfo) getCurrentProperty(\"runtime\");")
    }

    @Test
    void testMethodWrapperReturnsTaskInstance() {
        run()
        String c = new File(dest, "VirtualMachineBase.java").text
        assert c.contains(
            "public Task powerOnVM_Task(HostSystem host) " +
            "throws FileFault, InvalidState, TaskInProgress, RuntimeFault, RemoteException {")
        assert c.contains(
            "ManagedObjectReference resultMor = getVimService().powerOnVM_Task(getMOR(), " +
            "host == null ? null : host.getMOR());")
        assert c.contains("return new Task(getServerConnection(), resultMor);")
    }
}
