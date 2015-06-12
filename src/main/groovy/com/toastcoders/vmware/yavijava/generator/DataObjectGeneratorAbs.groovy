package com.toastcoders.vmware.yavijava.generator

import com.toastcoders.vmware.yavijava.contracts.Generator
import com.toastcoders.vmware.yavijava.contracts.HTMLClient
import com.toastcoders.vmware.yavijava.contracts.WSDLParser
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
class DataObjectGeneratorAbs implements Generator {

    @Override
    void generate() {
        generate(false)
    }

    @Override
    public void generate(boolean all) {
    }

    @Override
    void generate(boolean all, String packageName, Map nameSpace) {
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
            parser.parse(wsdl, nameSpace)
            DataObject dataObject = parser.dataObject
            String javaClass
            javaClass = DynamicDataTemplate.getPackageName(packageName)
            javaClass += DynamicDataTemplate.getImports()
            // look for calendar usage to include java.util.Calendar in imports
            if (dataObject.objProperties.find {it.propType == "Calendar"}) {
                javaClass += "import java.util.Calendar;\n"
            }
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
}
