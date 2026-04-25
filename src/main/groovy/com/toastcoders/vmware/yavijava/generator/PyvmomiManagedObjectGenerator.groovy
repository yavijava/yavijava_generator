package com.toastcoders.vmware.yavijava.generator

import com.toastcoders.vmware.yavijava.contracts.Generator
import com.toastcoders.vmware.yavijava.data.ManagedObjectSingleClassTemplate
import com.toastcoders.vmware.yavijava.pyvmomi.PyvmomiManagedObject
import com.toastcoders.vmware.yavijava.pyvmomi.PyvmomiSchema
import com.toastcoders.vmware.yavijava.writer.WriteJavaClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PyvmomiManagedObjectGenerator implements Generator {

    private static final Logger log = LoggerFactory.getLogger(PyvmomiManagedObjectGenerator)

    /** MOs that are yavijava-side infrastructure — never generated. */
    static final Set<String> MIGRATION_EXCLUDE = [
        "ManagedObject", "ManagedEntity", "ExtensibleManagedObject", "View"
    ] as Set

    final String schemaPath
    final String dest

    PyvmomiManagedObjectGenerator(String schemaPath, String dest) {
        this.schemaPath = schemaPath
        this.dest = dest
    }

    @Override void generate() { generate(true, "com.vmware.vim25.mo", [:]) }
    @Override void generate(boolean all) { generate(all, "com.vmware.vim25.mo", [:]) }

    @Override
    void generate(boolean all, String packageName, Map nameSpace) {
        Map<String, PyvmomiManagedObject> mos = new PyvmomiSchema().read(new File(schemaPath))
        int wrote = 0, skipped = 0, excluded = 0
        mos.values().sort { it.name }.each { mo ->
            if (MIGRATION_EXCLUDE.contains(mo.name)) {
                excluded++
                return
            }
            String content = ManagedObjectSingleClassTemplate.render(mo)
            String path = dest + mo.name + ".java"
            if (WriteJavaClass.writeFileWithFence(path, content)) wrote++
            else skipped++
        }
        log.info("pyvmomi_mo: ${wrote} written, ${skipped} skipped (no marker), ${excluded} excluded (infrastructure)")
    }
}
