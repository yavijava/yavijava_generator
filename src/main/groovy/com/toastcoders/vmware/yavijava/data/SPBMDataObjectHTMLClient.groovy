package com.toastcoders.vmware.yavijava.data

import com.toastcoders.vmware.yavijava.contracts.YavijavaHTMLClientAbs
import org.apache.log4j.Logger
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import sun.reflect.generics.reflectiveObjects.NotImplementedException

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
class SPBMDataObjectHTMLClient extends YavijavaHTMLClientAbs {

    private Logger log = Logger.getLogger(SPBMDataObjectHTMLClient)
    public Document document
    /**
     * Basic constructor
     */
    public SPBMDataObjectHTMLClient() {}

    /**
     * Constructor that takes a String
     * @param html String containing the HTML
     */
    public SPBMDataObjectHTMLClient(String html) {
        loadDocument(html)
    }

    /**
     * Constructor that will take a File object
     * @param htmlFile a Java File object that contains HTML
     */
    public SPBMDataObjectHTMLClient(File htmlFile) {
        loadDocument(htmlFile)
    }

    /**
     * Constructor that take a java.net.URL
     * @param url java.net.URL to parse
     */
    public SPBMDataObjectHTMLClient(URL url) {
        loadDocument(url)
    }

    @Override
    Document getDocument() {
        return document
    }

    @Override
    Map getAllObjects() {
        assert document != null
        log.debug("Fetching new DataObjects from SPBM Document")
        Element[] fonts = document.select("font")
        // the last <font tag had all the elements we wanted as children.
        Element secondFont = fonts[-1]
        Map retMap = [:]
        secondFont.each { fontChildren ->
            Element[] nobrs = fontChildren.select("nobr")
            nobrs.each { nobr ->
                Element link = nobr.select("a").first()
                retMap.put(link.text(), link.attr("href"))
            }
        }
        return retMap
    }

    @Override
    Map getNewObjects() {
        // In the 6.0 docs this doesnt seem to be a thing yet..
        throw new NotImplementedException()
    }
}
