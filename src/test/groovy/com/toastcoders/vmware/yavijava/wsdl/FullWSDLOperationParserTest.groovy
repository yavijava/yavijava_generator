package com.toastcoders.vmware.yavijava.wsdl

import com.toastcoders.vmware.yavijava.data.Operation
import org.junit.Test

class FullWSDLOperationParserTest {

    private List<Operation> parseFixture() {
        def schemas = new FullWSDLSchemaReader().loadSchema(
            new File("src/test/resources/wsdl/test-vim-operations.wsdl"))
        return new FullWSDLOperationParser().parse(
            new File("src/test/resources/wsdl/test-vim-operations.wsdl"), schemas)
    }

    @Test
    void testParsesAllThreeOperations() {
        def ops = parseFixture()
        def names = ops*.name as Set
        assert names == ["Reload", "PowerOnVM_Task", "QueryNames"] as Set
    }

    @Test
    void testReloadIsVoidWithSingleThis() {
        def reload = parseFixture().find { it.name == "Reload" }
        assert reload.returnType == ""
        assert !reload.returnIsArray
        assert reload.params.size() == 1
        assert reload.params[0].name == "_this"
        assert reload.params[0].type == "ManagedObjectReference"
        assert reload.faults == ["RuntimeFault"]
    }

    @Test
    void testPowerOnVMHasOptionalHostParamAndReturnsMOR() {
        def op = parseFixture().find { it.name == "PowerOnVM_Task" }
        assert op.returnType == "ManagedObjectReference"
        assert !op.returnIsArray
        assert op.params*.name == ["_this", "host"]
        assert op.params[1].type == "ManagedObjectReference"
        assert op.params[1].isOptional
        assert op.faults as Set == ["TaskInProgress", "InvalidState", "RuntimeFault"] as Set
    }

    @Test
    void testQueryNamesReturnsArrayOfString() {
        def op = parseFixture().find { it.name == "QueryNames" }
        assert op.returnType == "String"
        assert op.returnIsArray
    }
}
