package com.toastcoders.vmware.yavijava

import groovy.cli.picocli.CliBuilder
import com.toastcoders.vmware.yavijava.contracts.Generator
import com.toastcoders.vmware.yavijava.generator.DataObjectGeneratorImpl
import com.toastcoders.vmware.yavijava.generator.EnumGeneratorImpl
import com.toastcoders.vmware.yavijava.generator.SPBMDataObjectGeneratorImpl
import com.toastcoders.vmware.yavijava.generator.SPBMEnumGeneratorImpl
import com.toastcoders.vmware.yavijava.generator.WSDLDataObjectGenerator
import com.toastcoders.vmware.yavijava.generator.WSDLEnumGenerator
import com.toastcoders.vmware.yavijava.generator.WSDLVimStubGenerator
import com.toastcoders.vmware.yavijava.generator.DTMManagedObjectGenerator
import com.toastcoders.vmware.yavijava.generator.PyvmomiManagedObjectGenerator
import com.toastcoders.vmware.yavijava.migration.MoSplitMigration
import com.toastcoders.vmware.yavijava.migration.PyvmomiAudit
import com.toastcoders.vmware.yavijava.migration.PyvmomiMigrate

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
        cli._(longOpt: 'source', 'Source to read from (WSDL/HTML — required for non-DTM modes)', required: false, args: 1)
        cli._(longOpt: 'dest', 'Destination path for where to write new files', required: false, args: 1)
        cli._(longOpt: 'type',
            'Type of objects to create. Valid values are one of either: dataobj, fault, enum, spbm_do, spbm_fault, spbm_enum, wsdl_do, wsdl_enum, wsdl_vimstub, dtm_mo, migrate_mo, pyvmomi_audit, pyvmomi_migrate, pyvmomi_mo',
            required: true, args: 1
        )
        cli.a(longOpt: 'all', 'Generate new and changed. Default is new only')
        cli._(longOpt: 'esx-url',         'ESXi SDK URL for dtm_mo (e.g., https://10.0.0.5/sdk)', required: false, args: 1)
        cli._(longOpt: 'esx-user',        'ESXi username (default: root)',                         required: false, args: 1)
        cli._(longOpt: 'esx-pass',        'ESXi password (env fallback: YAVIJAVA_ESX_PASS)',       required: false, args: 1)
        cli._(longOpt: 'esx-strict-cert', 'Verify the ESXi cert (default: false)',                 required: false)
        cli._(longOpt: 'dtm-snapshot',    'JSON snapshot path (read or write per design §4.2)',    required: false, args: 1)
        cli._(longOpt: 'yavijava-src',    'Yavijava source root for migrate_mo',                   required: false, args: 1)
        cli._(longOpt: 'schema', 'pyVmomi schema JSON path (for pyvmomi_audit/migrate/mo)', required: false, args: 1)
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

        List valid = ["dataobj", "fault", "enum", "spbm_do", "spbm_fault", "spbm_enum", "wsdl_do", "wsdl_enum", "wsdl_vimstub", "dtm_mo", "migrate_mo", "pyvmomi_audit", "pyvmomi_migrate", "pyvmomi_mo"]
        if (!(opt.type in valid)) {
            println "Invalid type detected. ${opt.type} not supported."
            cli.usage()
            System.exit(1)
        }
        if (opt.type != "dtm_mo" && opt.type != "migrate_mo" && opt.type != "pyvmomi_audit" && opt.type != "pyvmomi_migrate" && opt.type != "pyvmomi_mo" && !opt?.source) {
            println "--source is required for type ${opt.type}"
            cli.usage()
            System.exit(1)
        }
        if ((opt.type == "wsdl_vimstub" || opt.type == "dtm_mo" || opt.type == "pyvmomi_mo") && !opt?.dest) {
            println "--dest is required for type ${opt.type}"
            cli.usage()
            System.exit(1)
        }
        if ((opt.type == "pyvmomi_audit" || opt.type == "pyvmomi_migrate" || opt.type == "pyvmomi_mo") && !opt?.schema) {
            println "--schema is required for type ${opt.type}"
            cli.usage()
            System.exit(1)
        }
        if ((opt.type == "pyvmomi_audit" || opt.type == "pyvmomi_migrate" || opt.type == "migrate_mo") && !opt?.'yavijava-src') {
            println "--yavijava-src is required for type ${opt.type}"
            cli.usage()
            System.exit(1)
        }
        String source = opt.source
        String dest = opt.dest
        switch (opt.type) {
            case "dataobj":
                Generator dataObjectGenerator = new DataObjectGeneratorImpl(source, dest)
                dataObjectGenerator.generate(all, "com.vmware.vim25", [vim25: 'xmlns:vim25="urn:vim25"'])
                break
            case "fault":
                Generator dataObjectGenerator = new DataObjectGeneratorImpl(source, dest)
                dataObjectGenerator.generate(all, "com.vmware.vim25", [vim25: 'xmlns:vim25="urn:vim25"'])
                break
            case "spbm_do":
                Generator spbmDoGenerator = new SPBMDataObjectGeneratorImpl(source, dest)
                spbmDoGenerator.generate(all, "com.vmware.spbm", ['pbm': 'xmlns:pbm="urn:pbm"'])
                break
            case "spbm_fault":
                Generator spbmDoGenerator = new SPBMDataObjectGeneratorImpl(source, dest)
                spbmDoGenerator.generate(all, "com.vmware.spbm", ['pbm': 'xmlns:pbm="urn:pbm"'])
                break
            case "spbm_enum":
                Generator spbmEnumGenerator = new SPBMEnumGeneratorImpl(source, dest)
                spbmEnumGenerator.generate(all, "com.vmware.spbm", ['pbm': 'xmlns:pbm="urn:pbm"'])
                break
            case "wsdl_do":
                Generator wsdlDoGenerator = new WSDLDataObjectGenerator(source, dest)
                wsdlDoGenerator.generate(all, "com.vmware.vim25", [vim25: 'urn:vim25'])
                break
            case "wsdl_enum":
                Generator wsdlEnumGenerator = new WSDLEnumGenerator(source, dest)
                wsdlEnumGenerator.generate(all, "com.vmware.vim25", [vim25: 'urn:vim25'])
                break
            case "wsdl_vimstub":
                Generator wsdlVimStubGen = new WSDLVimStubGenerator(source, dest)
                wsdlVimStubGen.generate(all, "com.vmware.vim25", [vim25: 'urn:vim25'])
                break
            case "migrate_mo":
                String yvSrc = opt?.'yavijava-src'
                if (!yvSrc) {
                    println "migrate_mo requires --yavijava-src"
                    System.exit(1)
                }
                new MoSplitMigration().run(yvSrc)
                break
            case "pyvmomi_audit":
                new PyvmomiAudit().run(opt.schema, opt.'yavijava-src')
                break
            case "pyvmomi_migrate":
                new PyvmomiMigrate().run(opt.schema, opt.'yavijava-src')
                break
            case "pyvmomi_mo":
                Generator pyvmomiGen = new PyvmomiManagedObjectGenerator(opt.schema, dest)
                pyvmomiGen.generate(all, "com.vmware.vim25.mo", [:])
                break
            case "dtm_mo":
                String snap = opt?.'dtm-snapshot' ?: null
                String esxUrl = opt?.'esx-url' ?: null
                String esxUser = opt?.'esx-user' ?: "root"
                String esxPass = opt?.'esx-pass' ?: System.getenv("YAVIJAVA_ESX_PASS")
                boolean strict = opt?.'esx-strict-cert' ? true : false
                if (esxUrl == null && snap == null) {
                    println "dtm_mo requires either --esx-url or --dtm-snapshot"
                    System.exit(1)
                }
                if (esxUrl != null && esxPass == null) {
                    println "dtm_mo with --esx-url requires --esx-pass or YAVIJAVA_ESX_PASS env var"
                    System.exit(1)
                }
                Generator dtmMoGen = new DTMManagedObjectGenerator(snap, dest, esxUrl, esxUser, esxPass, strict)
                dtmMoGen.generate(all, "com.vmware.vim25.mo", [:])
                break
            default:
                // enums for vim25 yavijava
                Generator enumGenerator = new EnumGeneratorImpl(source, dest)
                enumGenerator.generate(all, "com.vmware.vim25", [vim25: 'xmlns:vim25="urn:vim25"'])
        }
    }
}
