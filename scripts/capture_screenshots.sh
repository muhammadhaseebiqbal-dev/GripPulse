#!/usr/bin/env bash
set -euo pipefail

APP_ID="com.aistudio.grippulse.vkmqtr"
OUT_DIR="docs/screenshots"

mkdir -p "$OUT_DIR"

if ! command -v adb >/dev/null 2>&1; then
  echo "Error: adb not found in PATH. Install Android Platform Tools first." >&2
  exit 1
fi

if ! adb get-state >/dev/null 2>&1; then
  echo "Error: No Android device or emulator connected via ADB." >&2
  exit 1
fi

echo "Launching GripPulse ($APP_ID)..."
adb shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true

capture_screen() {
  local filename="$1"
  local prompt="$2"
  echo ""
  read -r -p "$prompt — press ENTER to capture $OUT_DIR/$filename..."
  adb exec-out screencap -p > "$OUT_DIR/$filename"
  echo "Saved $OUT_DIR/$filename"
}

capture_screen "dashboard.png" "1/4 Open the 'Today' Dashboard tab"
capture_screen "grips.png"     "2/4 Open the 'Grips' tab"
capture_screen "widgets.png"   "3/4 Open the 'Widgets' tab"
capture_screen "calibrate.png" "4/4 Open the 'Calibrate' tab"

echo ""
echo "All real in-app screenshots captured in $OUT_DIR/."
echo "README.md is already wired to display them."
