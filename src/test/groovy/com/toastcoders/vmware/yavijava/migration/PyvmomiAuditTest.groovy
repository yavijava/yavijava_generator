package com.toastcoders.vmware.yavijava.migration

import org.junit.After
import org.junit.Before
import org.junit.Test

class PyvmomiAuditTest {

    File workDir
    File moDir

    @Before void setUp() {
        workDir = File.createTempDir()
        moDir = new File(workDir, "com/vmware/vim25/mo")
        moDir.mkdirs()
        // Stage fixtures with names matching schema entries
        new File(moDir, "VirtualMachine.java").text = new File("src/test/resources/migration/pyvmomi/CustomHelperMo.java").text
            .replace("CustomHelperMo", "VirtualMachine")
        new File(moDir, "HostSystem.java").text = new File("src/test/resources/migration/pyvmomi/PureBoilerplateMo.java").text
            .replace("PureBoilerplateMo", "HostSystem")
    }

    @After void tearDown() { workDir.deleteDir() }

    @Test
    void testReportsAllAutoGenForPureBoilerplate() {
        PyvmomiAudit.AuditReport r = new PyvmomiAudit().run(
            "src/test/resources/pyvmomi/sample-schema.json", workDir.absolutePath)
        def host = r.perMo.find { it.name == "HostSystem" }
        assert host != null
        assert host.customMemberNames == []
    }

    @Test
    void testFlagsCustomMemberOnCustomHelper() {
        PyvmomiAudit.AuditReport r = new PyvmomiAudit().run(
            "src/test/resources/pyvmomi/sample-schema.json", workDir.absolutePath)
        def vm = r.perMo.find { it.name == "VirtualMachine" }
        assert vm != null
        assert vm.customMemberNames as Set == ["getAlarmActionEnabled"] as Set
    }

    @Test
    void testSkipsManagedEntityViaExcludeList() {
        PyvmomiAudit.AuditReport r = new PyvmomiAudit().run(
            "src/test/resources/pyvmomi/sample-schema.json", workDir.absolutePath)
        assert r.perMo.find { it.name == "ManagedEntity" } == null
    }

    @Test
    void testReportsMissingExistingFileAsNotPresent() {
        // VirtualMachine staged, HostSystem NOT staged
        new File(moDir, "VirtualMachine.java").delete()
        new File(moDir, "HostSystem.java").delete()
        PyvmomiAudit.AuditReport r = new PyvmomiAudit().run(
            "src/test/resources/pyvmomi/sample-schema.json", workDir.absolutePath)
        // No entries because no files exist
        assert r.perMo.isEmpty()
    }

    @Test
    void testIsReadOnly() {
        long mtimeBefore = new File(moDir, "VirtualMachine.java").lastModified()
        new PyvmomiAudit().run("src/test/resources/pyvmomi/sample-schema.json", workDir.absolutePath)
        assert new File(moDir, "VirtualMachine.java").lastModified() == mtimeBefore
        // No .bak created
        assert !new File(moDir, "VirtualMachine.java.bak").exists()
    }
}
