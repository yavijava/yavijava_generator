package com.toastcoders.vmware.yavijava.writer

import org.junit.After
import org.junit.Before
import org.junit.Test

class WriteJavaClassFenceTest {

    File tempDir
    File outFile

    private static final String GENERATED_TEMPLATE = """\
// auto generated using yavijava_generator
package com.vmware.vim25.mo;

import com.vmware.vim25.*;

/* ===== BEGIN custom imports (preserved by regenerator) ===== */
/* ===== END custom imports ===== */

public class Sample {
    public Sample() { }

    /* ===== BEGIN custom (preserved by regenerator) ===== */
    /* ===== END custom ===== */
}
"""

    @Before void setUp() {
        tempDir = File.createTempDir()
        outFile = new File(tempDir, "Sample.java")
    }

    @After void tearDown() { tempDir.deleteDir() }

    @Test
    void testWritesFreshFileWithEmptyFences() {
        boolean ok = WriteJavaClass.writeFileWithFence(outFile.absolutePath, GENERATED_TEMPLATE)
        assert ok
        assert outFile.exists()
        assert outFile.text == GENERATED_TEMPLATE
    }

    @Test
    void testPreservesCustomCodeAcrossOverwrite() {
        // Stage existing file with custom code in the fence
        String withCustom = GENERATED_TEMPLATE.replace(
            "/* ===== BEGIN custom (preserved by regenerator) ===== */\n    /* ===== END custom ===== */",
            "/* ===== BEGIN custom (preserved by regenerator) ===== */\n    public boolean isReady() { return true; }\n    /* ===== END custom ===== */"
        )
        outFile.text = withCustom

        // Regenerate with empty fences
        WriteJavaClass.writeFileWithFence(outFile.absolutePath, GENERATED_TEMPLATE)

        // Custom code preserved
        assert outFile.text.contains("public boolean isReady() { return true; }")
    }

    @Test
    void testPreservesCustomImportsAcrossOverwrite() {
        String withImports = GENERATED_TEMPLATE.replace(
            "/* ===== BEGIN custom imports (preserved by regenerator) ===== */\n/* ===== END custom imports ===== */",
            "/* ===== BEGIN custom imports (preserved by regenerator) ===== */\nimport java.util.List;\n/* ===== END custom imports ===== */"
        )
        outFile.text = withImports
        WriteJavaClass.writeFileWithFence(outFile.absolutePath, GENERATED_TEMPLATE)
        assert outFile.text.contains("import java.util.List;")
    }

    @Test
    void testSkipsFileWithoutMarker() {
        outFile.text = "public class HandWritten {}"
        boolean ok = WriteJavaClass.writeFileWithFence(outFile.absolutePath, GENERATED_TEMPLATE)
        assert !ok
        assert outFile.text == "public class HandWritten {}"
    }

    @Test
    void testRefusesFileWithMismatchedFence() {
        // Marker present but only END custom — missing BEGIN
        String broken = """\
// auto generated using yavijava_generator
package com.vmware.vim25.mo;

public class Sample {
    /* ===== END custom ===== */
}
"""
        outFile.text = broken
        boolean ok = WriteJavaClass.writeFileWithFence(outFile.absolutePath, GENERATED_TEMPLATE)
        assert !ok
        assert outFile.text == broken  // untouched
    }

    @Test
    void testRoundTripIsLossless() {
        // Custom region with multi-line content; regen shouldn't munge it
        String multiline = """    /* ===== BEGIN custom (preserved by regenerator) ===== */
    public boolean a() { return true; }
    public boolean b() { return false; }
    /* ===== END custom ===== */"""
        String withMulti = GENERATED_TEMPLATE.replace(
            "/* ===== BEGIN custom (preserved by regenerator) ===== */\n    /* ===== END custom ===== */",
            multiline.trim()
        )
        outFile.text = withMulti
        WriteJavaClass.writeFileWithFence(outFile.absolutePath, GENERATED_TEMPLATE)
        assert outFile.text.contains("public boolean a() { return true; }")
        assert outFile.text.contains("public boolean b() { return false; }")
    }
}
