package com.toastcoders.vmware.yavijava.data

class VimStubTemplate {

    static final String MARKER = "// auto generated using yavijava_generator"

    static String render(List<Operation> operations) {
        StringBuilder sb = new StringBuilder()
        sb << header()
        sb << packageAndImports()
        sb << classOpenWithBoilerplate()
        operations.sort { it.name.toLowerCase() }
                  .each { sb << renderMethod(it) }
        sb << "}\n"
        return sb.toString()
    }

    private static String header() {
        return """\
/*================================================================================
Copyright (c) 2013 Steve Jin. All Rights Reserved.

Auto-generated dispatch table — do not edit by hand.
${MARKER}
================================================================================*/
"""
    }

    private static String packageAndImports() {
        return """
package com.vmware.vim25.ws;

import com.vmware.vim25.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManager;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.Calendar;

"""
    }

    private static String classOpenWithBoilerplate() {
        return """\
public class VimStub {

    private Client wsc = null;
    private static Logger log = LoggerFactory.getLogger(VimStub.class);

    public VimStub(String url, boolean ignoreCert) {
        try {
            this.wsc = ClientCreator.getClient(url, ignoreCert);
        } catch (NoSuchMethodException | IllegalAccessException
               | InvocationTargetException | InstantiationException e) {
            log.error("Error detected for url: " + url + " ignoreSSL: " + ignoreCert, e);
        }
    }

    public VimStub(String url, TrustManager trustManager) {
        try {
            this.wsc = ClientCreator.getClient(url, trustManager);
        } catch (Exception e) {
            log.error("Error detected for url: " + url + " trustManager: " + trustManager, e);
        }
    }

    public VimStub(Client wsc) {
        this.wsc = wsc;
    }

    public Client getWsc() {
        return wsc;
    }

"""
    }

    private static String renderMethod(Operation op) {
        String javaName = lowerCamelCase(op.name)
        String returnJava = mapReturnType(op)
        String throws_ = renderThrowsForVimStub(op)
        StringBuilder sb = new StringBuilder()
        sb << "    public ${returnJava} ${javaName}("
        sb << op.params.collect { "${mapParamType(it)} ${it.name}" }.join(", ")
        sb << ") throws ${throws_} {\n"
        sb << "        Argument[] paras = new Argument[${op.params.size()}];\n"
        op.params.eachWithIndex { p, i ->
            sb << "        paras[${i}] = new Argument(\"${p.name}\", \"${wireTypeOf(p)}\", ${p.name});\n"
        }
        if (returnJava == "void") {
            sb << "        getWsc().invoke(\"${op.name}\", paras, null);\n"
        } else {
            String castType = wireReturnType(op)
            sb << "        return (${returnJava}) getWsc().invoke(\"${op.name}\", paras, \"${castType}\");\n"
        }
        sb << "    }\n\n"
        return sb.toString()
    }

    private static String mapReturnType(Operation op) {
        if (op.returnType == null || op.returnType == "") return "void"
        return op.returnIsArray ? "${op.returnType}[]" : op.returnType
    }

    private static String mapParamType(OpParam p) {
        if (p.type.startsWith("xsd:")) {
            return p.isArray ? "${stripXsd(p.type)}[]" : stripXsd(p.type)
        }
        return p.isArray ? "${p.type}[]" : p.type
    }

    private static String wireTypeOf(OpParam p) {
        return mapParamType(p)
    }

    private static String wireReturnType(Operation op) {
        return op.returnIsArray ? "${op.returnType}[]" : op.returnType
    }

    private static String stripXsd(String t) {
        switch (t) {
            case "xsd:string":      return "String"
            case "xsd:int":         return "int"
            case "xsd:long":        return "long"
            case "xsd:boolean":     return "boolean"
            case "xsd:short":       return "short"
            case "xsd:byte":        return "byte"
            case "xsd:float":       return "float"
            case "xsd:double":      return "double"
            case "xsd:dateTime":    return "Calendar"
            case "xsd:anyType":     return "Object"
            case "xsd:anyURI":      return "String"
            case "xsd:base64Binary":return "byte[]"
            default: return t.contains(":") ? t.split(":", 2)[1] : t
        }
    }

    private static String renderThrowsForVimStub(Operation op) {
        List<String> opSpecific = op.faults.findAll { it != "RuntimeFault" }.sort()
        List<String> all = ["java.rmi.RemoteException"] + opSpecific + ["RuntimeFault"]
        return all.join(", ")
    }

    private static String lowerCamelCase(String name) {
        if (name == null || name.isEmpty()) return name
        return name[0].toLowerCase() + name.substring(1)
    }
}
