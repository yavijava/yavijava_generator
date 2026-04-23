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
        Set<String> visited = [wsdlFile.canonicalPath] as Set

        // Recursively resolve <include> and <import> directives from the
        // inline schema and all transitively referenced XSD files.
        resolveRefs(inlineSchema, baseDir, schemas, visited)

        return schemas
    }

    private void resolveRefs(GPathResult schema, File baseDir, List<GPathResult> schemas, Set<String> visited) {
        // Resolve <include> directives
        schema.include.each { incl ->
            String loc = incl.'@schemaLocation'.text()
            if (loc) loadAndRecurse(new File(baseDir, loc), schemas, visited)
        }

        // Resolve <import> directives (different element, same loading logic)
        schema.'import'.each { imp ->
            String loc = imp.'@schemaLocation'.text()
            if (loc) loadAndRecurse(new File(baseDir, loc), schemas, visited)
        }
    }

    private void loadAndRecurse(File xsdFile, List<GPathResult> schemas, Set<String> visited) {
        String canonical = xsdFile.canonicalPath
        if (canonical in visited) return
        visited << canonical

        if (xsdFile.canRead()) {
            GPathResult parsed = parseXml(xsdFile)
            schemas << parsed
            // Recursively follow includes/imports in the newly loaded schema
            resolveRefs(parsed, xsdFile.parentFile, schemas, visited)
        } else {
            log.warn("Referenced schema not found, skipping: ${xsdFile.absolutePath}")
        }
    }

    private GPathResult parseXml(File f) {
        new XmlSlurper().parse(f).declareNamespace([xsd: 'http://www.w3.org/2001/XMLSchema'])
    }
}
