#!/usr/bin/env bash
# Manual end-to-end smoke test for the pyvmomi_mo pipeline.
#
# Prerequisites:
#   - vcsim binary on $PATH or $VCSIM (default: /home/errr/programs/personal/esx/vcsim)
#   - pyvmomi-schema-<version>.json checked in under esx/
#   - A working yavijava checkout to regenerate. Set $YAVIJAVA_SRC.
#
# This is a documented procedure, not an automated test.

set -euo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
VCSIM="${VCSIM:-/home/errr/programs/personal/esx/vcsim}"
SCHEMA="${REPO}/esx/pyvmomi-schema-9.0.0.json"
YAVIJAVA_SRC="${YAVIJAVA_SRC:-/home/errr/programs/personal/yavijava}"
PORT="${VCSIM_PORT:-8989}"

if [[ ! -x "$VCSIM" ]]; then
    echo "vcsim not found at $VCSIM. Set \$VCSIM or place the binary there." >&2
    exit 1
fi
if [[ ! -f "$SCHEMA" ]]; then
    echo "schema not found at $SCHEMA. Run tools/extract_pyvmomi_schema.py first." >&2
    exit 1
fi
if [[ ! -d "$YAVIJAVA_SRC" ]]; then
    echo "yavijava source not found at $YAVIJAVA_SRC. Set \$YAVIJAVA_SRC." >&2
    exit 1
fi

echo "==> Booting vcsim on 127.0.0.1:${PORT}"
"$VCSIM" -l "127.0.0.1:${PORT}" -api-version 9.0.0.0 > /tmp/vcsim.log 2>&1 &
VCSIM_PID=$!
trap "kill ${VCSIM_PID} 2>/dev/null || true; rm -f /tmp/vcsim.log" EXIT
sleep 2

echo "==> Building the generator fat jar"
(cd "$REPO" && ./gradlew --quiet fatJar)
JAR="$(ls "$REPO"/build/libs/yavijava_generator-*-all.jar | tail -1)"

echo "==> Working in a copy of yavijava"
WORK="$(mktemp -d)"
cp -r "$YAVIJAVA_SRC" "$WORK/yavijava"

echo "==> Audit"
java -jar "$JAR" --type pyvmomi_audit --schema "$SCHEMA" --yavijava-src "$WORK/yavijava/src/main/java"

echo "==> Migrate (commit this if real)"
java -jar "$JAR" --type pyvmomi_migrate --schema "$SCHEMA" --yavijava-src "$WORK/yavijava/src/main/java"

echo "==> Regenerate"
java -jar "$JAR" --type pyvmomi_mo --schema "$SCHEMA" --dest "$WORK/yavijava/src/main/java/com/vmware/vim25/mo/"

echo "==> Compile yavijava"
(cd "$WORK/yavijava" && ./gradlew --quiet compileJava)

echo "==> Run the bundled vcsim smoke probe"
# The probe class is committed in the test directory and exercises a small
# surface: connect, list HostSystems, list VMs, attempt PowerOn on the first VM.
# (The probe itself is left for the operator to write or use govc — vcsim's
# default inventory has 1 vCenter, 1 datacenter, 1 cluster, 3 hosts, 2 VMs/pool.)
echo "    (TODO: invoke a Java probe class — see README; for now this script"
echo "     verifies the regen+compile pipeline end-to-end without runtime probe)"

echo "==> All steps passed"
rm -rf "$WORK"
