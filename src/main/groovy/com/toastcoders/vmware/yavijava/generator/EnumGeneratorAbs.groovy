package com.toastcoders.vmware.yavijava.generator

import com.toastcoders.vmware.yavijava.contracts.Generator
import com.toastcoders.vmware.yavijava.contracts.HTMLClient
import com.toastcoders.vmware.yavijava.contracts.WSDLParser
import com.toastcoders.vmware.yavijava.data.DataObject
import com.toastcoders.vmware.yavijava.data.EnumJavaTemplate
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
class EnumGeneratorAbs implements Generator {

    @Override
    void generate() {
        generate(false)
    }

    @Override
    void generate(boolean all) {
        generate(all, "com.vmware.vim25;\n", [vim25: 'xmlns:vim25="urn:vim25"'])
    }

    @Override
    void generate(boolean all, String packageName, Map nameSpace) {
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
            parser.parse(wsdl, nameSpace)
            DataObject dataObject = parser.dataObject
            String javaClass
            javaClass = EnumJavaTemplate.getPackageName(packageName)
            javaClass += EnumJavaTemplate.getLicense()
            javaClass += EnumJavaTemplate.getClassDef(dataObject.name)
            String propEnding = ","
            int numProps = dataObject.objProperties.size()
            if (numProps == 1) {
                propEnding = ";"
            }
            dataObject.objProperties.each {
                javaClass += EnumJavaTemplate.getEnumProp(it.toString(), propEnding)
                numProps -= 1
                if (numProps == 1) {
                    propEnding = ";"
                }
            }
            javaClass += EnumJavaTemplate.getPrivVal()
            javaClass += EnumJavaTemplate.constructorGenerator(dataObject.name)
            javaClass += EnumJavaTemplate.toStringGenerator()
            javaClass += EnumJavaTemplate.closeClass()
            String fileName = dest + dataObject.name + ".java"
            WriteJavaClass.writeFile(fileName, javaClass)
        }
    }
}
