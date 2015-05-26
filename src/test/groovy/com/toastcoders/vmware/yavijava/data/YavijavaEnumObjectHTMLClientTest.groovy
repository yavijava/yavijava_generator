package com.toastcoders.vmware.yavijava.data

import com.toastcoders.vmware.yavijava.contracts.HTMLClient

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
class YavijavaEnumObjectHTMLClientTest extends GroovyTestCase {

    File htmlFile
    HTMLClient enumClient

    @Override
    protected void setUp() throws Exception {
        this.htmlFile = new File("src/test/resources/new-enum-types-landing.html")
        enumClient = new YavijavaEnumObjectHTMLClient(htmlFile)
    }

    void testGetNewEnumObjects() {
        Map newEnums = enumClient.newObjects
        assert newEnums.size() == 37
    }

    void testGetAllEnums() {
        Map newEnums = enumClient.allObjects
        assert newEnums.size() == 52
    }
}
