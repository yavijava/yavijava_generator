package com.toastcoders.vmware.yavijava

import com.toastcoders.vmware.yavijava.contracts.WSDLParser
import org.junit.Test

/**
 * Created by Michael Rice on 5/20/15.
 * <p/>
 * Copyright 2015 Michael Rice
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class DataObjectWSDLParserImplTest {

    @Test
    public void testParse() throws Exception {
        WSDLParser parser = new DataObjectWSDLParserImpl()
        File html = new File('src/test/resources/YavijavaClientTest.html')
        String wsdl = new YavijavaDataObjectHTMLClient(html).WSDLDefXML
        parser.parse(wsdl)
        assert parser.dataObject.name == "BatchResult"
    }
}
