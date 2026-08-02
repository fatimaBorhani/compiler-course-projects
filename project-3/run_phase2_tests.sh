#!/usr/bin/env bash
#
# فاز ۲ را کامپایل می‌کند و روی همه‌ی نمونه‌های پوشه‌ی samples/ (زیرپوشه‌های
# Name و Type) اجرا می‌کند و خروجی را با فایل .out متناظر مقایسه می‌کند.
#
# اجرا از ریشه‌ی PLC:
#   bash run_phase2_tests.sh
#
set -u
ROOT="$(cd "$(dirname "$0")" && pwd)"
ANTLR="$ROOT/utilities/antlr-4.13.1-complete.jar"
OUT="$ROOT/out"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo ">> Compiling ..."
rm -rf "$OUT"; mkdir -p "$OUT"
if ! javac -cp "$ANTLR" -d "$OUT" $(find "$ROOT/src" "$ROOT/gen" -name "*.java"); then
    echo "!! Compilation failed."; exit 1
fi
echo ">> Compiled OK."; echo

pass=0; fail=0

# expected output file for a given .mol (handles the notDeclaredMethod..out typo)
expected_of() {
    local base="$1"
    if [ -f "$base.out" ]; then echo "$base.out";
    elif [ -f "$base..out" ]; then echo "$base..out";
    else echo ""; fi
}

for mol in "$ROOT"/samples/Name/*.mol "$ROOT"/samples/Type/*.mol; do
    name="$(basename "$mol")"
    case "$name" in *_optimized.mol) continue;; esac   # not a test input
    base="${mol%.mol}"
    exp="$(expected_of "$base")"
    [ -z "$exp" ] && continue

    cp "$mol" "$TMP/$name"
    actual="$(java -cp "$OUT:$ANTLR" SimpleLang "$TMP/$name" 2>&1)"
    expected="$(cat "$exp")"

    if [ "$actual" == "$expected" ]; then
        echo "PASS  $name"
        pass=$((pass+1))
    else
        echo "FAIL  $name"
        echo "   --- expected ---"; echo "$expected" | sed 's/^/     /'
        echo "   --- actual   ---"; echo "$actual"   | sed 's/^/     /'
        fail=$((fail+1))
    fi

    # bonus: check optimized source for the unreachable case
    if [ "$name" = "unreachable.mol" ] && [ -f "$base"_optimized.mol ]; then
        gen="$TMP/${name%.mol}_optimized.mol"
        if [ -f "$gen" ] && diff -q "$gen" "$base"_optimized.mol >/dev/null; then
            echo "PASS  unreachable_optimized.mol (generated file matches)"
        else
            echo "WARN  unreachable_optimized.mol mismatch or not generated"
        fi
    fi
done

echo; echo ">> $pass passed, $fail failed."
