package com.toastcoders.vmware.yavijava.writer

import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
class WriteJavaClass {

    static final String GENERATOR_MARKER = "auto generated using yavijava_generator"
    private static final Logger log = LoggerFactory.getLogger(WriteJavaClass)

    static boolean writeFile(String name, String contents) {
        File file = new File(name)

        if (file.exists()) {
            String existing = file.text
            if (!existing.contains(GENERATOR_MARKER)) {
                log.warn("Skipping ${file.name}: file exists and lacks generator marker (likely hand-written)")
                return false
            }
        }

        file.withWriter("utf-8") { writer ->
            writer.write(contents)
        }
        return true
    }
}
