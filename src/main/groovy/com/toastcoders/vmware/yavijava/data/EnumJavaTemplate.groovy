package com.toastcoders.vmware.yavijava.data

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
class EnumJavaTemplate extends BaseTemplate {

    public static String getClassDef(String enumName) {
        return """public enum ${enumName} {\n\n"""
    }

    public static String constructorGenerator(String enumName) {
        return """    ${enumName}(String val) {\n        this.val = val;\n    }\n\n"""
    }

    public static String toStringGenerator() {
        return """    @Override\n    public String toString() {\n        return this.val;\n    }\n"""
    }

    public static String getEnumProp(String propName, String endnig) {
        return """    ${propName}("${propName}")${endnig}\n"""
    }

    public static String getPrivVal() {
        return """\n    private String val;\n\n"""
    }
}
