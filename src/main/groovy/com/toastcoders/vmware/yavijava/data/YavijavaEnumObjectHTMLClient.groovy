package com.toastcoders.vmware.yavijava.data

import com.toastcoders.vmware.yavijava.contracts.YavijavaHTMLClientAbs
import org.apache.log4j.Logger
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

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
class YavijavaEnumObjectHTMLClient extends YavijavaHTMLClientAbs {

    private Logger log = Logger.getLogger(YavijavaEnumObjectHTMLClient)
    public Document document
    /**
     * Basic constructor
     */
    public YavijavaEnumObjectHTMLClient() {}

    /**
     * Constructor that takes a String
     * @param html String containing the HTML
     */
    public YavijavaEnumObjectHTMLClient(String html) {
        loadDocument(html)
    }

    /**
     * Constructor that will take a File object
     * @param htmlFile a Java File object that contains HTML
     */
    public YavijavaEnumObjectHTMLClient(File htmlFile) {
        loadDocument(htmlFile)
    }

    /**
     * Constructor that take a java.net.URL
     * @param url java.net.URL to parse
     */
    public YavijavaEnumObjectHTMLClient(URL url) {
        loadDocument(url)
    }
    @Override
    Document getDocument() {
        return document
    }

}
