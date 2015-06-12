package com.toastcoders.vmware.yavijava.generator

import com.toastcoders.vmware.yavijava.contracts.Generator
import com.toastcoders.vmware.yavijava.contracts.HTMLClient
import com.toastcoders.vmware.yavijava.contracts.WSDLParser
import com.toastcoders.vmware.yavijava.data.DataObject
import com.toastcoders.vmware.yavijava.data.EnumJavaTemplate
import com.toastcoders.vmware.yavijava.data.YavijavaEnumObjectHTMLClient
import com.toastcoders.vmware.yavijava.parsers.EnumWSDLParserImpl
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
class EnumGeneratorImpl extends EnumGeneratorAbs {

    String source
    String dest

    EnumGeneratorImpl(String source, String dest) {
        this.source = source
        this.dest = dest
    }

    protected File loadFile(String source) {
        return new File(source)
    }

    protected HTMLClient loadHTMLClient(File htmlFile) {
        return new YavijavaEnumObjectHTMLClient(htmlFile)
    }

    protected WSDLParser loadWSDLParser() {
        return new EnumWSDLParserImpl()
    }

    protected String loadWSDLFromDOFile(File doFile) {
        return loadHTMLClient(doFile).WSDLDefXML
    }
}
