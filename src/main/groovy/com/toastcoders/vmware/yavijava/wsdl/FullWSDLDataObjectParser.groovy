package com.toastcoders.vmware.yavijava.wsdl

import groovy.xml.slurpersupport.GPathResult
import com.toastcoders.vmware.yavijava.data.DataObject
import com.toastcoders.vmware.yavijava.data.Property

class FullWSDLDataObjectParser {

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

    private static final Map<String, String> BOXED_TYPE_MAP = [
        int    : 'Integer',
        long   : 'Long',
        boolean: 'Boolean',
        float  : 'Float',
        double : 'Double',
        short  : 'Short',
        byte   : 'Byte',
    ].asImmutable()

    List<DataObject> parse(List<GPathResult> schemas) {
        List<DataObject> result = []
        schemas.each { schema ->
            schema.complexType.each { ct ->
                String name = ct.'@name'.text()
                if (!name) return

                // Only handle types with complexContent/extension. ArrayOf-style
                // bare-sequence types are processed by FullWSDLArrayOfParser.
                if (!ct.complexContent.extension.size()) return

                DataObject obj = new DataObject()
                obj.name = name

                String rawBase = ct.complexContent.extension.'@base'.text()
                obj.extendsBase = rawBase ? rawBase.split(':')[-1] : ''

                ct.complexContent.extension.sequence.element.each { el ->
                    obj.objProperties << parseProperty(el)
                }

                result << obj
            }
        }
        return result
    }

    private Property parseProperty(el) {
        String name      = el.'@name'.text()
        String rawType   = el.'@type'.text()
        String minOccurs = el.'@minOccurs'.text()
        String maxOccurs = el.'@maxOccurs'.text()

        String objType = rawType.split(':')[-1]

        if (PRIMITIVE_TYPE_MAP.containsKey(objType)) {
            objType = (minOccurs == '0' && BOXED_TYPE_MAP.containsKey(objType)) \
                ? BOXED_TYPE_MAP[objType] \
                : PRIMITIVE_TYPE_MAP[objType]
        }

        if (maxOccurs == 'unbounded') {
            String unboxed = BOXED_TYPE_MAP.find { k, v -> v == objType }?.key
            if (unboxed) objType = PRIMITIVE_TYPE_MAP[unboxed]
            objType = objType + '[]'
        }

        int min = (minOccurs && minOccurs.isNumber()) ? (minOccurs as int) : 0
        return min > 0 ? new Property(name, objType, min) : new Property(name, objType)
    }
}
