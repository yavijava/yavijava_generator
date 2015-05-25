package com.toastcoders.vmware.yavijava.contracts

import com.toastcoders.vmware.yavijava.data.YavijavaDataObjectHTMLClient
import org.apache.log4j.Logger
import org.jsoup.Jsoup

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
abstract class YavijavaHTMLClientAbs implements HTMLClient {

    private Logger log = Logger.getLogger(YavijavaDataObjectHTMLClient)

    /**
     * Method to load the Document
     *
     * @param url URL for web page to parse
     * @throws IOException
     */
    @Override
    void loadDocument(URL url) throws IOException {
        log.debug("Loading HTML from URL")
        log.trace("URL: ${url.toString()}")
        document = Jsoup.parse(url, 20000)
        assert document != null
        log.debug("Loaded HTML from URL successfully.")
    }

    /**
     * Loads a document object from a File object
     *
     * @param file
     * @throws IOException
     */
    @Override
    void loadDocument(File file) throws IOException {
        log.debug("Loading HTML from file.")
        log.trace("File name: ${file.name}")
        assert file.canRead()
        document = Jsoup.parse(file, null)
        assert document != null
        log.trace("Loaded HTML from file successfully.")
    }

    /**
     * Loads a Document from a String that contains the HTML file
     * @param html
     * @throws IOException
     */
    @Override
    void loadDocument(String html) throws IOException {
        log.debug("Loading HTML from String")
        log.trace("HTML string: ${html}")
        document = Jsoup.parse((html.toString() as String))
        assert document != null
        log.trace("Loaded HTML from string successfully.")
    }
}
