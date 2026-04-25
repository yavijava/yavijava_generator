package com.toastcoders.vmware.yavijava.generator

import com.github.javaparser.StaticJavaParser
import org.junit.After
import org.junit.Before
import org.junit.Test

class PyvmomiManagedObjectGeneratorTest {

    File tempDir
    String dest
    String schemaPath = "src/test/resources/pyvmomi/sample-schema.json"

    @Before void setUp() {
        tempDir = File.createTempDir()
        dest = tempDir.absolutePath + File.separator
    }

    @After void tearDown() { tempDir.deleteDir() }

    private void run() {
        new PyvmomiManagedObjectGenerator(schemaPath, dest).generate(true, "com.vmware.vim25.mo", [:])
    }

    @Test
    void testEmitsFileForVirtualMachine() {
        run()
        assert new File(dest, "VirtualMachine.java").exists()
    }

    @Test
    void testEmitsFileForHostSystem() {
        run()
        assert new File(dest, "HostSystem.java").exists()
    }

    @Test
    void testSkipsManagedEntityViaExcludeList() {
        run()
        // ManagedEntity is in MIGRATION_EXCLUDE; never written by the generator
        assert !new File(dest, "ManagedEntity.java").exists()
    }

    @Test
    void testGeneratedFileIsValidJava() {
        run()
        // Parse with JavaParser as the syntactic validator
        def cu = StaticJavaParser.parse(new File(dest, "VirtualMachine.java"))
        def cls = cu.getType(0)
        assert cls.nameAsString == "VirtualMachine"
        assert cls.extendedTypes[0].nameAsString == "ManagedEntity"
        assert cls.constructors.size() == 1
    }

    @Test
    void testGeneratedVirtualMachineHasMarker() {
        run()
        assert new File(dest, "VirtualMachine.java").text.contains("auto generated using yavijava_generator")
    }

    @Test
    void testGeneratedVirtualMachineHasFences() {
        run()
        String c = new File(dest, "VirtualMachine.java").text
        assert c.contains("BEGIN custom imports (preserved by regenerator)")
        assert c.contains("BEGIN custom (preserved by regenerator)")
    }

    @Test
    void testRegeneratePreservesCustomFenceContent() {
        run()
        // Inject custom code into the fence
        File vm = new File(dest, "VirtualMachine.java")
        String before = vm.text
        String withCustom = before.replace(
            "/* ===== BEGIN custom (preserved by regenerator) ===== */\n    /* ===== END custom ===== */",
            "/* ===== BEGIN custom (preserved by regenerator) ===== */\n    public boolean isReady() { return true; }\n    /* ===== END custom ===== */"
        )
        vm.text = withCustom
        run()  // second pass
        assert vm.text.contains("public boolean isReady() { return true; }")
    }
}
