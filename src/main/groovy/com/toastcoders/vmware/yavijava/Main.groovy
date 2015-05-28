package com.toastcoders.vmware.yavijava

import com.toastcoders.vmware.yavijava.contracts.Generator
import com.toastcoders.vmware.yavijava.generator.DataObjectGeneratorImpl
import com.toastcoders.vmware.yavijava.generator.EnumGeneratorImpl
import com.toastcoders.vmware.yavijava.generator.SPBMDataObjectGeneratorImpl

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
class Main {

    public static void main(String[] args) {

        def cli = new CliBuilder(usage: 'yavijava_generator')
        cli.h(longOpt: 'help', 'usage information', required: false)
        cli._(longOpt: 'source', 'Source to read from', required: true, args: 1)
        cli._(longOpt: 'dest', 'Destination path for where to write new files', required: true, args: 1)
        cli._(longOpt: 'type',
            'Type of objects to create. Valid values are one of either: dataobj, fault, enum, spbm_do',
            required: true, args: 1
        )
        cli.a(longOpt: 'all', 'Generate new and changed. Default is new only')
        def opt = cli.parse(args)

        if(!opt) {
            return
        }

        if (opt?.h) {
            cli.usage()
            System.exit(1)
        }

        boolean all = false
        if (opt?.a) {
            all = true
        }

        List valid = ["dataobj", "fault", "enum", "spbm_do"]
        if (!(opt.type in valid)) {
            println "Invalid type detected. ${opt.type} not supported."
            cli.usage()
            System.exit(1)
        }
        // all options verified. time to spawn a generator
        // to make some classes for us.
        String source = opt.source
        String dest = opt.dest
        switch (opt.type) {
            case "dataobj":
                Generator dataObjectGenerator = new DataObjectGeneratorImpl(source, dest)
                dataObjectGenerator.generate(all)
                break
            case "fault":
                Generator dataObjectGenerator = new DataObjectGeneratorImpl(source, dest)
                dataObjectGenerator.generate(all)
                break
            case "spbm_do":
                Generator spbmDoGenerator = new SPBMDataObjectGeneratorImpl(source, dest)
                spbmDoGenerator.generate(all)
                break
            default:
                Generator enumGenerator = new EnumGeneratorImpl(source, dest)
                enumGenerator.generate(all)
        }
    }
}
