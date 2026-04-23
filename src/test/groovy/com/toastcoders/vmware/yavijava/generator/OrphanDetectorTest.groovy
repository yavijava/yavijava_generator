package com.toastcoders.vmware.yavijava.generator

import com.toastcoders.vmware.yavijava.writer.WriteJavaClass
import org.junit.After
import org.junit.Before
import org.junit.Test

public class OrphanDetectorTest {

    File tempDir

    @Before
    void setUp() { tempDir = File.createTempDir() }

    @After
    void tearDown() { tempDir.deleteDir() }

    @Test
    void testReportsMarkerFileNotInGeneratedSet() {
        new File(tempDir, "DeprecatedType.java").text =
            "// ${WriteJavaClass.GENERATOR_MARKER}\npublic class DeprecatedType {}"
        Set<String> generated = ["StillCurrent.java"] as Set
        List<File> orphans = new OrphanDetector().findOrphans(tempDir, generated)
        assert orphans.size() == 1
        assert orphans[0].name == "DeprecatedType.java"
    }

    @Test
    void testIgnoresFilesInGeneratedSet() {
        new File(tempDir, "Current.java").text =
            "// ${WriteJavaClass.GENERATOR_MARKER}\npublic class Current {}"
        Set<String> generated = ["Current.java"] as Set
        List<File> orphans = new OrphanDetector().findOrphans(tempDir, generated)
        assert orphans.isEmpty()
    }

    @Test
    void testIgnoresHandWrittenFiles() {
        new File(tempDir, "HandWritten.java").text = "// no marker here"
        Set<String> generated = [] as Set
        List<File> orphans = new OrphanDetector().findOrphans(tempDir, generated)
        assert orphans.isEmpty()
    }

    @Test
    void testIgnoresNonJavaFiles() {
        new File(tempDir, "README.md").text = "// ${WriteJavaClass.GENERATOR_MARKER}"
        Set<String> generated = [] as Set
        List<File> orphans = new OrphanDetector().findOrphans(tempDir, generated)
        assert orphans.isEmpty()
    }
}
