package com.toastcoders.vmware.yavijava.generator

import org.junit.After
import org.junit.Before
import org.junit.Test

public class WSDLDataObjectGeneratorTest {

    File tempDir
    WSDLDataObjectGenerator generator

    @Before
    void setUp() {
        tempDir = File.createTempDir()
        generator = new WSDLDataObjectGenerator(
            'src/test/resources/wsdl/test-vim.wsdl',
            tempDir.absolutePath + File.separator
        )
        generator.generate(true, "com.vmware.vim25", [vim25: 'urn:vim25'])
    }

    @After
    void tearDown() { tempDir.deleteDir() }

    // === Data object (extension-style) outputs ===

    @Test
    void testGeneratesBatchResultJavaFile() {
        assert new File(tempDir, "BatchResult.java").exists()
    }

    @Test
    void testGeneratesTaskInfoFromIncludedXsd() {
        assert new File(tempDir, "TaskInfo.java").exists()
    }

    @Test
    void testGeneratesReflectInfoFromImportedXsd() {
        assert new File(tempDir, "ReflectInfo.java").exists()
    }

    @Test
    void testDataObjectFileContainsExpectedShape() {
        String content = new File(tempDir, "BatchResult.java").text
        assert content.contains("package com.vmware.vim25")
        assert content.contains("import lombok.Getter;")
        assert content.contains("import lombok.Setter;")
        assert content.contains("public class BatchResult extends DynamicData")
        assert content.contains("@Getter @Setter public String result;")
    }

    @Test
    void testDataObjectFileContainsGeneratorMarker() {
        String content = new File(tempDir, "BatchResult.java").text
        assert content.contains("auto generated using yavijava_generator")
    }

    // === ArrayOf outputs ===

    @Test
    void testGeneratesArrayOfBatchResultJavaFile() {
        assert new File(tempDir, "ArrayOfBatchResult.java").exists()
    }

    @Test
    void testArrayOfFileContainsExpectedShape() {
        String content = new File(tempDir, "ArrayOfBatchResult.java").text
        assert content.contains("package com.vmware.vim25")
        assert content.contains("public class ArrayOfBatchResult {")
        assert content.contains("public BatchResult[] BatchResult;")
        assert content.contains("public BatchResult[] getBatchResult()")
        assert content.contains("public BatchResult getBatchResult(int i)")
        assert content.contains("public void setBatchResult(BatchResult[] BatchResult)")
    }

    @Test
    void testArrayOfFileDoesNotImportLombok() {
        String content = new File(tempDir, "ArrayOfBatchResult.java").text
        assert !content.contains("import lombok")
    }

    @Test
    void testArrayOfFileContainsGeneratorMarker() {
        String content = new File(tempDir, "ArrayOfBatchResult.java").text
        assert content.contains("auto generated using yavijava_generator")
    }

    // === Marker protection ===

    @Test
    void testHandWrittenFileIsNotOverwritten() {
        File handWritten = new File(tempDir, "BatchResult.java")
        // Pretend BatchResult.java already exists as a hand-written file (no marker)
        handWritten.text = "// hand-written, no marker"
        // Re-generate
        generator.generate(true, "com.vmware.vim25", [vim25: 'urn:vim25'])
        assert handWritten.text == "// hand-written, no marker"
    }

    @Test
    void testGeneratedFileIsOverwrittenOnRegeneration() {
        File generated = new File(tempDir, "BatchResult.java")
        String firstContent = generated.text
        // Mutate slightly — but keep the marker — to simulate stale generation
        generated.text = firstContent + "\n// stale extra"
        // Re-generate
        generator.generate(true, "com.vmware.vim25", [vim25: 'urn:vim25'])
        assert !generated.text.contains("stale extra")
    }
}
