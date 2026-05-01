package com.toastcoders.vmware.yavijava.migration

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.toastcoders.vmware.yavijava.generator.PyvmomiManagedObjectGenerator
import com.toastcoders.vmware.yavijava.pyvmomi.PyvmomiManagedObject
import com.toastcoders.vmware.yavijava.pyvmomi.PyvmomiSchema
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PyvmomiMigrate {

    private static final Logger log = LoggerFactory.getLogger(PyvmomiMigrate)
    private static final String MARKER = "// auto generated using yavijava_generator"
    private static final String CUSTOM_IMPORTS_BEGIN = "/* ===== BEGIN custom imports (preserved by regenerator) ===== */"
    private static final String CUSTOM_IMPORTS_END   = "/* ===== END custom imports ===== */"
    private static final String CUSTOM_BEGIN         = "/* ===== BEGIN custom (preserved by regenerator) ===== */"
    private static final String CUSTOM_END           = "/* ===== END custom ===== */"

    /** Imports the regenerator template emits unconditionally; never relocate these. */
    private static final Set<String> STANDARD_IMPORTS = [
        "com.vmware.vim25.*",
        "java.rmi.RemoteException",
        "java.util.Calendar",
    ] as Set

    private final MoMemberClassifier classifier = new MoMemberClassifier()

    void run(String schemaPath, String yavijavaSrcRoot) {
        Map<String, PyvmomiManagedObject> mos = new PyvmomiSchema().read(new File(schemaPath))
        File moDir = new File(yavijavaSrcRoot, "com/vmware/vim25/mo")
        if (!moDir.isDirectory()) {
            throw new RuntimeException("Migration root has no com/vmware/vim25/mo: ${yavijavaSrcRoot}")
        }
        int migrated = 0, skippedAlready = 0, skippedExcluded = 0, skippedMissing = 0
        mos.values().sort { it.name }.each { mo ->
            if (PyvmomiManagedObjectGenerator.MIGRATION_EXCLUDE.contains(mo.name)) {
                skippedExcluded++
                return
            }
            File f = new File(moDir, "${mo.name}.java")
            if (!f.exists()) {
                skippedMissing++
                return
            }
            String existing = f.text
            if (existing.contains(MARKER) && existing.contains(CUSTOM_BEGIN) && existing.contains(CUSTOM_END)) {
                skippedAlready++
                return
            }
            migrateOne(f)
            migrated++
        }
        log.info("pyvmomi_migrate: ${migrated} migrated, ${skippedAlready} already migrated, ${skippedExcluded} excluded, ${skippedMissing} missing")
    }

    private void migrateOne(File file) {
        CompilationUnit cu
        try { cu = StaticJavaParser.parse(file) }
        catch (Exception e) { log.warn("migrate: cannot parse ${file.name}: ${e.message}"); return }
        ClassOrInterfaceDeclaration cls = cu.getType(0) as ClassOrInterfaceDeclaration

        // Capture non-standard imports before clearing them — they'll be relocated to the
        // BEGIN custom imports fence so referenced types still resolve after regen.
        // Note: JavaParser's ImportDeclaration.nameAsString omits the trailing ".*" for
        // wildcard imports, so we reconstruct the full form for comparison.
        StringBuilder customImports = new StringBuilder()
        cu.imports.findAll {
            String full = it.nameAsString + (it.asterisk ? ".*" : "")
            !STANDARD_IMPORTS.contains(full)
        }.each {
            customImports << it.toString()
        }
        // Replace all imports with the standard set so the migrated file is compilable
        // even in the intermediate state (before the regenerator runs and rewrites them).
        cu.imports.clear()
        cu.addImport("com.vmware.vim25", false, true)  // wildcard
        cu.addImport("java.rmi.RemoteException")
        cu.addImport("java.util.Calendar")

        // Bucket members
        List<BodyDeclaration<?>> autoGen = []
        List<BodyDeclaration<?>> custom = []
        ConstructorDeclaration ctor = null
        cls.members.each { m ->
            if (m instanceof ConstructorDeclaration) {
                ctor = m as ConstructorDeclaration
            } else if (m instanceof MethodDeclaration && classifier.isAutoGeneratable(m as MethodDeclaration)) {
                autoGen << m
            } else {
                custom << m
            }
        }
        // Rebuild class body: ctor → auto-gen → BEGIN custom fence + relocated members + END custom fence
        cls.members.clear()
        if (ctor != null) cls.addMember(ctor)
        autoGen.each { cls.addMember(it.clone()) }

        // Build the fence + relocated content as raw text. Using JavaParser's comment API for fences is awkward;
        // instead we serialize the class without fences, then post-process the text.
        String customBlock = ""
        if (!custom.isEmpty()) {
            StringBuilder sb = new StringBuilder()
            custom.each { sb << "    " << it.toString() << "\n" }
            customBlock = sb.toString()
        }

        // Render compilation unit
        String classText = cu.toString()
        // Inject fences inside class body (just before final }) — locate the last } of the class
        int classEnd = classText.lastIndexOf("}")
        String beforeClose = classText.substring(0, classEnd)
        String afterClose  = classText.substring(classEnd)
        String withFence = beforeClose +
            "    ${CUSTOM_BEGIN}\n" +
            customBlock +
            "    ${CUSTOM_END}\n" +
            afterClose

        // Inject custom-imports fence directly after the package statement.
        // Locate the package declaration line so we don't get fooled by ';' in a
        // license comment block above it (Steve-Jin yavijava files have one).
        java.util.regex.Matcher pkgMatcher = (withFence =~ /(?m)^\s*package\s+[\w.]+\s*;/)
        if (!pkgMatcher.find()) {
            log.warn("migrate: no package declaration found in ${file.name}; skipping fence insertion")
            return
        }
        int pkgEnd = pkgMatcher.end() - 1  // position of the ';'
        int firstNl = withFence.indexOf("\n", pkgEnd)
        if (firstNl < 0) firstNl = pkgEnd
        String head = withFence.substring(0, firstNl + 1)
        String tail = withFence.substring(firstNl + 1)
        String withImportsFence = head + "\n" +
            "${CUSTOM_IMPORTS_BEGIN}\n" +
            customImports.toString() +
            "${CUSTOM_IMPORTS_END}\n\n" +
            tail

        // Prepend marker
        String final_ = MARKER + "\n" + withImportsFence

        file.text = final_
    }
}
