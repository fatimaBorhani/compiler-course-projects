#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Phase 3 - build the compiler, generate jasmin code for a .mol file,
#           optionally assemble it with jasmin.jar and run it.
#
#   ./run_phase3.sh Sample/sample.mol
#
# Put jasmin.jar next to this script (or in utilities/) to get the
# assemble + run steps as well.
# ---------------------------------------------------------------------------
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

INPUT="${1:-Sample/sample.mol}"
ANTLR="utilities/antlr-4.13.1-complete.jar"
BUILD="build_classes"

echo "==> compiling the compiler"
rm -rf "$BUILD"
mkdir -p "$BUILD"
find src gen -name "*.java" > /tmp/mol_sources.txt
javac -nowarn -cp "$ANTLR" -d "$BUILD" @/tmp/mol_sources.txt

echo "==> generating jasmin code for $INPUT"
rm -rf codeGenOutput
java -cp "$BUILD:$ANTLR" SimpleLang "$INPUT"

echo "==> generated files:"
ls -1 codeGenOutput

JASMIN=""
for candidate in jasmin.jar utilities/jasmin.jar; do
    if [ -f "$candidate" ]; then
        JASMIN="$ROOT/$candidate"
    fi
done

if [ -z "$JASMIN" ]; then
    echo "==> jasmin.jar not found - skipping assemble/run"
    echo "    (drop jasmin.jar in the project root and re-run)"
    exit 0
fi

echo "==> assembling with jasmin"
cd codeGenOutput
java -jar "$JASMIN" *.j

echo "==> running Main"
java Main
