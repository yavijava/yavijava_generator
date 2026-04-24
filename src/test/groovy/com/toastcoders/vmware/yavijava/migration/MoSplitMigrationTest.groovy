package com.toastcoders.vmware.yavijava.migration

import org.junit.After
import org.junit.Before
import org.junit.Test

class MoSplitMigrationTest {

    File workDir
    File moDir
    File wsDir

    @Before void setUp() {
        workDir = File.createTempDir()
        moDir = new File(workDir, "com/vmware/vim25/mo")
        moDir.mkdirs()
        wsDir = new File(workDir, "com/vmware/vim25/ws")
        wsDir.mkdirs()
        ["PureBoilerplateMo.java", "CustomHelperMo.java", "AnnotatedMo.java", "AlreadyMigratedMo.java"]
            .each { name ->
                new File(moDir, name).text = new File("src/test/resources/migration/before/${name}").text
            }
        new File(wsDir, "VimStub.java").text = new File("src/test/resources/migration/before/VimStubFixture.java").text
    }

    @After void tearDown() { workDir.deleteDir() }

    @Test
    void testPureBoilerplateMoFullySplit() {
        new MoSplitMigration().run(workDir.absolutePath)
        File base = new File(moDir, "PureBoilerplateMoBase.java")
        File sub = new File(moDir, "PureBoilerplateMo.java")
        assert base.exists()
        assert base.text.contains("auto generated using yavijava_generator")
        assert base.text.contains("public String getName()")
        assert base.text.contains("public ManagedEntity getParent()")
        assert base.text.contains("public void reload()")
        assert sub.text.contains("extends PureBoilerplateMoBase")
        assert !sub.text.contains("public String getName()")
        assert new File(moDir, "PureBoilerplateMo.java.bak").exists()
    }

    @Test
    void testCustomHelperKeepsCustomMembersInSubclass() {
        new MoSplitMigration().run(workDir.absolutePath)
        File base = new File(moDir, "CustomHelperMoBase.java")
        File sub = new File(moDir, "CustomHelperMo.java")
        assert sub.text.contains("getAlarmActionEabled")
        assert !base.text.contains("getAlarmActionEabled")
        assert base.text.contains("getRecentTasks")
    }

    @Test
    void testAnnotatedMoPreservesJavadoc() {
        new MoSplitMigration().run(workDir.absolutePath)
        File base = new File(moDir, "AnnotatedMoBase.java")
        assert base.exists()
        assert base.text.contains("@since SDK4.0")
    }

    @Test
    void testAlreadyMigratedMoSkipped() {
        long mtime = new File(moDir, "AlreadyMigratedMo.java").lastModified()
        new MoSplitMigration().run(workDir.absolutePath)
        assert !new File(moDir, "AlreadyMigratedMoBase.java").exists()
        assert new File(moDir, "AlreadyMigratedMo.java").lastModified() == mtime
        assert !new File(moDir, "AlreadyMigratedMo.java.bak").exists()
    }

    @Test
    void testVimStubMarkerInserted() {
        new MoSplitMigration().run(workDir.absolutePath)
        String content = new File(wsDir, "VimStub.java").text
        assert content.contains("auto generated using yavijava_generator")
        int posPkg = content.indexOf("package com.vmware.vim25.ws;")
        int posMarker = content.indexOf("auto generated using yavijava_generator")
        assert posPkg >= 0 && posMarker > posPkg
    }

    @Test
    void testIdempotentRerun() {
        new MoSplitMigration().run(workDir.absolutePath)
        long mtimeBase = new File(moDir, "PureBoilerplateMoBase.java").lastModified()
        long mtimeSub  = new File(moDir, "PureBoilerplateMo.java").lastModified()
        Thread.sleep(1100)
        new MoSplitMigration().run(workDir.absolutePath)
        assert new File(moDir, "PureBoilerplateMoBase.java").lastModified() == mtimeBase
        assert new File(moDir, "PureBoilerplateMo.java").lastModified() == mtimeSub
    }
}
