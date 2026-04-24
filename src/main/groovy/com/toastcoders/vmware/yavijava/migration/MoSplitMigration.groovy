package com.toastcoders.vmware.yavijava.migration

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.toastcoders.vmware.yavijava.generator.DTMManagedObjectGenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MoSplitMigration {

    private static final Logger log = LoggerFactory.getLogger(MoSplitMigration)
    private static final String MARKER = "// auto generated using yavijava_generator"
    private final MoMemberClassifier classifier = new MoMemberClassifier()

    void run(String yavijavaSrcRoot) {
        File moDir = new File(yavijavaSrcRoot, "com/vmware/vim25/mo")
        if (!moDir.isDirectory()) {
            throw new RuntimeException("Migration root has no com/vmware/vim25/mo: ${yavijavaSrcRoot}")
        }
        int migrated = 0, skipped = 0
        moDir.eachFile { f ->
            if (!f.name.endsWith(".java")) return
            String typeName = f.name.replace(".java", "")
            if (typeName.endsWith("Base")) {
                skipped++
                return
            }
            if (DTMManagedObjectGenerator.MIGRATION_EXCLUDE.contains(typeName)) {
                log.info("migration: skipping infrastructure file ${f.name}")
                skipped++
                return
            }
            if (migrateOne(f, typeName)) migrated++ else skipped++
        }
        File vimStub = new File(yavijavaSrcRoot, "com/vmware/vim25/ws/VimStub.java")
        if (vimStub.exists() && !vimStub.text.contains(MARKER)) {
            insertVimStubMarker(vimStub)
            log.info("migration: inserted marker into VimStub.java")
        }
        log.info("migration: ${migrated} migrated, ${skipped} skipped")
    }

    private boolean migrateOne(File file, String typeName) {
        String original = file.text
        if (original.contains("extends ${typeName}Base")) {
            log.info("migration: already migrated ${file.name}")
            return false
        }
        CompilationUnit cu
        try {
            cu = StaticJavaParser.parse(original)
        } catch (Exception e) {
            log.warn("migration: cannot parse ${file.name}: ${e.message}")
            return false
        }
        ClassOrInterfaceDeclaration cls = cu.getType(0) as ClassOrInterfaceDeclaration
        if (cls.extendedTypes.isEmpty()) {
            log.info("migration: skipping non-MO (no extends): ${file.name}")
            return false
        }

        List<BodyDeclaration<?>> baseMembers = []
        List<BodyDeclaration<?>> subclassMembers = []
        cls.members.each { m ->
            if (m instanceof ConstructorDeclaration) {
                subclassMembers << m
            } else if (m instanceof MethodDeclaration && classifier.isAutoGeneratable(m as MethodDeclaration)) {
                baseMembers << m
            } else {
                subclassMembers << m
            }
        }

        new File(file.parentFile, file.name + ".bak").text = original

        String baseSource = renderBase(cu, cls, typeName, baseMembers)
        new File(file.parentFile, "${typeName}Base.java").text = baseSource

        String subSource = renderSubclass(cu, cls, typeName, subclassMembers)
        file.text = subSource
        return true
    }

    private String renderBase(CompilationUnit origCu, ClassOrInterfaceDeclaration origCls,
                              String typeName, List<BodyDeclaration<?>> members) {
        CompilationUnit baseCu = origCu.clone()
        ensureImport(baseCu, "com.vmware.vim25.ManagedObjectReference")
        ClassOrInterfaceDeclaration baseCls = baseCu.getType(0) as ClassOrInterfaceDeclaration
        baseCls.setName("${typeName}Base")
        baseCls.members.clear()
        baseCls.addConstructor(com.github.javaparser.ast.Modifier.Keyword.PUBLIC)
            .addParameter("ServerConnection", "serverConnection")
            .addParameter("ManagedObjectReference", "mor")
            .createBody()
            .addStatement("super(serverConnection, mor);")
        members.each { baseCls.addMember(it.clone()) }
        return MARKER + "\n" + baseCu.toString()
    }

    private void ensureImport(CompilationUnit cu, String fqn) {
        boolean already = cu.imports.any { it.nameAsString == fqn }
        if (!already) cu.addImport(fqn)
    }

    private String renderSubclass(CompilationUnit origCu, ClassOrInterfaceDeclaration origCls,
                                  String typeName, List<BodyDeclaration<?>> members) {
        CompilationUnit subCu = origCu.clone()
        ClassOrInterfaceDeclaration subCls = subCu.getType(0) as ClassOrInterfaceDeclaration
        subCls.setExtendedTypes(com.github.javaparser.ast.NodeList.nodeList(
            com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType("${typeName}Base")))
        subCls.members.clear()
        members.each { subCls.addMember(it.clone()) }
        return subCu.toString()
    }

    private void insertVimStubMarker(File vimStub) {
        List<String> lines = vimStub.readLines()
        int insertAt = lines.findIndexOf { it.trim().startsWith("package ") } + 1
        lines.add(insertAt, MARKER)
        vimStub.text = lines.join(System.lineSeparator()) + System.lineSeparator()
    }
}
