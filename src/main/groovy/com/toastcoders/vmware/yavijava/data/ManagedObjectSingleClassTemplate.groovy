package com.toastcoders.vmware.yavijava.data

import com.toastcoders.vmware.yavijava.pyvmomi.PyvmomiManagedObject
import com.toastcoders.vmware.yavijava.pyvmomi.PyvmomiMethod
import com.toastcoders.vmware.yavijava.pyvmomi.PyvmomiParam
import com.toastcoders.vmware.yavijava.pyvmomi.PyvmomiProperty

class ManagedObjectSingleClassTemplate {

    static final String MARKER = "// auto generated using yavijava_generator"

    private static final Map<String, String> TYPED_HELPERS = [
        "Task"           : "getTasks",
        "Datastore"      : "getDatastores",
        "HostSystem"     : "getHosts",
        "VirtualMachine" : "getVms",
        "Network"        : "getNetworks",
        "ResourcePool"   : "getResourcePools",
        "ScheduledTask"  : "getScheduledTasks",
        "View"           : "getViews",
        "PropertyFilter" : "getFilter",
    ]

    static String render(PyvmomiManagedObject mo) {
        StringBuilder sb = new StringBuilder()
        sb << MARKER << "\n"
        sb << "package com.vmware.vim25.mo;\n\n"
        sb << "import com.vmware.vim25.*;\n"
        sb << "import java.rmi.RemoteException;\n\n"
        sb << "/* ===== BEGIN custom imports (preserved by regenerator) ===== */\n"
        sb << "/* ===== END custom imports ===== */\n\n"
        String parent = mo.parent ?: "ManagedObject"
        sb << "public class ${mo.name} extends ${parent} {\n\n"
        sb << "    public ${mo.name}(ServerConnection serverConnection, ManagedObjectReference mor) {\n"
        sb << "        super(serverConnection, mor);\n"
        sb << "    }\n\n"
        mo.properties.sort { it.name }.each { sb << renderGetter(it) }
        mo.methods.sort { it.name }.each { sb << renderMethod(it) }
        sb << "    /* ===== BEGIN custom (preserved by regenerator) ===== */\n"
        sb << "    /* ===== END custom ===== */\n"
        sb << "}\n"
        return sb.toString()
    }

    private static String renderGetter(PyvmomiProperty p) {
        StringBuilder sb = new StringBuilder()
        String getter = "get" + capitalize(p.name)
        if (p.isManagedObjectReference) {
            String moType = p.type ?: "ManagedObject"
            if (p.isArray) {
                String helper = TYPED_HELPERS[moType]
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
            String javaType = p.isArray ? "${p.type}[]" : p.type
            sb << "    public ${javaType} ${getter}() {\n"
            sb << "        return (${javaType}) getCurrentProperty(\"${p.name}\");\n"
            sb << "    }\n\n"
        }
        return sb.toString()
    }

    private static String renderMethod(PyvmomiMethod m) {
        StringBuilder sb = new StringBuilder()
        String returnJava = mapReturn(m)
        String paramsSig  = m.params.collect { paramSig(it) }.join(", ")
        String throws_    = renderThrows(m)
        sb << "    public ${returnJava} ${m.name}(${paramsSig}) throws ${throws_} {\n"
        List<String> stubArgs = ["getMOR()"]
        m.params.each { p ->
            if (p.isManagedObjectReference) {
                stubArgs << "${p.name} == null ? null : ${p.name}.getMOR()"
            } else {
                stubArgs << p.name
            }
        }
        String stubCall = "getVimService().${m.name}(${stubArgs.join(", ")})"
        if (m.returnType == "void" || !m.returnType) {
            sb << "        ${stubCall};\n"
        } else if (m.returnIsManagedObjectReference && !m.returnIsArray) {
            sb << "        ManagedObjectReference resultMor = ${stubCall};\n"
            sb << "        return new ${m.returnType}(getServerConnection(), resultMor);\n"
        } else {
            sb << "        return ${stubCall};\n"
        }
        sb << "    }\n\n"
        return sb.toString()
    }

    private static String paramSig(PyvmomiParam p) {
        String t = p.isArray ? "${p.type}[]" : p.type
        return "${t} ${p.name}"
    }

    private static String mapReturn(PyvmomiMethod m) {
        if (!m.returnType || m.returnType == "void") return "void"
        return m.returnIsArray ? "${m.returnType}[]" : m.returnType
    }

    private static String renderThrows(PyvmomiMethod m) {
        List<String> opSpecific = (m.faults ?: []).findAll { it != "RuntimeFault" }.sort()
        return (opSpecific + ["RuntimeFault", "RemoteException"]).join(", ")
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s
        return s[0].toUpperCase() + s.substring(1)
    }
}
