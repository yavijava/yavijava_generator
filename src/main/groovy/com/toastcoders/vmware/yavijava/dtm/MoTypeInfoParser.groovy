package com.toastcoders.vmware.yavijava.dtm

import groovy.xml.XmlSlurper

class MoTypeInfoParser {

    MoTypeInfo parseSingle(String soapXml) {
        def env = new XmlSlurper().parseText(soapXml)
        def info = env.depthFirst().find { it.name() == "managedTypeInfo" }
        return parseManagedTypeInfo(info)
    }

    Map<String, MoTypeInfo> parseAll(String soapXml) {
        def env = new XmlSlurper().parseText(soapXml)
        Map<String, MoTypeInfo> out = [:]
        env.depthFirst().findAll { it.name() == "managedTypeInfo" }.each { info ->
            MoTypeInfo t = parseManagedTypeInfo(info)
            out[t.name] = t
        }
        return out
    }

    private MoTypeInfo parseManagedTypeInfo(Object info) {
        MoTypeInfo t = new MoTypeInfo()
        t.name = info.name.text()
        t.parent = info.base.text()
        info.property.each { p ->
            MoProperty prop = new MoProperty()
            prop.name = p.name.text()
            prop.type = p.type.text()
            prop.referencedMoType = p.referencedMoType.text() ?: null
            prop.isArray = (p.isArray.text() == "true")
            prop.isOptional = (p.isOptional.text() == "true")
            t.properties << prop
        }
        info.method.each { m ->
            MoMethod meth = new MoMethod()
            meth.name = m.name.text()
            m.paramDef.each { pd ->
                MoMethodParam mp = new MoMethodParam()
                mp.name = pd.name.text()
                mp.type = pd.type.text()
                mp.referencedMoType = pd.referencedMoType.text() ?: null
                mp.isArray = (pd.isArray.text() == "true")
                mp.isOptional = (pd.isOptional.text() == "true")
                meth.params << mp
            }
            def rti = m.returnTypeInfo[0]
            if (rti != null && !rti.isEmpty()) {
                String rt = rti.type.text()
                meth.returnType = (rt == "void") ? "void" : rt
                meth.referencedReturnMoType = rti.referencedMoType.text() ?: null
                meth.returnIsArray = (rti.isArray.text() == "true")
            } else {
                meth.returnType = "void"
            }
            m.fault.each { f -> meth.faults << f.text() }
            t.methods << meth
        }
        return t
    }
}
