package com.toastcoders.vmware.yavijava.wsdl

import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FullWSDLSchemaReader {

    private static final Logger log = LoggerFactory.getLogger(FullWSDLSchemaReader)

    List<GPathResult> loadSchema(File wsdlFile) {
        if (!wsdlFile.canRead()) {
            throw new IllegalArgumentException("Cannot read WSDL file: ${wsdlFile.absolutePath}")
        }

        def wsdl = parseXml(wsdlFile)
        GPathResult inlineSchema = wsdl.types.schema
        List<GPathResult> schemas = [inlineSchema]

        File baseDir = wsdlFile.parentFile

        // Resolve <include> directives
        inlineSchema.include.each { incl ->
            String loc = incl.'@schemaLocation'.text()
            loadIfPresent(new File(baseDir, loc), schemas)
        }

        // Resolve <import> directives (different element, same loading logic)
        inlineSchema.'import'.each { imp ->
            String loc = imp.'@schemaLocation'.text()
            loadIfPresent(new File(baseDir, loc), schemas)
        }

        return schemas
    }

    private void loadIfPresent(File xsdFile, List<GPathResult> schemas) {
        if (xsdFile.canRead()) {
            schemas << parseXml(xsdFile)
        } else {
            log.warn("Referenced schema not found, skipping: ${xsdFile.absolutePath}")
        }
    }

    private GPathResult parseXml(File f) {
        new XmlSlurper().parse(f).declareNamespace([xsd: 'http://www.w3.org/2001/XMLSchema'])
    }
}
