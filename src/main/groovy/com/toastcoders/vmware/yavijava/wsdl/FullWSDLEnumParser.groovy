package com.toastcoders.vmware.yavijava.wsdl

import groovy.xml.slurpersupport.GPathResult
import com.toastcoders.vmware.yavijava.data.DataObject

class FullWSDLEnumParser {

    List<DataObject> parse(List<GPathResult> schemas) {
        List<DataObject> result = []
        schemas.each { schema ->
            schema.simpleType.each { st ->
                String name = st.'@name'.text()
                if (!name) return

                def enumerations = st.restriction.enumeration
                if (!enumerations.size()) return

                DataObject obj = new DataObject()
                obj.name = name
                obj.extendsBase = ''
                enumerations.each { e ->
                    obj.objProperties << e.'@value'.text()
                }
                result << obj
            }
        }
        return result
    }
}
