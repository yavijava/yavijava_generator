package com.toastcoders.vmware.yavijava.wsdl

import groovy.xml.slurpersupport.GPathResult
import com.toastcoders.vmware.yavijava.data.DataObject
import com.toastcoders.vmware.yavijava.data.Property

class FullWSDLArrayOfParser {

    private static final Map<String, String> PRIMITIVE_TYPE_MAP = [
        string      : 'String',
        base64Binary: 'byte[]',
        dateTime    : 'Calendar',
        anyType     : 'Object',
        anyURI      : 'String',
        boolean     : 'boolean',
        int         : 'int',
        long        : 'long',
        float       : 'float',
        double      : 'double',
        short       : 'short',
        byte        : 'byte',
    ].asImmutable()

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
                String rawElementType = el.'@type'.text().split(':')[-1]
                String elementType = PRIMITIVE_TYPE_MAP[rawElementType] ?: rawElementType
                obj.objProperties << new Property(elementName, elementType)

                result << obj
            }
        }
        return result
    }
}
