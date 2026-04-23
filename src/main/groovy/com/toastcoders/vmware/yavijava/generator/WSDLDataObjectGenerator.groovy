package com.toastcoders.vmware.yavijava.generator

import com.toastcoders.vmware.yavijava.contracts.Generator
import com.toastcoders.vmware.yavijava.data.ArrayOfTemplate
import com.toastcoders.vmware.yavijava.data.DataObject
import com.toastcoders.vmware.yavijava.data.DynamicDataTemplate
import com.toastcoders.vmware.yavijava.wsdl.FullWSDLArrayOfParser
import com.toastcoders.vmware.yavijava.wsdl.FullWSDLDataObjectParser
import com.toastcoders.vmware.yavijava.wsdl.FullWSDLSchemaReader
import com.toastcoders.vmware.yavijava.writer.WriteJavaClass

class WSDLDataObjectGenerator implements Generator {

    String source
    String dest

    WSDLDataObjectGenerator(String source, String dest) {
        this.source = source
        this.dest = dest
    }

    @Override
    void generate() { generate(true, "com.vmware.vim25", [vim25: 'urn:vim25']) }

    @Override
    void generate(boolean all) { generate(all, "com.vmware.vim25", [vim25: 'urn:vim25']) }

    @Override
    void generate(boolean all, String packageName, Map nameSpace) {
        File wsdlFile = new File(source)
        assert wsdlFile.canRead(): "Cannot read WSDL: ${wsdlFile.absolutePath}"

        def schemas = new FullWSDLSchemaReader().loadSchema(wsdlFile)

        new FullWSDLDataObjectParser().parse(schemas).each {
            writeDataObject(it, packageName)
        }
        new FullWSDLArrayOfParser().parse(schemas).each {
            writeArrayOf(it, packageName)
        }
    }

    private void writeDataObject(DataObject obj, String packageName) {
        StringBuilder sb = new StringBuilder()
        sb << DynamicDataTemplate.getPackageName(packageName)
        sb << DynamicDataTemplate.getImports()
        if (obj.objProperties.find { it.propType == "Calendar" }) {
            sb << "import java.util.Calendar;\n"
        }
        sb << DynamicDataTemplate.getLicense()
        sb << DynamicDataTemplate.getClassDef(obj.name, obj.extendsBase)
        obj.objProperties.each {
            sb << "    ${DynamicDataTemplate.getPropertyType(it.propType, it.name)}"
        }
        sb << DynamicDataTemplate.closeClass()
        WriteJavaClass.writeFile(dest + obj.name + ".java", sb.toString())
    }

    private void writeArrayOf(DataObject obj, String packageName) {
        // ArrayOf has exactly one property
        def prop = obj.objProperties[0]
        StringBuilder sb = new StringBuilder()
        sb << ArrayOfTemplate.getPackageName(packageName)
        sb << ArrayOfTemplate.getLicense()
        sb << ArrayOfTemplate.getClassDef(obj.name)
        sb << ArrayOfTemplate.getField(prop.propType, prop.name)
        sb << ArrayOfTemplate.getArrayGetter(prop.propType, prop.name)
        sb << ArrayOfTemplate.getIndexedGetter(prop.propType, prop.name)
        sb << ArrayOfTemplate.getSetter(prop.propType, prop.name)
        sb << ArrayOfTemplate.closeClass()
        WriteJavaClass.writeFile(dest + obj.name + ".java", sb.toString())
    }
}
