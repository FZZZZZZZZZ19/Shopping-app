#!/usr/bin/env bash
set -euo pipefail
# Use a disposable emulator: this smoke test clears Shopping APP's local data.
adb_bin="${ANDROID_HOME:?Set ANDROID_HOME}/platform-tools/adb"
"$adb_bin" wait-for-device
"$adb_bin" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb_bin" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$adb_bin" shell pm clear com.shopping.app
result_file="$(mktemp)"
trap 'rm -f "$result_file"' EXIT
"$adb_bin" shell am instrument -w com.shopping.app.test/com.shopping.app.SmokeInstrumentation | tee "$result_file"
rg -q 'PASS: 14 device checks' "$result_file"
