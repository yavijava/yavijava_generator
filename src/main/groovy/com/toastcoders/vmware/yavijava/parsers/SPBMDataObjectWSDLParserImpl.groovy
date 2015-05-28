package com.toastcoders.vmware.yavijava.parsers

import com.toastcoders.vmware.yavijava.contracts.WSDLParser
import com.toastcoders.vmware.yavijava.data.DataObject
import com.toastcoders.vmware.yavijava.data.Property

/**
 *  Copyright 2015 Michael Rice <michael@michaelrice.org>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
class SPBMDataObjectWSDLParserImpl implements WSDLParser {

    DataObject dataObject
    @Override
    void parse(String wsdl) {
        XmlSlurper slurper = new XmlSlurper()
        def doc = slurper.parseText(wsdl).declareNamespace(['pbm': 'xmlns:pbm="urn:pbm"'])
        String className = doc."@name"
        dataObject = new DataObject()
        dataObject.name = className

        String extendsBase = doc.'complexContent'.'extension'.'@base'
        dataObject.extendsBase = extendsBase.split(":")[-1]

        def props = doc.'complexContent'.'extension'.'sequence'.'element'
        String name
        String objType
        int min
        props.each {
            name = it.'@name'
            objType = it.'@type'
            objType = objType.split(":")[-1]
            if (objType == "string") {
                objType = objType.capitalize()
            }
            else if (objType == "base64Binary") {
                objType = "byte[]"
            }
            else if (objType == "dateTime") {
                objType = "Calendar"
            }
            else if (objType == "int" && it."@minOccurs" == "0") {
                objType = "Integer"
            }
            else if (objType == "anyType") {
                objType = "Object"
            }
            if (it.'@maxOccurs' == 'unbounded') {
                objType = objType + "[]"
            }
            if (it.'@minOccurs' == 'unbounded') {
                objType = objType + "[]"
            }
            else if (it.'@minOccurs' != '' && (it.'@minOccurs' as String).isNumber()) {
                if (objType == "boolean") {
                    objType = objType.capitalize()
                }
                min = ((it.'@minOccurs' as String) as int)
            }
            if (min) {
                dataObject.objProperties << new Property(name, objType, min)
            }
            else {
                dataObject.objProperties << new Property(name, objType)
            }
        }
    }
}
