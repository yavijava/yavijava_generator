package com.toastcoders.vmware.yavijava.generator

import com.toastcoders.vmware.yavijava.contracts.Generator
import com.toastcoders.vmware.yavijava.data.ManagedObjectBaseTemplate
import com.toastcoders.vmware.yavijava.data.ManagedObjectSubclassStubTemplate
import com.toastcoders.vmware.yavijava.dtm.DtmSnapshot
import com.toastcoders.vmware.yavijava.dtm.DynamicTypeManagerClient
import com.toastcoders.vmware.yavijava.dtm.MoTypeInfo
import com.toastcoders.vmware.yavijava.writer.WriteJavaClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DTMManagedObjectGenerator implements Generator {

    private static final Logger log = LoggerFactory.getLogger(DTMManagedObjectGenerator)

    static final Set<String> MIGRATION_EXCLUDE = [
        "ManagedObject", "ManagedEntity", "ExtensibleManagedObject", "View"
    ] as Set

    final String snapshotPath
    final String dest
    final String esxUrl
    final String esxUser
    final String esxPass
    final boolean strictCert

    DTMManagedObjectGenerator(String snapshotPath, String dest, String esxUrl, String esxUser, String esxPass, boolean strictCert) {
        this.snapshotPath = snapshotPath
        this.dest = dest
        this.esxUrl = esxUrl
        this.esxUser = esxUser
        this.esxPass = esxPass
        this.strictCert = strictCert
    }

    @Override void generate() { generate(true, "com.vmware.vim25.mo", [:]) }
    @Override void generate(boolean all) { generate(all, "com.vmware.vim25.mo", [:]) }

    @Override
    void generate(boolean all, String packageName, Map nameSpace) {
        Map<String, MoTypeInfo> types = loadTypes()
        int baseCount = 0, stubCount = 0
        List<String> skipped = []

        types.values().sort { it.name }.each { t ->
            if (MIGRATION_EXCLUDE.contains(t.name)) return
            String parentClass = resolveParentClass(t.parent, types)
            String baseContent = ManagedObjectBaseTemplate.render(t, parentClass)
            String basePath = dest + t.name + "Base.java"
            if (WriteJavaClass.writeFile(basePath, baseContent)) baseCount++
            else skipped << "${t.name}Base.java"

            File subclassFile = new File(dest, t.name + ".java")
            if (!subclassFile.exists()) {
                String stub = ManagedObjectSubclassStubTemplate.render(t.name)
                WriteJavaClass.writeFile(subclassFile.absolutePath, stub)
                stubCount++
            }
        }
        printSummary(baseCount, stubCount, skipped)
    }

    private Map<String, MoTypeInfo> loadTypes() {
        File snapshotFile = snapshotPath != null ? new File(snapshotPath) : null
        boolean haveSnap = snapshotFile?.exists()
        boolean haveUrl  = esxUrl != null && !esxUrl.isEmpty()

        if (!haveSnap && !haveUrl) {
            throw new RuntimeException("Either --dtm-snapshot or --esx-url must be provided")
        }
        if (haveUrl) {
            log.info("DTM: querying live ESXi at ${esxUrl}")
            Map<String, MoTypeInfo> fresh = new DynamicTypeManagerClient(
                esxUrl, esxUser, esxPass, strictCert).fetchAll()
            if (snapshotFile != null) {
                new DtmSnapshot().write(snapshotFile, fresh, "live")
                log.info("DTM: snapshot written to ${snapshotFile}")
            }
            return fresh
        }
        log.info("DTM: reading snapshot ${snapshotFile}")
        return new DtmSnapshot().read(snapshotFile)
    }

    private String resolveParentClass(String parent, Map<String, MoTypeInfo> types) {
        if (parent == null || parent.isEmpty()) return "ManagedObject"
        return MIGRATION_EXCLUDE.contains(parent) ? parent : "${parent}Base"
    }

    private void printSummary(int baseCount, int stubCount, List<String> skipped) {
        println "DTM-MO generation complete:"
        println "  Generated: ${baseCount} Base files, ${stubCount} new subclass stubs"
        if (!skipped.isEmpty()) {
            println "  Skipped:   ${skipped.size()} files (no marker — likely hand-edited):"
            skipped.each { println "    - ${it}" }
        }
    }
}
