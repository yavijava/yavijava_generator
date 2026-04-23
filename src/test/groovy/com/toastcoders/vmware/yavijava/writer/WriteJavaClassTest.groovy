package com.toastcoders.vmware.yavijava.writer

import org.junit.After
import org.junit.Before
import org.junit.Test

public class WriteJavaClassTest {

    File tempDir

    @Before
    void setUp() { tempDir = File.createTempDir() }

    @After
    void tearDown() { tempDir.deleteDir() }

    @Test
    void testWritesNewFile() {
        File target = new File(tempDir, "New.java")
        WriteJavaClass.writeFile(target.absolutePath, "content")
        assert target.exists()
        assert target.text == "content"
    }

    @Test
    void testOverwritesFileWithMarker() {
        File target = new File(tempDir, "Generated.java")
        target.text = "// old\n// auto generated using yavijava_generator\n// stale body"
        WriteJavaClass.writeFile(target.absolutePath, "// new content")
        assert target.text == "// new content"
    }

    @Test
    void testSkipsFileWithoutMarker() {
        File target = new File(tempDir, "HandWritten.java")
        String original = "public class HandWritten { /* custom code */ }"
        target.text = original
        WriteJavaClass.writeFile(target.absolutePath, "// would clobber hand-written code")
        assert target.text == original
    }

    @Test
    void testReturnsTrueOnWrite() {
        File target = new File(tempDir, "New.java")
        boolean wrote = WriteJavaClass.writeFile(target.absolutePath, "content")
        assert wrote
    }

    @Test
    void testReturnsFalseOnSkip() {
        File target = new File(tempDir, "HandWritten.java")
        target.text = "no marker here"
        boolean wrote = WriteJavaClass.writeFile(target.absolutePath, "new content")
        assert !wrote
    }
}
