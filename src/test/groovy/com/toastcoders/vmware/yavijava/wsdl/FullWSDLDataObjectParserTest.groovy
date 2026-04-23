package com.toastcoders.vmware.yavijava.wsdl

import com.toastcoders.vmware.yavijava.data.DataObject
import com.toastcoders.vmware.yavijava.data.Property
import org.junit.Before
import org.junit.Test

public class FullWSDLDataObjectParserTest {

    List<DataObject> dataObjects

    @Before
    void setUp() {
        File wsdl = new File('src/test/resources/wsdl/test-vim.wsdl')
        def schemas = new FullWSDLSchemaReader().loadSchema(wsdl)
        dataObjects = new FullWSDLDataObjectParser().parse(schemas)
    }

    @Test
    void testExtractsBatchResult() {
        assert dataObjects.find { it.name == "BatchResult" } != null
    }

    @Test
    void testExtractsTaskInfoFromIncludedXsd() {
        assert dataObjects.find { it.name == "TaskInfo" } != null
    }

    @Test
    void testExtractsReflectInfoFromImportedXsd() {
        assert dataObjects.find { it.name == "ReflectInfo" } != null
    }

    @Test
    void testExtendsBaseIsParsed() {
        DataObject obj = dataObjects.find { it.name == "BatchResult" }
        assert obj.extendsBase == "DynamicData"
    }

    @Test
    void testStringPropertyType() {
        Property p = propOf("BatchResult", "result")
        assert p.propType == "String"
    }

    @Test
    void testOptionalLongBecomesBoxed() {
        Property p = propOf("BatchResult", "checkLong")
        assert p.propType == "Long"
    }

    @Test
    void testOptionalIntBecomesBoxed() {
        Property p = propOf("BatchResult", "optionalInt")
        assert p.propType == "Integer"
    }

    @Test
    void testUnboundedStringBecomesArray() {
        Property p = propOf("BatchResult", "tags")
        assert p.propType == "String[]"
    }

    @Test
    void testUnboundedIntBecomesPrimitiveArray() {
        Property p = propOf("BatchResult", "counts")
        assert p.propType == "int[]"
    }

    @Test
    void testAnyTypeBecomesObjectArray() {
        Property p = propOf("TaskInfo", "results")
        assert p.propType == "Object[]"
    }

    @Test
    void testReferencedTypeIsStrippedOfNamespace() {
        Property p = propOf("TaskInfo", "state")
        assert p.propType == "TaskInfoState"
    }

    @Test
    void testArrayOfTypesAreSkipped() {
        // ArrayOfBatchResult and ArrayOfTaskInfo are handled by FullWSDLArrayOfParser
        assert dataObjects.find { it.name == "ArrayOfBatchResult" } == null
        assert dataObjects.find { it.name == "ArrayOfTaskInfo" } == null
    }

    @Test
    void testAnonymousComplexTypesAreSkipped() {
        assert dataObjects.every { it.name && !it.name.isEmpty() }
    }

    private Property propOf(String typeName, String propName) {
        DataObject obj = dataObjects.find { it.name == typeName }
        assert obj != null, "Missing type: $typeName"
        Property p = obj.objProperties.find { it.name == propName }
        assert p != null, "Missing property $propName on $typeName"
        return p
    }
}
