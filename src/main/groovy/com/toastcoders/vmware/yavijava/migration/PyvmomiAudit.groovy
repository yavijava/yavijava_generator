package com.toastcoders.vmware.yavijava.migration

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.toastcoders.vmware.yavijava.generator.PyvmomiManagedObjectGenerator
import com.toastcoders.vmware.yavijava.pyvmomi.PyvmomiManagedObject
import com.toastcoders.vmware.yavijava.pyvmomi.PyvmomiSchema
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PyvmomiAudit {

    private static final Logger log = LoggerFactory.getLogger(PyvmomiAudit)
    private final MoMemberClassifier classifier = new MoMemberClassifier()

    static class MoAudit {
        String name
        int autoGenMemberCount
        List<String> customMemberNames = []
    }

    static class AuditReport {
        List<MoAudit> perMo = []
        int totalAutoGenOnly = 0
        int totalWithCustom = 0
    }

    AuditReport run(String schemaPath, String yavijavaSrcRoot) {
        Map<String, PyvmomiManagedObject> mos = new PyvmomiSchema().read(new File(schemaPath))
        AuditReport report = new AuditReport()
        File moDir = new File(yavijavaSrcRoot, "com/vmware/vim25/mo")
        if (!moDir.isDirectory()) {
            log.warn("Audit root has no com/vmware/vim25/mo: ${yavijavaSrcRoot}")
            return report
        }
        mos.values().sort { it.name }.each { mo ->
            if (PyvmomiManagedObjectGenerator.MIGRATION_EXCLUDE.contains(mo.name)) return
            File f = new File(moDir, "${mo.name}.java")
            if (!f.exists()) return
            MoAudit a = auditOne(f, mo.name)
            if (a == null) return
            report.perMo << a
            if (a.customMemberNames.isEmpty()) report.totalAutoGenOnly++
            else                                 report.totalWithCustom++
        }
        printReport(report)
        return report
    }

    private MoAudit auditOne(File f, String typeName) {
        def cu
        try { cu = StaticJavaParser.parse(f) }
        catch (Exception e) { log.warn("audit: cannot parse ${f.name}: ${e.message}"); return null }
        def cls = cu.getType(0) as ClassOrInterfaceDeclaration
        MoAudit a = new MoAudit(name: typeName)
        cls.members.each { m ->
            if (m instanceof ConstructorDeclaration) return  // ctor never custom
            if (m instanceof MethodDeclaration && classifier.isAutoGeneratable(m as MethodDeclaration)) {
                a.autoGenMemberCount++
            } else if (m instanceof MethodDeclaration) {
                a.customMemberNames << (m as MethodDeclaration).nameAsString
            }
        }
        return a
    }

    private void printReport(AuditReport report) {
        println "=== Migration audit ==="
        println "${report.totalAutoGenOnly} MOs are 100% auto-generatable — safe to regenerate."
        if (report.totalWithCustom == 0) {
            println "No custom members detected. Migration is straightforward."
            return
        }
        println "${report.totalWithCustom} MOs contain custom members that will be relocated to the custom fence on migrate:"
        report.perMo.findAll { !it.customMemberNames.isEmpty() }.each {
            println "  ${it.name}.java"
            it.customMemberNames.each { name -> println "    - ${name}" }
        }
    }
}
