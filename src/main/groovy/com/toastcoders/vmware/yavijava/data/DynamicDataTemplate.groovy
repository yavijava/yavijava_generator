package com.toastcoders.vmware.yavijava.data

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
class DynamicDataTemplate {

    public static String getPackage() {
        return "package com.vmware.vim25;\n"
    }

    public static String getImports() {
        String imports = "import lombok.Getter;\n"
        imports += "import lombok.Setter;\n"
        return imports
    }

    public static String getLicense() {
        Date today = new Date()
        return """
/**
 * Created by Michael Rice on ${today}
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
 * @since 6.0
 */\n\n"""
    }

    public static String getClassDef(String name, String extendsBase) {
        return "public class ${name} extends ${extendsBase} {\n"
    }
    public static String getPropertyType(String type, String name) {
        return "@Getter @Setter public ${type} ${name};\n"
    }

    public static String getMethodCreator(String type, String name) {
        return """public ${type} get${name.capitalize()}() {\n        return this.${name};\n    }\n\n"""
    }

    public static String setMethodCreator(String type, String name) {
        return """public void set${name.capitalize()}(${type} ${name}) {\n        this.${name} = ${name};\n    }\n\n"""
    }

    public static String closeClass() {
        return "}\n"
    }
}
