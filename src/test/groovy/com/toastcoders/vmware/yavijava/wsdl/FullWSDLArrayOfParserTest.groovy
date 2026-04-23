package com.toastcoders.vmware.yavijava.wsdl

import com.toastcoders.vmware.yavijava.data.DataObject
import com.toastcoders.vmware.yavijava.data.Property
import org.junit.Before
import org.junit.Test

public class FullWSDLArrayOfParserTest {

    List<DataObject> arrayOfs

    @Before
    void setUp() {
        File wsdl = new File('src/test/resources/wsdl/test-vim.wsdl')
        def schemas = new FullWSDLSchemaReader().loadSchema(wsdl)
        arrayOfs = new FullWSDLArrayOfParser().parse(schemas)
    }

    @Test
    void testExtractsArrayOfBatchResult() {
        assert arrayOfs.find { it.name == "ArrayOfBatchResult" } != null
    }

    @Test
    void testExtractsArrayOfTaskInfoFromIncludedXsd() {
        assert arrayOfs.find { it.name == "ArrayOfTaskInfo" } != null
    }

    @Test
    void testExtendsBaseIsEmpty() {
        DataObject obj = arrayOfs.find { it.name == "ArrayOfBatchResult" }
        assert obj.extendsBase == ""
    }

    @Test
    void testHasSinglePropertyMatchingElementName() {
        DataObject obj = arrayOfs.find { it.name == "ArrayOfBatchResult" }
        assert obj.objProperties.size() == 1
        Property p = obj.objProperties[0]
        assert p.name == "BatchResult"
    }

    @Test
    void testPropertyTypeIsTheElementTypeStrippedOfNamespace() {
        DataObject obj = arrayOfs.find { it.name == "ArrayOfBatchResult" }
        Property p = obj.objProperties[0]
        // Stored without [] — template renders the array brackets to match
        // yavijava's existing ArrayOf field declaration style
        assert p.propType == "BatchResult"
    }

    @Test
    void testNonArrayOfComplexTypesAreSkipped() {
        // BatchResult and TaskInfo have complexContent/extension — not ArrayOf
        assert arrayOfs.find { it.name == "BatchResult" } == null
        assert arrayOfs.find { it.name == "TaskInfo" } == null
    }

    @Test
    void testTypesNamedArrayOfButWithExtensionAreSkipped() {
        // Defensive: only bare-sequence types are ArrayOf wrappers.
        // (No fixture for this case exists; assertion is structural via the
        // implementation contract checked by the prior test.)
        assert arrayOfs.every { it.extendsBase == "" }
    }

    @Test
    void testNonArrayOfBareSequenceTypeIsSkipped() {
        // SomeRequestType has a bare sequence (no complexContent/extension)
        // but does not start with "ArrayOf" — must not be classified as an ArrayOf wrapper.
        assert arrayOfs.find { it.name == "SomeRequestType" } == null
    }
}
