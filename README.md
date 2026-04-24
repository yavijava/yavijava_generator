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

Managed object generation (`dtm_mo`) and SOAP dispatch generation (`wsdl_vimstub`) are supported as of vSphere 9. MOs require live DynamicTypeManager introspection on first run; snapshots can be cached for offline reruns and CI.

## Usage

### HTML-scraping modes (vSphere ≤ 8)

    ./gradelw fatJar
    java -jar build/libs/yavijava_generator-1.0.jar --dest /Users/errr/temp/ --source /Users/errr/programs/java/yavijava.github.io/docs/new-do-types-landing.html --type dataobj --all

This would build a jar containing all deps needed to run the app.

    --dest is the output directory where generated code will be placed
    --source is the path to the dataobjects file
    --type is the type of file to generate. Valid values are one of dataobj, fault, enum, wsdl_do, wsdl_enum, wsdl_vimstub, dtm_mo, migrate_mo
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

This is a full overwrite each run. The first time you run it against an existing yavijava tree, the writer will skip `VimStub.java` because the hand-written file lacks the generator marker — run `migrate_mo` first to insert the marker (see "Migrating an existing yavijava tree" below).

### Generating Managed Objects (vSphere 9+)

The `dtm_mo` mode introspects MO schemas (properties, methods, inheritance) via vSphere's internal `DynamicTypeManager` SOAP API. Output uses a **two-class split**: each MO becomes `<MO>Base.java` (auto-generated, fully overwritten each run) plus `<MO>.java` (hand-written subclass, never touched by the generator after first creation).

Live ESXi (writes a snapshot for next time):

    export YAVIJAVA_ESX_PASS=...
    java -jar build/libs/yavijava_generator-1.0.jar \
        --type dtm_mo --dest /tmp/gen/ \
        --esx-url https://10.0.0.5/sdk --esx-user root \
        --dtm-snapshot esx/dtm-snapshot-9.0.0.json

Offline reuse (no ESXi needed):

    java -jar build/libs/yavijava_generator-1.0.jar \
        --type dtm_mo --dest /tmp/gen/ \
        --dtm-snapshot esx/dtm-snapshot-9.0.0.json

Flag reference:

| Flag | Default | Purpose |
|------|---------|---------|
| `--esx-url` | (none) | ESXi SDK URL, e.g., `https://10.0.0.5/sdk` |
| `--esx-user` | `root` | ESXi username |
| `--esx-pass` | `$YAVIJAVA_ESX_PASS` | ESXi password (env-fallback to keep it out of shell history) |
| `--esx-strict-cert` | off | Verify the server cert; default off matches a fresh-install ESXi |
| `--dtm-snapshot` | (none) | JSON snapshot path. Read-only when no `--esx-url`; written when both are present |

The generator emits `<MO>.java` subclass stubs only when the file doesn't already exist. If you need to add custom helpers (e.g., the `getRecentTasks()` rename pattern in `ManagedEntity`), put them in the subclass — it's never touched by the generator after first creation. Java method dispatch ensures subclass methods override Base methods.

Snapshots are deterministic and check-in friendly. Committing `dtm-snapshot-<version>.json` to your repo means CI can regenerate without ESXi access.

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

## Obtaining DTM access

Any standalone ESXi exposes the internal `DynamicTypeManager` (DTM) SOAP API at `https://<esxi>/sdk` with default root credentials. The generator skips certificate verification by default — pass `--esx-strict-cert` only when targeting a properly-signed vCenter.

DTM is an internal/non-public VMware API but stable across the vSphere versions yavijava targets. PBM/SPBM coverage is deferred until vCenter access and `pbm.wsdl` are available — see `docs/superpowers/2026-04-23-managed-object-generation-design.md` for context.

## Marker mechanism — safe regeneration

Every generated file contains the comment:

    // auto generated using yavijava_generator

When the generator writes a file, `WriteJavaClass` reads any existing file at the destination and checks for this marker:

- **Marker present** — overwrites the file. Safe to regenerate as the WSDL evolves.
- **Marker absent** — skips the file with a WARN log. Protects hand-written files like `DynamicData.java` and `ManagedObjectReference.java`.

This means the generator can run directly against the yavijava source tree without a skip-list — hand-written files are protected automatically by their lack of the marker.

## Migrating an existing yavijava tree

The 133 hand-written MO files in yavijava predate the two-class split and lack the generator marker. The `migrate_mo` mode is a one-time refactor:

    java -jar build/libs/yavijava_generator-1.0.jar \
        --type migrate_mo --yavijava-src /path/to/yavijava/src/main/java

For each `com/vmware/vim25/mo/<MO>.java`:

- Writes `<MO>.java.bak` as a backup.
- Splits the class into `<MO>Base.java` (generated, marker inserted, contains all auto-generatable members) and `<MO>.java` (rewritten, `extends <MO>Base`, contains constructor + any custom members the tool couldn't classify as boilerplate).
- Compiles the result with `javac` to verify it builds; if compilation fails, restores the original file from `.bak` and aborts with the error.

Also rewrites `com/vmware/vim25/ws/VimStub.java` to insert the generator marker directly after the `package` statement — this unlocks `wsdl_vimstub` to overwrite the file on subsequent runs.

The migration is idempotent: re-running detects already-migrated files (those that `extends <ClassName>Base`) and skips them. Backup files (`.bak`) are left in place for manual review; clean them up when satisfied:

    find /path/to/yavijava/src/main/java -name '*.java.bak' -delete

Files left untouched (yavijava-side infrastructure with custom logic the generator doesn't describe):
`ManagedObject.java`, `ManagedEntity.java`, `ExtensibleManagedObject.java`, `View.java`.

After migration, run `wsdl_vimstub` and `dtm_mo` against the same vSphere version snapshot. The result should produce only "ordering churn" — the migration preserves source ordering of methods and throws clauses, while the generators emit them in deterministic alphabetical order. This expected one-time diff is best committed separately so the migration commit and the reorder commit each review cleanly.

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

    # 4. Capture a fresh DTM snapshot from the ESXi host (or skip if reusing an existing snapshot)
    export YAVIJAVA_ESX_PASS=...
    ./gradlew run --args="--type dtm_mo --dest /tmp/yavijava-gen/mo/ \
        --esx-url https://<esxi>/sdk --esx-user root \
        --dtm-snapshot esx/dtm-snapshot-9.0.0.json"

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
