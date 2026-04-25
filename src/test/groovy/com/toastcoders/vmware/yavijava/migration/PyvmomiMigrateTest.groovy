package com.toastcoders.vmware.yavijava.migration

import org.junit.After
import org.junit.Before
import org.junit.Test

class PyvmomiMigrateTest {

    File workDir
    File moDir

    @Before void setUp() {
        workDir = File.createTempDir()
        moDir = new File(workDir, "com/vmware/vim25/mo")
        moDir.mkdirs()
        new File(moDir, "VirtualMachine.java").text = new File("src/test/resources/migration/pyvmomi/CustomHelperMo.java").text
            .replace("CustomHelperMo", "VirtualMachine")
        new File(moDir, "HostSystem.java").text = new File("src/test/resources/migration/pyvmomi/PureBoilerplateMo.java").text
            .replace("PureBoilerplateMo", "HostSystem")
        new File(moDir, "AlreadyMigratedSample.java").text = new File("src/test/resources/migration/pyvmomi/AlreadyMigratedMo.java").text
            .replace("AlreadyMigratedMo", "AlreadyMigratedSample")
    }

    @After void tearDown() { workDir.deleteDir() }

    @Test
    void testAddsMarkerAndFencesToPureBoilerplate() {
        new PyvmomiMigrate().run(
            "src/test/resources/pyvmomi/sample-schema.json", workDir.absolutePath)
        String c = new File(moDir, "HostSystem.java").text
        assert c.contains("auto generated using yavijava_generator")
        assert c.contains("BEGIN custom imports (preserved by regenerator)")
        assert c.contains("BEGIN custom (preserved by regenerator)")
        assert c.contains("END custom")
    }

    @Test
    void testRelocatesCustomMembersIntoFence() {
        new PyvmomiMigrate().run(
            "src/test/resources/pyvmomi/sample-schema.json", workDir.absolutePath)
        String c = new File(moDir, "VirtualMachine.java").text
        // getAlarmActionEnabled is between BEGIN custom and END custom, not above
        int posBegin = c.indexOf("BEGIN custom (preserved by regenerator)")
        int posEnd   = c.lastIndexOf("END custom")
        int posCustom = c.indexOf("getAlarmActionEnabled")
        assert posBegin > 0 && posCustom > posBegin && posCustom < posEnd
    }

    @Test
    void testLeavesAutoGenMembersInPlace() {
        new PyvmomiMigrate().run(
            "src/test/resources/pyvmomi/sample-schema.json", workDir.absolutePath)
        String c = new File(moDir, "VirtualMachine.java").text
        // getName is auto-gen; should appear in main class body, not in fence
        int posClass = c.indexOf("public class VirtualMachine")
        int posBegin = c.indexOf("BEGIN custom (preserved by regenerator)")
        int posName  = c.indexOf("public String getName()")
        assert posClass > 0 && posBegin > 0 && posName > 0
        assert posName > posClass && posName < posBegin
    }

    @Test
    void testIdempotentOnAlreadyMigratedFile() {
        // Run once; file is unchanged because it already has marker + fences. We use a schema entry name.
        // Stage AlreadyMigratedSample under a name that's in the schema to exercise the path.
        new File(moDir, "VirtualMachine.java").text = new File("src/test/resources/migration/pyvmomi/AlreadyMigratedMo.java").text
            .replace("AlreadyMigratedMo", "VirtualMachine")
        long mtime = new File(moDir, "VirtualMachine.java").lastModified()
        Thread.sleep(1100)
        new PyvmomiMigrate().run("src/test/resources/pyvmomi/sample-schema.json", workDir.absolutePath)
        assert new File(moDir, "VirtualMachine.java").lastModified() == mtime
    }

    @Test
    void testSkipsMigrationExcludeList() {
        // Stage a ManagedEntity.java; migrate should leave it alone
        new File(moDir, "ManagedEntity.java").text = "public class ManagedEntity { }"
        long mtime = new File(moDir, "ManagedEntity.java").lastModified()
        Thread.sleep(1100)
        new PyvmomiMigrate().run("src/test/resources/pyvmomi/sample-schema.json", workDir.absolutePath)
        assert new File(moDir, "ManagedEntity.java").lastModified() == mtime
    }

    @Test
    void testIgnoresFilesNotInSchema() {
        // A random hand-written helper not in the schema must be untouched
        new File(moDir, "HandWrittenThing.java").text = "public class HandWrittenThing { }"
        new PyvmomiMigrate().run("src/test/resources/pyvmomi/sample-schema.json", workDir.absolutePath)
        assert new File(moDir, "HandWrittenThing.java").text == "public class HandWrittenThing { }"
    }
}
