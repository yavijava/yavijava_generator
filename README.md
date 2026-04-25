[![Build Status](https://travis-ci.org/yavijava/yavijava_generator.svg?branch=master)](https://travis-ci.org/yavijava/yavijava_generator)
[![Join the chat at https://gitter.im/yavijava/yavijava](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/yavijava/yavijava?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# yavijava_generator

A code generator written in Groovy using [jsoup](http://jsoup.org/) to 
parse the HTML documentation provided by VMWare that is used to generate 
code for [yavjava](http://yavijava.com).

## Why
Much of the code in yavijava is a wrapper around a pretty well documented 
SOAP API. I got sick of making the DataObject classes because it was a major 
exercise in copy and paste with getters and setters. Very boring!

## How
Using jsoup we parse the HTML file to find the embedded WSDL snippet for 
a given DataObject. Next using the XMLSlurper in Groovy we parse the WSDL 
and generate a Java class with the info found.

## Tests
Tests can be found in the src/test package. If you submit a pull request 
your pull request should have a test where applicable or the pull request 
will not be merged. Please make sure to run the tests before you submit a 
pull request to make sure your change did not break the tests.

To run the tests execute:

    ./gradlew test

## Current Status
Beta. Tested with vSphere 6.0 through vSphere 9. The HTML-scraping modes (`dataobj`, `fault`, `enum`) require VMware's per-type HTML documentation pages; vSphere 9 no longer ships that format. Use the WSDL-first modes (`wsdl_do`, `wsdl_enum`) for vSphere 9 and later.

Managed object generation is supported as of vSphere 9 via pyVmomi: a Python extractor produces a JSON schema snapshot (`esx/pyvmomi-schema-<version>.json`) which the Groovy generator consumes to emit single-class managed objects in the existing yavijava style. SOAP dispatch generation (`wsdl_vimstub`) and data-object/enum generation (`wsdl_do`, `wsdl_enum`) are unchanged.

## Usage

### HTML-scraping modes (vSphere ≤ 8)

    ./gradelw fatJar
    java -jar build/libs/yavijava_generator-1.0.jar --dest /Users/errr/temp/ --source /Users/errr/programs/java/yavijava.github.io/docs/new-do-types-landing.html --type dataobj --all

This would build a jar containing all deps needed to run the app.

    --dest is the output directory where generated code will be placed
    --source is the path to the dataobjects file
    --type is the type of file to generate. Valid values are one of dataobj, fault, enum, wsdl_do, wsdl_enum, wsdl_vimstub, pyvmomi_audit, pyvmomi_migrate, pyvmomi_mo
    --all sets a flag to generate all data objects found on the source html page. That means new and existing with new properties

### WSDL-first modes (vSphere 9+)

Two new `--type` values read directly from `vim.wsdl` instead of HTML:

- `wsdl_do` — generates data objects and ArrayOf wrappers from the WSDL
- `wsdl_enum` — generates enums from the WSDL

Build the fat jar first:

    ./gradlew fatJar

Then run against a local copy of `vim.wsdl`:

    java -jar build/libs/yavijava_generator-1.0.jar --source /path/to/esx/vim.wsdl --dest /tmp/gen/ --type wsdl_do --all
    java -jar build/libs/yavijava_generator-1.0.jar --source /path/to/esx/vim.wsdl --dest /tmp/gen/ --type wsdl_enum --all

Or via Gradle directly:

    ./gradlew run --args="--source /path/to/esx/vim.wsdl --dest /tmp/gen/ --type wsdl_do --all"

The `--source` argument must point at `vim.wsdl`. All XSD files referenced by `vim.wsdl` (transitively) must live in the same directory as `vim.wsdl`.

### Generating VimStub (vSphere 9+)

`VimStub.java` is the SOAP dispatch table — every managed object method calls into it via `getVimService()`. The generator emits one method per WSDL operation.

    java -jar build/libs/yavijava_generator-1.0.jar \
        --type wsdl_vimstub --source esx/vim.wsdl --dest /tmp/gen/

This is a full overwrite each run. The first time you run it against an existing yavijava tree, the writer will skip `VimStub.java` because the hand-written file lacks the generator marker — run `pyvmomi_migrate` first to insert the marker (see "Migrating an existing yavijava tree" below).

### Generating Managed Objects (vSphere 9+)

Managed objects are generated from a pyVmomi-derived JSON schema snapshot. The schema lives at `esx/pyvmomi-schema-<version>.json` and is checked into the repo; the Groovy generator never talks to a live vSphere.

To regenerate the schema (only when VMware ships a new vSphere release):

    pip install --upgrade pyvmomi
    python3 tools/extract_pyvmomi_schema.py > esx/pyvmomi-schema-9.0.0.json

To generate MO Java files from a snapshot:

    java -jar build/libs/yavijava_generator-1.1-all.jar \
        --type pyvmomi_mo --dest /path/to/yavijava/src/main/java/com/vmware/vim25/mo/ \
        --schema esx/pyvmomi-schema-9.0.0.json

The generator emits one `<MO>.java` per type, single-class style. Each file contains a `// auto generated using yavijava_generator` marker plus two preserved-content fences for hand-written customizations:

    /* ===== BEGIN custom imports (preserved by regenerator) ===== */
    /* ===== END custom imports ===== */

and

    /* ===== BEGIN custom (preserved by regenerator) ===== */
    public boolean isPoweredOn() {
        return getRuntime().getPowerState() == VirtualMachinePowerState.poweredOn;
    }
    /* ===== END custom ===== */

Bytes between the fences are preserved across regenerations. Code outside the fences is generator-owned.

The four files yavijava treats as hand-written infrastructure (`ManagedObject`, `ManagedEntity`, `ExtensibleManagedObject`, `View`) are skipped by the generator regardless of marker.

## Obtaining the WSDL files

The WSDL and its referenced XSDs are available from any ESXi host's `/sdk` endpoint. You need these files:

    vim.wsdl                  vim-types.xsd
    vimService.wsdl           reflect-types.xsd
    core-types.xsd            reflect-messagetypes.xsd
    query-types.xsd           query-messagetypes.xsd
    vim-messagetypes.xsd

Fetch them from a live ESXi host (self-signed cert — use `--insecure`):

    curl --insecure -o vim.wsdl https://<esxi-host>/sdk/vim.wsdl
    curl --insecure -o vim-types.xsd https://<esxi-host>/sdk/vim-types.xsd
    # repeat for each file above

They can also be found:
- In the VMware vSphere Management SDK download (from developer.vmware.com)
- On an ESXi host's filesystem at `/usr/lib/vmware/hostd/docRoot/sdk/`

## Marker mechanism — safe regeneration

Every generated file contains the comment:

    // auto generated using yavijava_generator

When the generator writes a file, `WriteJavaClass` reads any existing file at the destination and checks for this marker:

- **Marker present** — overwrites the file. Safe to regenerate as the WSDL evolves.
- **Marker absent** — skips the file with a WARN log. Protects hand-written files like `DynamicData.java` and `ManagedObjectReference.java`.

This means the generator can run directly against the yavijava source tree without a skip-list — hand-written files are protected automatically by their lack of the marker.

## Migrating an existing yavijava tree

The 133 existing hand-written MO files in yavijava predate the regeneration pipeline. Bringing them under regeneration is a three-command sequence:

1. **Audit** — read-only report of which MOs contain custom (non-auto-generatable) members:

       java -jar build/libs/yavijava_generator-1.1-all.jar \
           --type pyvmomi_audit \
           --schema esx/pyvmomi-schema-9.0.0.json \
           --yavijava-src /path/to/yavijava/src/main/java

2. **Migrate** — for each schema-listed MO with an existing file: parse, leave auto-generatable members in place, relocate any custom members into a `BEGIN custom (preserved by regenerator)` fence at the bottom, prepend the generator marker:

       java -jar build/libs/yavijava_generator-1.1-all.jar \
           --type pyvmomi_migrate \
           --schema esx/pyvmomi-schema-9.0.0.json \
           --yavijava-src /path/to/yavijava/src/main/java

   Idempotent — already-migrated files (with marker + fence) are skipped.

3. **Regenerate** — from this point forward, every MO is regeneratable:

       java -jar build/libs/yavijava_generator-1.1-all.jar \
           --type pyvmomi_mo \
           --schema esx/pyvmomi-schema-9.0.0.json \
           --dest /path/to/yavijava/src/main/java/com/vmware/vim25/mo/

The migrate step is the only moment the generator deliberately bypasses the `WriteJavaClass` marker check (the file gains the marker in the same write).

## Regeneration workflow for new vSphere releases

    # 1. Fetch updated WSDL+XSDs from a target ESXi host
    curl --insecure -o esx/vim.wsdl https://<esxi>/sdk/vim.wsdl
    # (repeat for each XSD — see the file list above)

    # 2. Generate to a scratch directory
    rm -rf /tmp/yavijava-gen && mkdir /tmp/yavijava-gen
    ./gradlew run --args="--source esx/vim.wsdl --dest /tmp/yavijava-gen/ --type wsdl_do --all"
    ./gradlew run --args="--source esx/vim.wsdl --dest /tmp/yavijava-gen/ --type wsdl_enum --all"

    # 3. Generate VimStub
    ./gradlew run --args="--source esx/vim.wsdl --dest /tmp/yavijava-gen/ --type wsdl_vimstub --all"

    # 4. Regenerate managed objects from the pyVmomi snapshot
    ./gradlew run --args="--type pyvmomi_mo --dest /tmp/yavijava-gen/mo/ --schema esx/pyvmomi-schema-9.0.0.json"

    # 5. Diff against current yavijava sources to preview changes
    diff -rq /tmp/yavijava-gen/ /path/to/yavijava/src/main/java/com/vmware/vim25/

    # 6. Copy generated files into yavijava
    cp /tmp/yavijava-gen/*.java /path/to/yavijava/src/main/java/com/vmware/vim25/

    # 7. Review the orphan list printed by the generator (deprecated types)
    #    and manually delete any types that are no longer in the WSDL.

    # 8. Build yavijava and run its tests
    cd /path/to/yavijava && ./gradlew build

Note: existing yavijava files predating the marker (around 3,200 of the older Steve-Jin-style files) will be skipped on first regeneration. Either copy them over with `cp` (which overwrites unconditionally) or add the marker line to existing files if you want them auto-regenerable going forward.

## Architecture note

The WSDL-first path (`wsdl_do`, `wsdl_enum`) reads `vim.wsdl` directly, recursively resolves `<include>` and `<import>` schema references, and emits one Java class per named complexType or simpleType. This replaces the older HTML-scraping flow which only worked when VMware published per-type HTML documentation pages with embedded WSDL textareas — a format vSphere 9 no longer ships.

## License
This application is released under the terms of the Apache 2.0 license. 
A license file is included.

## Build System
This application uses Gradle for the build system. You do not need to 
install Gradle to use it. All you need is a JDK. I include the Gradle 
Wrapper so everything is included by downloading the repo. To build 
the source code on Linux or Mac OSX:

    ./gradlew fatJar

On Windows please use the ```gradlew.bat``` file.

## Bugs
Report them on the GitHub issue tracker.
