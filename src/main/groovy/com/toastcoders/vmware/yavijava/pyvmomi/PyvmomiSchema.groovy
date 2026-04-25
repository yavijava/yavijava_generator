package com.toastcoders.vmware.yavijava.pyvmomi

import groovy.json.JsonSlurper

class PyvmomiSchema {

    static final int CURRENT_SCHEMA_VERSION = 1

    Map<String, PyvmomiManagedObject> read(File f) {
        Object parsed
        try {
            parsed = new JsonSlurper().parse(f)
        } catch (Exception e) {
            throw new RuntimeException("pyVmomi schema ${f} is not valid JSON: ${e.message}", e)
        }
        if (!(parsed instanceof Map) || parsed.schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new RuntimeException(
                "pyVmomi schema ${f} has schemaVersion ${parsed?.schemaVersion}, expected ${CURRENT_SCHEMA_VERSION}.")
        }
        Map<String, PyvmomiManagedObject> out = [:]
        (parsed.managedObjects ?: []).each { Map mo ->
            PyvmomiManagedObject pm = new PyvmomiManagedObject()
            pm.name           = mo.name
            pm.wsdlName       = mo.wsdlName
            pm.qualifiedName  = mo.qualifiedName
            pm.parent         = mo.parent ?: ""
            pm.version        = mo.version
            pm.introducedIn   = mo.introducedIn
            (mo.properties ?: []).each { Map p ->
                PyvmomiProperty pp = new PyvmomiProperty()
                pp.name = p.name
                pp.type = p.type
                pp.isArray = p.isArray as boolean
                pp.isOptional = p.isOptional as boolean
                pp.isManagedObjectReference = p.isManagedObjectReference as boolean
                pp.version = p.version
                pp.privilegeId = p.privilegeId
                pm.properties << pp
            }
            (mo.methods ?: []).each { Map m ->
                PyvmomiMethod pmth = new PyvmomiMethod()
                pmth.name = m.name
                pmth.wsdlName = m.wsdlName
                pmth.version = m.version
                pmth.returnType = m.returnType
                pmth.returnIsArray = m.returnIsArray as boolean
                pmth.returnIsManagedObjectReference = m.returnIsManagedObjectReference as boolean
                pmth.faults = (m.faults ?: []) as List<String>
                pmth.privilegeId = m.privilegeId
                (m.params ?: []).each { Map pa ->
                    PyvmomiParam pp = new PyvmomiParam()
                    pp.name = pa.name
                    pp.type = pa.type
                    pp.isArray = pa.isArray as boolean
                    pp.isOptional = pa.isOptional as boolean
                    pp.isManagedObjectReference = pa.isManagedObjectReference as boolean
                    pmth.params << pp
                }
                pm.methods << pmth
            }
            out[pm.name] = pm
        }
        return out
    }
}
