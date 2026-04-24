package com.toastcoders.vmware.yavijava.dtm

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class DtmSnapshot {

    static final int CURRENT_SCHEMA_VERSION = 1

    Map<String, MoTypeInfo> read(File f) {
        Object parsed
        try {
            parsed = new JsonSlurper().parse(f)
        } catch (Exception e) {
            throw new RuntimeException("Snapshot ${f} is not valid JSON: ${e.message}", e)
        }
        if (!(parsed instanceof Map) || parsed.schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new RuntimeException(
                "Snapshot ${f} has schemaVersion ${parsed?.schemaVersion}, expected ${CURRENT_SCHEMA_VERSION}. " +
                "Rerun with --esx-url to regenerate.")
        }
        Map<String, MoTypeInfo> out = [:]
        parsed.types.each { tMap ->
            MoTypeInfo t = new MoTypeInfo()
            t.name = tMap.name
            t.parent = tMap.parent ?: ""
            tMap.properties.each { pMap ->
                MoProperty p = new MoProperty()
                p.name = pMap.name
                p.type = pMap.type
                p.referencedMoType = pMap.referencedMoType
                p.isArray = pMap.isArray as boolean
                p.isOptional = pMap.isOptional as boolean
                t.properties << p
            }
            tMap.methods.each { mMap ->
                MoMethod m = new MoMethod()
                m.name = mMap.name
                mMap.params.each { pdMap ->
                    MoMethodParam mp = new MoMethodParam()
                    mp.name = pdMap.name
                    mp.type = pdMap.type
                    mp.referencedMoType = pdMap.referencedMoType
                    mp.isArray = pdMap.isArray as boolean
                    mp.isOptional = pdMap.isOptional as boolean
                    m.params << mp
                }
                m.returnType = mMap.returnType
                m.referencedReturnMoType = mMap.referencedReturnMoType
                m.returnIsArray = mMap.returnIsArray as boolean
                m.faults = (mMap.faults ?: []) as List<String>
                t.methods << m
            }
            out[t.name] = t
        }
        return out
    }

    void write(File f, Map<String, MoTypeInfo> types, String vimVersion) {
        Map root = [
            schemaVersion: CURRENT_SCHEMA_VERSION,
            generatedAt:   new Date().toInstant().toString(),
            vimVersion:    vimVersion,
            types:         types.values().sort { it.name }.collect { typeToMap(it) }
        ]
        f.text = JsonOutput.prettyPrint(JsonOutput.toJson(root))
    }

    private Map typeToMap(MoTypeInfo t) {
        return [
            name:       t.name,
            parent:     t.parent,
            properties: t.properties.sort { it.name }.collect { p ->
                [name: p.name, type: p.type, referencedMoType: p.referencedMoType,
                 isArray: p.isArray, isOptional: p.isOptional]
            },
            methods:    t.methods.sort { it.name }.collect { m ->
                [name:       m.name,
                 params:     m.params.collect { pd ->
                     [name: pd.name, type: pd.type, referencedMoType: pd.referencedMoType,
                      isArray: pd.isArray, isOptional: pd.isOptional]
                 },
                 returnType: m.returnType,
                 referencedReturnMoType: m.referencedReturnMoType,
                 returnIsArray: m.returnIsArray,
                 faults:     (m.faults ?: []).sort()]
            }
        ]
    }
}
