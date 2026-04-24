package com.toastcoders.vmware.yavijava.generator

import org.junit.After
import org.junit.Before
import org.junit.Test

import java.security.MessageDigest

class DeterminismTest {

    File run1, run2

    @Before void setUp() {
        run1 = File.createTempDir("det-run1-")
        run2 = File.createTempDir("det-run2-")
    }

    @After void tearDown() { run1.deleteDir(); run2.deleteDir() }

    @Test
    void testWsdlVimStubByteIdenticalBetweenRuns() {
        new WSDLVimStubGenerator(
            "src/test/resources/wsdl/test-vim-operations.wsdl",
            run1.absolutePath + File.separator).generate(true, "com.vmware.vim25", [vim25: 'urn:vim25'])
        new WSDLVimStubGenerator(
            "src/test/resources/wsdl/test-vim-operations.wsdl",
            run2.absolutePath + File.separator).generate(true, "com.vmware.vim25", [vim25: 'urn:vim25'])

        assertSameDigests(run1, run2)
    }

    @Test
    void testDtmManagedObjectByteIdenticalBetweenRuns() {
        new DTMManagedObjectGenerator(
            "src/test/resources/dtm/sample-mo-types.json",
            run1.absolutePath + File.separator,
            null, null, null, false).generate(true, "com.vmware.vim25.mo", [:])
        new DTMManagedObjectGenerator(
            "src/test/resources/dtm/sample-mo-types.json",
            run2.absolutePath + File.separator,
            null, null, null, false).generate(true, "com.vmware.vim25.mo", [:])

        assertSameDigests(run1, run2)
    }

    private void assertSameDigests(File a, File b) {
        Map<String, String> dA = digestsOf(a)
        Map<String, String> dB = digestsOf(b)
        assert dA.keySet() == dB.keySet() : "file sets differ: ${dA.keySet()} vs ${dB.keySet()}"
        dA.each { name, digest ->
            assert digest == dB[name] : "${name} differs between runs"
        }
    }

    private Map<String, String> digestsOf(File dir) {
        Map<String, String> out = [:]
        dir.listFiles().findAll { it.name.endsWith(".java") }.each {
            out[it.name] = sha256(it.bytes)
        }
        return out
    }

    private String sha256(byte[] bytes) {
        return MessageDigest.getInstance("SHA-256").digest(bytes).encodeHex().toString()
    }
}
