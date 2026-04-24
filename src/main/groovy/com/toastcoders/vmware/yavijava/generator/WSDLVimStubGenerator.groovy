package com.toastcoders.vmware.yavijava.generator

import com.toastcoders.vmware.yavijava.contracts.Generator
import com.toastcoders.vmware.yavijava.data.VimStubTemplate
import com.toastcoders.vmware.yavijava.wsdl.FullWSDLOperationParser
import com.toastcoders.vmware.yavijava.wsdl.FullWSDLSchemaReader
import com.toastcoders.vmware.yavijava.writer.WriteJavaClass

class WSDLVimStubGenerator implements Generator {

    String source
    String dest

    WSDLVimStubGenerator(String source, String dest) {
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
        def operations = new FullWSDLOperationParser().parse(wsdlFile, schemas)
        String content = VimStubTemplate.render(operations)
        WriteJavaClass.writeFile(dest + "VimStub.java", content)
    }
}
