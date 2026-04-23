package com.toastcoders.vmware.yavijava.data

class ArrayOfTemplate extends BaseTemplate {

    static String getClassDef(String name) {
        "public class ${name} {\n"
    }

    static String getField(String type, String name) {
        "    public ${type}[] ${name};\n\n"
    }

    static String getArrayGetter(String type, String name) {
        "    public ${type}[] get${name.capitalize()}() {\n" +
        "        return this.${name};\n" +
        "    }\n\n"
    }

    static String getIndexedGetter(String type, String name) {
        "    public ${type} get${name.capitalize()}(int i) {\n" +
        "        return this.${name}[i];\n" +
        "    }\n\n"
    }

    static String getSetter(String type, String name) {
        "    public void set${name.capitalize()}(${type}[] ${name}) {\n" +
        "        this.${name} = ${name};\n" +
        "    }\n"
    }
}
