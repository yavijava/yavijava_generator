package com.toastcoders.vmware.yavijava.generator

import com.toastcoders.vmware.yavijava.contracts.Generator
import com.toastcoders.vmware.yavijava.data.DataObject
import com.toastcoders.vmware.yavijava.data.EnumJavaTemplate
import com.toastcoders.vmware.yavijava.wsdl.FullWSDLEnumParser
import com.toastcoders.vmware.yavijava.wsdl.FullWSDLSchemaReader
import com.toastcoders.vmware.yavijava.writer.WriteJavaClass

class WSDLEnumGenerator implements Generator {

    String source
    String dest

    WSDLEnumGenerator(String source, String dest) {
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

        def schemas = new FullWSDLSchemaReader().loadSchema(wsdlFile)
        new FullWSDLEnumParser().parse(schemas).each { writeEnum(it, packageName) }
    }

    private void writeEnum(DataObject obj, String packageName) {
        StringBuilder sb = new StringBuilder()
        sb << EnumJavaTemplate.getPackageName(packageName)
        sb << EnumJavaTemplate.getLicense()
        sb << EnumJavaTemplate.getClassDef(obj.name)

        int remaining = obj.objProperties.size()
        obj.objProperties.each { val ->
            String ending = (remaining == 1) ? ";" : ","
            sb << EnumJavaTemplate.getEnumProp(val.toString(), ending)
            remaining--
        }
        sb << EnumJavaTemplate.getPrivVal()
        sb << EnumJavaTemplate.constructorGenerator(obj.name)
        sb << EnumJavaTemplate.toStringGenerator()
        sb << EnumJavaTemplate.closeClass()

        WriteJavaClass.writeFile(dest + obj.name + ".java", sb.toString())
    }
}
