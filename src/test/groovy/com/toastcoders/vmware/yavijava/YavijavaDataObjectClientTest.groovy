package com.toastcoders.vmware.yavijava

import com.toastcoders.vmware.yavijava.contracts.HTMLClient
import org.jsoup.nodes.Document
import org.junit.Test

/**
 * Created by Michael Rice on 5/19/15.
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
public class YavijavaDataObjectClientTest {

//    @Test
    public void testLoadDocumentByUrl() throws Exception {
        URL url = new URL("http://www.google.com")
        HTMLClient client = new YavijavaDataObjectHTMLClient()
        client.loadDocument(url)
        Document document = client.getDocument()
        assert document.title() == "Google"
    }

    @Test
    public void testLoadDocumentByString() throws Exception {
        String html = "<html><head><title>Foo Bar</title></head><body>My body</body></html>"
        HTMLClient client = new YavijavaDataObjectHTMLClient(html)
        assert client.document.title() == "Foo Bar"
    }

    @Test
    public void testLoadDocumentByFile() throws Exception {
        File htmlFile = new File("src/test/resources/YavijavaClientTest.html")
        assert htmlFile.canRead()
        HTMLClient client = new YavijavaDataObjectHTMLClient(htmlFile)
        assert client.document.title() == "Test File"
    }

    @Test
    public void testGetWsdlContent() {
        File htmlFile = new File("src/test/resources/YavijavaClientTest.html")
        assert htmlFile.canRead()
        HTMLClient client = new YavijavaDataObjectHTMLClient(htmlFile)
        assert client.WSDLDefXML.contains("<complexType xmlns")
    }

    @Test
    public void testGetNewDataObjects() {
        File htmlFile = new File("src/test/resources/new-do-types-landing.html")
        assert htmlFile.canRead()
        HTMLClient client = new YavijavaDataObjectHTMLClient(htmlFile)
        Map newDataObjects = client.getNewDataObjects()
        assert newDataObjects.size() == 132
    }
}
