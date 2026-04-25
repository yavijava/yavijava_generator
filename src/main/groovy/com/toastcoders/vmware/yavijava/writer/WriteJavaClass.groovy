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

    static final String CUSTOM_IMPORTS_BEGIN = "/* ===== BEGIN custom imports (preserved by regenerator) ===== */"
    static final String CUSTOM_IMPORTS_END   = "/* ===== END custom imports ===== */"
    static final String CUSTOM_BEGIN         = "/* ===== BEGIN custom (preserved by regenerator) ===== */"
    static final String CUSTOM_END           = "/* ===== END custom ===== */"

    static boolean writeFileWithFence(String name, String generated) {
        File file = new File(name)
        String toWrite = generated
        if (file.exists()) {
            String existing = file.text
            if (!existing.contains(GENERATOR_MARKER)) {
                log.warn("Skipping ${file.name}: file exists and lacks generator marker (likely hand-written)")
                return false
            }
            // Validate fence integrity in the existing file
            String importsContent = extractFence(existing, CUSTOM_IMPORTS_BEGIN, CUSTOM_IMPORTS_END, file.name)
            if (importsContent == null) return false
            String customContent = extractFence(existing, CUSTOM_BEGIN, CUSTOM_END, file.name)
            if (customContent == null) return false
            // Splice extracted content into the generated template's empty fences
            toWrite = spliceFence(toWrite, CUSTOM_IMPORTS_BEGIN, CUSTOM_IMPORTS_END, importsContent)
            toWrite = spliceFence(toWrite, CUSTOM_BEGIN, CUSTOM_END, customContent)
        }
        file.withWriter("utf-8") { it.write(toWrite) }
        return true
    }

    /** Returns the bytes between begin and end markers (exclusive), or null on integrity error. */
    private static String extractFence(String content, String begin, String end, String fileName) {
        int beginCount = countOccurrences(content, begin)
        int endCount   = countOccurrences(content, end)
        if (beginCount == 0 && endCount == 0) {
            return ""  // No fence at all is valid (no preservation needed)
        }
        if (beginCount != 1 || endCount != 1) {
            log.error("Refusing to rewrite ${fileName}: fence markers malformed (expected 1 BEGIN+END pair, found ${beginCount} BEGINs and ${endCount} ENDs for ${begin}/${end})")
            return null
        }
        int s = content.indexOf(begin) + begin.length()
        int e = content.indexOf(end)
        if (s >= e) {
            log.error("Refusing to rewrite ${fileName}: fence END appears before BEGIN")
            return null
        }
        return content.substring(s, e)
    }

    /** Replace the empty fence in generated with extracted content. */
    private static String spliceFence(String generated, String begin, String end, String content) {
        if (content == null || content.isEmpty()) return generated
        int s = generated.indexOf(begin)
        int e = generated.indexOf(end)
        if (s < 0 || e < 0) return generated  // generator didn't include this fence
        return generated.substring(0, s + begin.length()) + content + generated.substring(e)
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0
        int idx = 0
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++
            idx += needle.length()
        }
        return count
    }
}
