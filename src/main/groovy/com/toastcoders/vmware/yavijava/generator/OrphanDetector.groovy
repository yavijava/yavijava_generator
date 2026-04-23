package com.toastcoders.vmware.yavijava.generator

import com.toastcoders.vmware.yavijava.writer.WriteJavaClass

class OrphanDetector {

    List<File> findOrphans(File destDir, Set<String> generatedFileNames) {
        if (!destDir.isDirectory()) return []
        List<File> orphans = []
        destDir.listFiles({ File f -> f.name.endsWith('.java') } as FileFilter).each { f ->
            if (generatedFileNames.contains(f.name)) return
            if (f.text.contains(WriteJavaClass.GENERATOR_MARKER)) {
                orphans << f
            }
        }
        return orphans
    }
}
