package com.toastcoders.vmware.yavijava.parsers

import com.toastcoders.vmware.yavijava.contracts.WSDLParser
import com.toastcoders.vmware.yavijava.data.Property
import com.toastcoders.vmware.yavijava.data.YavijavaDataObjectHTMLClient
import com.toastcoders.vmware.yavijava.parsers.DataObjectWSDLParserImpl
import org.junit.Before
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

    WSDLParser parser
    List props

    @Before
    public void setUp() throws Exception {
        parser = new DataObjectWSDLParserImpl()
        File html = new File('src/test/resources/YavijavaClientTest.html')
        String wsdl = new YavijavaDataObjectHTMLClient(html).WSDLDefXML
        parser.parse(wsdl)
        props = this.parser.dataObject.objProperties
    }

    @Test
    public void testParse() throws Exception {
        assert this.parser.dataObject.name == "BatchResult"
    }

    @Test
    public void testParserCatchesLongObjectWhenNeeded() throws Exception {
        Property myLongProp = props.find {it.name == "checkLong"} as Property
        assert myLongProp.propType == "Long"
    }

    @Test
    public void testParserMakesLongArrayCorrectly() throws Exception {
        Property myLongArray = props.find {it.name == "longArray"} as Property
        assert myLongArray.propType == "long[]"
    }

    @Test
    public void testParserMakesIntegerWhenIntIsOptional() throws Exception {
        Property myInt = props.find {it.name == "optionalInt"} as Property
        assert myInt.propType == "Integer"
    }

    @Test
    public void testParserMakesIntArrayWhenIntIsOptional() throws Exception {
        Property myOptionalIntArray = props.find {it.name == "optionalIntArray"} as Property
        assert myOptionalIntArray.propType == "int[]"
    }
}
