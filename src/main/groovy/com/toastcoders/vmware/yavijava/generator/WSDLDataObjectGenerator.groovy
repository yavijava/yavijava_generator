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

    /**
     * Generates Java source files for every data object and ArrayOf wrapper
     * type defined in the WSDL.
     *
     * @param all       Inherited from {@link Generator}; ignored — WSDL-first mode
     *                  always emits all types (no "new vs changed" concept).
     * @param packageName Java package for the generated classes.
     * @param nameSpace Inherited from {@link Generator}; ignored — XML namespaces
     *                  are resolved from the schema documents directly.
     */
    @Override
    void generate(boolean all, String packageName, Map nameSpace) {
        File wsdlFile = new File(source)

        def schemas = new FullWSDLSchemaReader().loadSchema(wsdlFile)
        Set<String> generated = [] as Set

        new FullWSDLDataObjectParser().parse(schemas).each {
            writeDataObject(it, packageName)
            generated << "${it.name}.java".toString()
        }
        new FullWSDLArrayOfParser().parse(schemas).each {
            writeArrayOf(it, packageName)
            generated << "${it.name}.java".toString()
        }

        reportOrphans(generated)
    }

    private void reportOrphans(Set<String> generated) {
        def orphans = new OrphanDetector().findOrphans(new File(dest), generated)
        if (!orphans.isEmpty()) {
            println "Orphan generated files (in dest but not produced this run):"
            orphans.each { println "  ${it.name}" }
        }
    }

    private void writeDataObject(DataObject obj, String packageName) {
        StringBuilder sb = new StringBuilder()
        sb << DynamicDataTemplate.getPackageName(packageName)
        sb << DynamicDataTemplate.getImports()
        if (obj.objProperties.find { it.propType == "Calendar" || it.propType == "Calendar[]" }) {
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
        if (prop.propType == "Calendar" || prop.propType == "Calendar[]") {
            sb << "import java.util.Calendar;\n"
        }
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
