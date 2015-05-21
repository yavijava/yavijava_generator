package com.toastcoders.vmware.yavijava

import com.toastcoders.vmware.yavijava.contracts.HTMLClient
import com.toastcoders.vmware.yavijava.contracts.WSDLParser
import com.toastcoders.vmware.yavijava.data.DataObject
import com.toastcoders.vmware.yavijava.data.DynamicDataTemplate

/**
 * Created by Michael Rice on 5/20/15.
 *
 * Copyright 2015 Michael Rice
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class Main {

    public static void main(String[] args) {

        def cli = new CliBuilder(usage: 'yavijava_generator')
        cli.h(longOpt: 'help', 'usage information', required: false)
        cli._(longOpt: 'source', 'Source to read from', required: true, args: 1)
        cli._(longOpt: 'dest', 'Destination path for where to write new files', required: true, args: 1)
        def opt = cli.parse(args)
        assert opt != null
        if (opt.h) {
            cli.usage()
            System.exit(1)
        }
        String source = opt.source
        String dest = opt.dest
        // This should be the new-do-types-landing.html
        File htmlFile = new File(source)
        String base = htmlFile.getParent()
        HTMLClient client = new YavijavaDataObjectHTMLClient(htmlFile)
        Map newDataObjects = client.getNewDataObjects()

        //Iterate through the map to open each new DO html file
        newDataObjects.each { name, doHTMLFile ->
            File doFile = new File(base + htmlFile.separator + doHTMLFile)
            assert doFile.canRead()
            WSDLParser parser = new DataObjectWSDLParserImpl()
            String wsdl = new YavijavaDataObjectHTMLClient(doFile).WSDLDefXML
            parser.parse(wsdl)
            DataObject dataObject = parser.dataObject
            String javaClass
            javaClass = DynamicDataTemplate.getPackage()
            javaClass += DynamicDataTemplate.getLicense()
            javaClass += DynamicDataTemplate.getClassDef(dataObject.name, dataObject.extendsBase)
            dataObject.objProperties.each {
                javaClass += "    ${DynamicDataTemplate.getPropertyType(it.propType, it.name)}"
            }
            dataObject.objProperties.each {
                javaClass += "    ${DynamicDataTemplate.getMethodCreator(it.propType, it.name)}"
                javaClass += "    ${DynamicDataTemplate.setMethodCreator(it.propType, it.name)}"
            }
            javaClass += DynamicDataTemplate.closeClass()
            String fileName = dest + dataObject.name + ".java"
            WriteJavaClass.writeFile(fileName, javaClass)
        }
    }
}
