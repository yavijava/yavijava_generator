package com.toastcoders.vmware.yavijava.generator

import org.junit.After
import org.junit.Before
import org.junit.Test

class WSDLVimStubGeneratorTest {

    File tempDir
    String dest

    @Before void setUp() {
        tempDir = File.createTempDir()
        dest = tempDir.absolutePath + File.separator
    }

    @After void tearDown() { tempDir.deleteDir() }

    @Test
    void testGeneratesVimStubFile() {
        new WSDLVimStubGenerator(
            "src/test/resources/wsdl/test-vim-operations.wsdl", dest
        ).generate(true, "com.vmware.vim25", [vim25: 'urn:vim25'])

        File out = new File(dest, "VimStub.java")
        assert out.exists()
    }

    @Test
    void testFileHasMarker() {
        new WSDLVimStubGenerator(
            "src/test/resources/wsdl/test-vim-operations.wsdl", dest
        ).generate(true, "com.vmware.vim25", [vim25: 'urn:vim25'])

        String content = new File(dest, "VimStub.java").text
        assert content.contains("auto generated using yavijava_generator")
    }

    @Test
    void testMethodsAreAlphabetical() {
        new WSDLVimStubGenerator(
            "src/test/resources/wsdl/test-vim-operations.wsdl", dest
        ).generate(true, "com.vmware.vim25", [vim25: 'urn:vim25'])

        String content = new File(dest, "VimStub.java").text
        int posPower  = content.indexOf("public ManagedObjectReference powerOnVM_Task(")
        int posQuery  = content.indexOf("public String[] queryNames(")
        int posReload = content.indexOf("public void reload(")
        assert posPower > 0 && posQuery > 0 && posReload > 0
        assert posPower < posQuery
        assert posQuery < posReload
    }

    @Test
    void testReloadIsVoidWithRuntimeFaultAndRemoteException() {
        new WSDLVimStubGenerator(
            "src/test/resources/wsdl/test-vim-operations.wsdl", dest
        ).generate(true, "com.vmware.vim25", [vim25: 'urn:vim25'])

        String content = new File(dest, "VimStub.java").text
        assert content.contains(
            "public void reload(ManagedObjectReference _this) " +
            "throws java.rmi.RemoteException, RuntimeFault {")
        assert content.contains("getWsc().invoke(\"Reload\", paras, null);")
    }

    @Test
    void testPowerOnVMHasAlphabeticalSpecificFaults() {
        new WSDLVimStubGenerator(
            "src/test/resources/wsdl/test-vim-operations.wsdl", dest
        ).generate(true, "com.vmware.vim25", [vim25: 'urn:vim25'])

        String content = new File(dest, "VimStub.java").text
        assert content.contains(
            "throws java.rmi.RemoteException, InvalidState, TaskInProgress, RuntimeFault {")
    }

    @Test
    void testQueryNamesReturnsStringArray() {
        new WSDLVimStubGenerator(
            "src/test/resources/wsdl/test-vim-operations.wsdl", dest
        ).generate(true, "com.vmware.vim25", [vim25: 'urn:vim25'])

        String content = new File(dest, "VimStub.java").text
        assert content.contains("public String[] queryNames(ManagedObjectReference _this)")
        assert content.contains("return (String[]) getWsc().invoke(\"QueryNames\", paras, \"String[]\");")
    }
}
