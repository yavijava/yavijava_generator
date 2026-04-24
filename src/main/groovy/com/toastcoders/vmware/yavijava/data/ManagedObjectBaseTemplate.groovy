package com.toastcoders.vmware.yavijava.data

import com.toastcoders.vmware.yavijava.dtm.MoMethod
import com.toastcoders.vmware.yavijava.dtm.MoMethodParam
import com.toastcoders.vmware.yavijava.dtm.MoProperty
import com.toastcoders.vmware.yavijava.dtm.MoTypeInfo
import com.toastcoders.vmware.yavijava.dtm.TypeMapper

class ManagedObjectBaseTemplate {

    static final String MARKER = "// auto generated using yavijava_generator"
    private static final TypeMapper MAPPER = new TypeMapper()

    static String render(MoTypeInfo t, String parentClassName) {
        StringBuilder sb = new StringBuilder()
        sb << header()
        sb << "package com.vmware.vim25.mo;\n\n"
        sb << "import com.vmware.vim25.*;\n"
        sb << "import java.rmi.RemoteException;\n\n"
        sb << "public class ${t.name}Base extends ${parentClassName} {\n\n"
        sb << "    public ${t.name}Base(ServerConnection serverConnection, ManagedObjectReference mor) {\n"
        sb << "        super(serverConnection, mor);\n"
        sb << "    }\n\n"

        if (!t.properties.isEmpty()) {
            sb << "    /* =========================Accessors=================================*/\n\n"
            t.properties.sort { it.name }.each { sb << renderGetter(it) }
        }
        if (!t.methods.isEmpty()) {
            sb << "    /* =========================Methods=================================*/\n\n"
            t.methods.sort { it.name }.each { sb << renderMethod(it) }
        }
        sb << "}\n"
        return sb.toString()
    }

    private static String header() {
        return """\
/*
 * Auto-generated managed object base — do not edit by hand. Subclass it.
 * ${MARKER}
 */
"""
    }

    private static String renderGetter(MoProperty p) {
        StringBuilder sb = new StringBuilder()
        String getter = "get" + capitalize(p.name)
        if (p.type == "ManagedObjectReference") {
            String moType = p.referencedMoType ?: "ManagedObject"
            if (p.isArray) {
                String helper = MAPPER.typedHelperFor(moType)
                if (helper) {
                    sb << "    public ${moType}[] ${getter}() {\n"
                    sb << "        return ${helper}(\"${p.name}\");\n"
                    sb << "    }\n\n"
                } else {
                    sb << "    public ${moType}[] ${getter}() {\n"
                    sb << "        return (${moType}[]) getManagedObjects(\"${p.name}\");\n"
                    sb << "    }\n\n"
                }
            } else {
                sb << "    public ${moType} ${getter}() {\n"
                sb << "        return (${moType}) this.getManagedObject(\"${p.name}\");\n"
                sb << "    }\n\n"
            }
        } else {
            String javaType = MAPPER.toJavaType(p.type, p.isArray, p.isOptional)
            sb << "    public ${javaType} ${getter}() {\n"
            sb << "        return (${javaType}) getCurrentProperty(\"${p.name}\");\n"
            sb << "    }\n\n"
        }
        return sb.toString()
    }

    private static String renderMethod(MoMethod m) {
        StringBuilder sb = new StringBuilder()
        String javaName = lowerCamelCase(m.name)
        String returnJava = mapReturn(m)
        String paramsSig = m.params.collect { paramSig(it) }.join(", ")
        String throws_ = renderThrowsForMo(m)

        sb << "    public ${returnJava} ${javaName}(${paramsSig}) throws ${throws_} {\n"

        List<String> stubArgs = ["getMOR()"]
        m.params.each { p ->
            if (p.type == "ManagedObjectReference" && p.referencedMoType) {
                stubArgs << "${p.name} == null ? null : ${p.name}.getMOR()"
            } else {
                stubArgs << p.name
            }
        }
        String stubCall = "getVimService().${javaName}(${stubArgs.join(", ")})"

        if (m.returnType == "void") {
            sb << "        ${stubCall};\n"
        } else if (m.returnType == "ManagedObjectReference" && m.referencedReturnMoType && !m.returnIsArray) {
            sb << "        ManagedObjectReference resultMor = ${stubCall};\n"
            sb << "        return new ${m.referencedReturnMoType}(getServerConnection(), resultMor);\n"
        } else {
            sb << "        return ${stubCall};\n"
        }
        sb << "    }\n\n"
        return sb.toString()
    }

    private static String paramSig(MoMethodParam p) {
        if (p.type == "ManagedObjectReference" && p.referencedMoType) {
            return p.isArray ? "${p.referencedMoType}[] ${p.name}" : "${p.referencedMoType} ${p.name}"
        }
        return "${MAPPER.toJavaType(p.type, p.isArray, p.isOptional)} ${p.name}"
    }

    private static String mapReturn(MoMethod m) {
        if (m.returnType == "void" || m.returnType == "" || m.returnType == null) return "void"
        if (m.returnType == "ManagedObjectReference" && m.referencedReturnMoType && !m.returnIsArray) {
            return m.referencedReturnMoType
        }
        return MAPPER.toJavaType(m.returnType, m.returnIsArray, false)
    }

    private static String renderThrowsForMo(MoMethod m) {
        List<String> opSpecific = (m.faults ?: []).findAll { it != "RuntimeFault" }.sort()
        List<String> all = opSpecific + ["RuntimeFault", "RemoteException"]
        return all.join(", ")
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s
        return s[0].toUpperCase() + s.substring(1)
    }

    private static String lowerCamelCase(String s) {
        if (s == null || s.isEmpty()) return s
        return s[0].toLowerCase() + s.substring(1)
    }
}
