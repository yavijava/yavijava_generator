package com.toastcoders.vmware.yavijava

import org.apache.log4j.Logger
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Created by Michael Rice on 5/19/15.
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
class YavijavaDataObjectHTMLClient extends YavijavaHTMLClientAbs {

    private Logger log = Logger.getLogger(YavijavaDataObjectHTMLClient)
    public Document document
    /**
     * Basic constructor
     */
    public YavijavaDataObjectHTMLClient() {}

    /**
     * Constructor that takes a String
     * @param html String containing the HTML
     */
    public YavijavaDataObjectHTMLClient(String html) {
        loadDocument(html)
    }

    /**
     * Constructor that will take a File object
     * @param htmlFile a Java File object that contains HTML
     */
    public YavijavaDataObjectHTMLClient(File htmlFile) {
        loadDocument(htmlFile)
    }

    /**
     * Constructor that take a java.net.URL
     * @param url java.net.URL to parse
     */
    public YavijavaDataObjectHTMLClient(URL url) {
        loadDocument(url)
    }

    /**
     * Return the Document object
     * @return
     */
    @Override
    Document getDocument() {
        return document
    }

    /**
     * Returns the WSDL XML def from a DataObject
     * @return
     */
    @Override
    String getWSDLDefXML() {
        assert document != null
        log.debug("Fetching WSDL Def from Document.")
        Element textArea = document.select("textarea[name=wsdl-textarea]").first()
        return textArea.text()
    }

    Map getNewDataObjects() {
        assert document != null
        log.debug("Fetching new DataObjects from Document")
        Element table = document.select("table").first()
        Element[] tRows = table.select("tr")
        Map retMap = [:]
        tRows.each { row ->
            Element[] cols = row.select("td")
            cols.each { col ->
                Element link = col.select("a").first()
                retMap.put(link.text(), link.attr("href"))
            }
        }
        return retMap
    }
}
