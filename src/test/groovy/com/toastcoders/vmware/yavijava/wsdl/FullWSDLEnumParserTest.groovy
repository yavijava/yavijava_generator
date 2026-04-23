package com.toastcoders.vmware.yavijava.wsdl

import com.toastcoders.vmware.yavijava.data.DataObject
import org.junit.Before
import org.junit.Test

public class FullWSDLEnumParserTest {

    List<DataObject> enums

    @Before
    void setUp() {
        File wsdl = new File('src/test/resources/wsdl/test-vim.wsdl')
        def schemas = new FullWSDLSchemaReader().loadSchema(wsdl)
        enums = new FullWSDLEnumParser().parse(schemas)
    }

    @Test
    void testExtractsTaskInfoStateFromInlineSchema() {
        assert enums.find { it.name == "TaskInfoState" } != null
    }

    @Test
    void testExtractsVirtualMachinePowerStateFromIncludedXsd() {
        assert enums.find { it.name == "VirtualMachinePowerState" } != null
    }

    @Test
    void testEnumValuesAreParsedInOrder() {
        DataObject obj = enums.find { it.name == "TaskInfoState" }
        assert obj.objProperties == ["queued", "running", "success", "error"]
    }

    @Test
    void testEnumValueCountIsCorrect() {
        DataObject obj = enums.find { it.name == "VirtualMachinePowerState" }
        assert obj.objProperties.size() == 3
    }

    @Test
    void testComplexTypesAreNotIncluded() {
        assert enums.every { it.name != "BatchResult" && it.name != "TaskInfo" }
    }

    @Test
    void testAllReturnedEnumsHaveValues() {
        assert enums.every { it.objProperties.size() > 0 }
    }
}
