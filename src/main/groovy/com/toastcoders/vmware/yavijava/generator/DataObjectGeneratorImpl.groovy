package com.toastcoders.vmware.yavijava.generator

import com.toastcoders.vmware.yavijava.contracts.HTMLClient
import com.toastcoders.vmware.yavijava.parsers.DataObjectWSDLParserImpl
import com.toastcoders.vmware.yavijava.data.YavijavaDataObjectHTMLClient
import com.toastcoders.vmware.yavijava.contracts.WSDLParser
import com.toastcoders.vmware.yavijava.contracts.Generator
import com.toastcoders.vmware.yavijava.data.DataObject
import com.toastcoders.vmware.yavijava.data.DynamicDataTemplate
import com.toastcoders.vmware.yavijava.writer.WriteJavaClass

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
class DataObjectGeneratorImpl implements Generator {

    String source
    String dest

    DataObjectGeneratorImpl(String source, String dest) {
        this.source = source
        this.dest = dest
    }

    @Override
    void generate() {
        generate(false)
    }

    @Override
    public void generate(boolean all) {
        // This should be the new-do-types-landing.html
        File htmlFile = loadFile(this.source)
        String base = htmlFile.getParent()
        HTMLClient client = loadHTMLClient(htmlFile)
        Map myDataObjects
        if (all) {
           myDataObjects = client.getAllObjects()
        }
        else {
            myDataObjects = client.getNewObjects()
        }
        //Iterate through the map to open each new DO html file
        myDataObjects.each { name, doHTMLFile ->
            File doFile = loadFile(base + htmlFile.separator + doHTMLFile)
            assert doFile.canRead()
            WSDLParser parser = loadWSDLParser()
            String wsdl = loadWSDLFromDOFile(doFile)
            parser.parse(wsdl)
            DataObject dataObject = parser.dataObject
            String javaClass
            javaClass = DynamicDataTemplate.getPackageName()
            javaClass += DynamicDataTemplate.getImports()
            javaClass += DynamicDataTemplate.getLicense()
            javaClass += DynamicDataTemplate.getClassDef(dataObject.name, dataObject.extendsBase)
            dataObject.objProperties.each {
                javaClass += "    ${DynamicDataTemplate.getPropertyType(it.propType, it.name)}"
            }
            javaClass += DynamicDataTemplate.closeClass()
            String fileName = dest + dataObject.name + ".java"
            WriteJavaClass.writeFile(fileName, javaClass)
        }
    }

    protected File loadFile(String source) {
        return new File(source)
    }

    protected HTMLClient loadHTMLClient(File htmlFile) {
        return new YavijavaDataObjectHTMLClient(htmlFile)
    }

    protected WSDLParser loadWSDLParser() {
        return new DataObjectWSDLParserImpl()
    }

    protected String loadWSDLFromDOFile(File doFile) {
        return loadHTMLClient(doFile).WSDLDefXML
    }
}
