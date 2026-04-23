package com.toastcoders.vmware.yavijava.generator

import org.junit.After
import org.junit.Before
import org.junit.Test

public class WSDLEnumGeneratorTest {

    File tempDir
    WSDLEnumGenerator generator

    @Before
    void setUp() {
        tempDir = File.createTempDir()
        generator = new WSDLEnumGenerator(
            'src/test/resources/wsdl/test-vim.wsdl',
            tempDir.absolutePath + File.separator
        )
        generator.generate(true, "com.vmware.vim25", [vim25: 'urn:vim25'])
    }

    @After
    void tearDown() { tempDir.deleteDir() }

    @Test
    void testGeneratesTaskInfoStateJavaFile() {
        assert new File(tempDir, "TaskInfoState.java").exists()
    }

    @Test
    void testGeneratesVirtualMachinePowerStateFromIncludedXsd() {
        assert new File(tempDir, "VirtualMachinePowerState.java").exists()
    }

    @Test
    void testEnumFileContainsExpectedShape() {
        String content = new File(tempDir, "TaskInfoState.java").text
        assert content.contains("package com.vmware.vim25")
        assert content.contains("public enum TaskInfoState")
        assert content.contains('queued("queued"),')
        assert content.contains('error("error");')
    }

    @Test
    void testEnumFileContainsGeneratorMarker() {
        String content = new File(tempDir, "TaskInfoState.java").text
        assert content.contains("auto generated using yavijava_generator")
    }

    @Test
    void testDataObjectsAreNotGenerated() {
        assert !new File(tempDir, "BatchResult.java").exists()
    }

    @Test
    void testHandWrittenEnumIsNotOverwritten() {
        File handWritten = new File(tempDir, "TaskInfoState.java")
        handWritten.text = "// hand-written enum, no marker"
        generator.generate(true, "com.vmware.vim25", [vim25: 'urn:vim25'])
        assert handWritten.text == "// hand-written enum, no marker"
    }
}
