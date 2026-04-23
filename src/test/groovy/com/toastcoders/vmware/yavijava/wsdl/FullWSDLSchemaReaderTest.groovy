package com.toastcoders.vmware.yavijava.wsdl

import org.junit.Before
import org.junit.Test

public class FullWSDLSchemaReaderTest {

    FullWSDLSchemaReader reader
    File wsdlFile

    @Before
    void setUp() {
        reader = new FullWSDLSchemaReader()
        wsdlFile = new File('src/test/resources/wsdl/test-vim.wsdl')
    }

    @Test
    void testLoadsInlineSchema() {
        def schemas = reader.loadSchema(wsdlFile)
        assert schemas != null
        assert schemas.size() > 0
    }

    @Test
    void testResolvesIncludedXsd() {
        def schemas = reader.loadSchema(wsdlFile)
        // 1 inline + test-types.xsd (include) + test-import.xsd (import) = 3
        assert schemas.size() == 3
    }

    @Test
    void testResolvesImportedXsd() {
        // Verify the imported schema's content is reachable by parsing for ReflectInfo
        def schemas = reader.loadSchema(wsdlFile)
        boolean found = schemas.any { schema ->
            schema.complexType.any { it.'@name'.text() == 'ReflectInfo' }
        }
        assert found, "ReflectInfo from imported test-import.xsd should be reachable"
    }

    @Test
    void testMissingWsdlFileThrows() {
        try {
            reader.loadSchema(new File('nonexistent.wsdl'))
            assert false, "Expected exception"
        } catch (Exception ignored) {
        }
    }

    @Test
    void testMissingReferencedXsdIsSkippedNotFatal() {
        File tmp = File.createTempFile("missing-include", ".wsdl")
        tmp.deleteOnExit()
        tmp.text = '''<?xml version="1.0"?>
<definitions targetNamespace="urn:vim25"
   xmlns="http://schemas.xmlsoap.org/wsdl/"
   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
   <types>
      <schema targetNamespace="urn:vim25" xmlns="http://www.w3.org/2001/XMLSchema">
         <include schemaLocation="does-not-exist.xsd" />
         <simpleType name="X">
            <restriction base="xsd:string"><enumeration value="a"/></restriction>
         </simpleType>
      </schema>
   </types>
</definitions>'''
        def schemas = reader.loadSchema(tmp)
        // Inline schema still loads; missing include is skipped (logged at WARN)
        assert schemas.size() == 1
    }
}
