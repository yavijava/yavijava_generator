package com.toastcoders.vmware.yavijava.wsdl

import com.toastcoders.vmware.yavijava.data.Operation
import com.toastcoders.vmware.yavijava.data.OpParam
import com.toastcoders.vmware.yavijava.dtm.TypeMapper
import groovy.xml.XmlSlurper

class FullWSDLOperationParser {

    private final TypeMapper typeMapper = new TypeMapper()

    List<Operation> parse(File wsdlFile, List schemas) {
        def wsdl = new XmlSlurper().parse(wsdlFile)
        wsdl.declareNamespace(wsdl: "http://schemas.xmlsoap.org/wsdl/")

        Map<String, String> msgToElement = [:]
        wsdl.message.each { m ->
            def part = m.part[0]
            String element = part.@element.text() ?: part.@type.text()
            msgToElement[m.@name.text()] = stripPrefix(element)
        }

        Map<String, Object> typesByName = indexComplexTypes(schemas)
        Map<String, String> elementsByName = indexTopLevelElements(schemas)
        Map<String, Object> elementNodesByName = indexTopLevelElementNodes(schemas)

        List<Operation> operations = []
        def portType = wsdl.portType.find { it.@name == "VimPortType" }
        portType.operation.each { op ->
            Operation o = new Operation()
            o.name = op.@name.text()

            String inputMsg = stripPrefix(op.input.@message.text())
            String requestElement = msgToElement[inputMsg]
            String requestTypeName = elementsByName[requestElement] ?: requestElement
            Object requestCt = typesByName[requestTypeName]
            if (requestCt == null) {
                // Fall back to inline complexType under the element
                requestCt = elementNodesByName[requestElement]?.complexType?.getAt(0)
            }
            o.params = extractParams(requestCt)

            String outputMsg = stripPrefix(op.output.@message.text())
            String responseElement = msgToElement[outputMsg]
            String responseTypeName = elementsByName[responseElement] ?: responseElement
            Object responseCt = typesByName[responseTypeName]
            if (responseCt == null) {
                responseCt = elementNodesByName[responseElement]
            }
            extractReturn(responseCt, o)

            op.fault.each { f ->
                String name = f.@name.text()
                if (name && name != "RuntimeFault") o.faults << name
            }
            o.faults << "RuntimeFault"

            operations << o
        }
        return operations
    }

    private Map<String, Object> indexComplexTypes(List schemas) {
        Map<String, Object> out = [:]
        schemas.each { schema ->
            schema.complexType.each { ct ->
                String name = ct.@name.text()
                if (name) out[name] = ct
            }
        }
        return out
    }

    private Map<String, String> indexTopLevelElements(List schemas) {
        Map<String, String> out = [:]
        schemas.each { schema ->
            schema.element.each { el ->
                String name = el.@name.text()
                String type = el.@type.text()
                if (name) out[name] = stripPrefix(type)
            }
        }
        return out
    }

    private Map<String, Object> indexTopLevelElementNodes(List schemas) {
        Map<String, Object> out = [:]
        schemas.each { schema ->
            schema.element.each { el ->
                String name = el.@name.text()
                if (name) out[name] = el
            }
        }
        return out
    }

    private List<OpParam> extractParams(Object complexType) {
        List<OpParam> params = []
        if (complexType == null) return params
        def seq = complexType.sequence[0]
        if (seq == null || seq.isEmpty()) {
            seq = complexType.complexContent[0]?.extension[0]?.sequence[0]
        }
        if (seq == null || seq.isEmpty()) return params
        seq.element.each { el ->
            OpParam p = new OpParam()
            p.name = el.@name.text()
            p.type = stripPrefix(el.@type.text())
            p.isArray = el.@maxOccurs.text() == "unbounded"
            p.isOptional = el.@minOccurs.text() == "0"
            params << p
        }
        return params
    }

    private void extractReturn(Object responseType, Operation op) {
        if (responseType == null) { op.returnType = ""; return }
        def seq = responseType.sequence[0]
        if (seq == null || seq.isEmpty()) {
            seq = responseType.complexType[0]?.sequence[0]
        }
        if (seq == null || seq.isEmpty()) { op.returnType = ""; return }
        def returnval = seq.element.find { it.@name.text() == "returnval" }
        if (returnval == null) { op.returnType = ""; return }
        op.returnType = stripPrefix(returnval.@type.text())
        op.returnIsArray = returnval.@maxOccurs.text() == "unbounded"
        if (returnval.@type.text().startsWith("xsd:")) {
            op.returnType = typeMapper.toJavaType(returnval.@type.text(), false, false)
        }
    }

    private String stripPrefix(String qname) {
        if (qname == null) return ""
        return qname.contains(":") ? qname.split(":", 2)[1] : qname
    }
}
