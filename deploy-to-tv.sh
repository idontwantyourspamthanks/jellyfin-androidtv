#!/usr/bin/env bash
#
# Build the debug APK and push it to the Android TV over ADB (network).
#
# Usage:
#   ./deploy-to-tv.sh            # build + install to the default TV
#   ./deploy-to-tv.sh 1.2.3.4    # ... to a different IP
#   SKIP_BUILD=1 ./deploy-to-tv.sh   # install the last-built APK without rebuilding
#
set -euo pipefail

TV_IP="${1:-192.168.68.57}"
TV_PORT="5555"
TV_ADDR="${TV_IP}:${TV_PORT}"
PACKAGE_ID="paw.koala.androidtv"

cd "$(dirname "$0")"

if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
	echo "==> Building debug APK..."
	./gradlew :app:assembleDebug
fi

# assembleDebug names the APK with the version, so glob for whatever it produced.
APK="$(ls -t app/build/outputs/apk/debug/*-debug.apk 2>/dev/null | head -n 1 || true)"
if [[ -z "${APK}" ]]; then
	echo "No debug APK found in app/build/outputs/apk/debug/ — build first (don't set SKIP_BUILD)." >&2
	exit 1
fi
echo "==> APK: ${APK}"

echo "==> Connecting to ${TV_ADDR}..."
adb connect "${TV_ADDR}"

echo "==> Installing (reinstall, keep data, allow downgrade)..."
# -r: reinstall keeping data; -d: allow version-code downgrade (debug builds are pinned low).
adb -s "${TV_ADDR}" install -r -d "${APK}"

echo "==> Done. ${PACKAGE_ID} is on ${TV_IP}."
