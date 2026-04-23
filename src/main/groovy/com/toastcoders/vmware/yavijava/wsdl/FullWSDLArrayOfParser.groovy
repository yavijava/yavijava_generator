package com.toastcoders.vmware.yavijava.wsdl

import groovy.xml.slurpersupport.GPathResult
import com.toastcoders.vmware.yavijava.data.DataObject
import com.toastcoders.vmware.yavijava.data.Property

class FullWSDLArrayOfParser {

    List<DataObject> parse(List<GPathResult> schemas) {
        List<DataObject> result = []
        schemas.each { schema ->
            schema.complexType.each { ct ->
                String name = ct.'@name'.text()
                if (!name) return
                if (!name.startsWith('ArrayOf')) return

                // ArrayOf types: bare <sequence>, no <complexContent><extension>
                if (ct.complexContent.extension.size()) return
                def seqElement = ct.sequence.element
                if (!seqElement.size()) return

                DataObject obj = new DataObject()
                obj.name = name
                obj.extendsBase = ''

                def el = seqElement[0]
                String elementName = el.'@name'.text()
                String elementType = el.'@type'.text().split(':')[-1]
                obj.objProperties << new Property(elementName, elementType)

                result << obj
            }
        }
        return result
    }
}
