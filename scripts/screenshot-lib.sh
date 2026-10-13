#!/usr/bin/env bash
# Shared helpers for scripts/screenshots.sh. Expects PKG to be set by the caller's environment
# or falls back to the sample's application id from sample/build.gradle.kts.
PKG="${PKG:-$(grep -oE 'applicationId = "[^"]+"' sample/build.gradle.kts | cut -d'"' -f2)}"
OUT="docs/screenshots"
mkdir -p "$OUT"

install_sample() {
  adb install -r sample/build/outputs/apk/debug/sample-debug.apk
  # A freshly booted emulator often shows "Pixel Launcher isn't responding".
  # Hide error/ANR dialogs and give the system time to settle.
  adb shell settings put global hide_error_dialogs 1
  adb shell settings put global anr_show_background 0
  sleep 20
  dismiss_system_dialogs
  # Clean status bar via System UI demo mode.
  adb shell settings put global sysui_demo_allowed 1
  adb shell am broadcast -a com.android.systemui.demo -e command enter
  adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
  adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
  adb shell am broadcast -a com.android.systemui.demo -e command network -e fully true -e wifi show -e level 4 -e mobile hide
  adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
}

set_night_mode() {
  if [ "$1" = dark ]; then adb shell cmd uimode night yes; else adb shell cmd uimode night no; fi
  sleep 2
}

# Scenes log "SCENE_READY <scene>" under this tag once they are laid out and settled (the zoomed scene after
# its zoom), or "SCENE_FAILED <scene>: <reason>" when something went wrong.
SCENE_TAG="${SCENE_TAG:-ImageCompareScene}"

# The emulator runner unlocks the screen with a key press, which leaves the device out of touch mode, so
# Compose would draw keyboard focus highlights. A tap on the empty home screen puts it back in touch mode.
enter_touch_mode() {
  local size w h
  adb shell input keyevent KEYCODE_HOME
  sleep 1
  size="$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | tr -d '\r')"
  w="${size%x*}"
  h="${size#*x}"
  adb shell input tap "$((w / 2))" "$((h * 2 / 5))"
  sleep 1
}

fresh_launch() {
  adb shell pm clear "$PKG" > /dev/null
  enter_touch_mode
  adb logcat -c || true
  adb shell am start -W -n "$PKG/.MainActivity" "$@" > /dev/null
}

# Waits until the scene reports that it is complete. Fails on an error state or after a timeout, so a
# screen that did not load never replaces a good screenshot.
wait_for_scene() {
  local scene="$1" timeout="${2:-150}" log=""
  for _ in $(seq 1 "$timeout"); do
    log="$(adb logcat -d -s "$SCENE_TAG:*" 2> /dev/null || true)"
    if printf '%s' "$log" | grep -q "SCENE_FAILED"; then
      echo "The $scene scene failed to load:" >&2
      printf '%s\n' "$log" | grep "SCENE_" >&2
      exit 1
    fi
    if printf '%s' "$log" | grep -q "SCENE_READY $scene"; then
      echo "Scene $scene is ready"
      sleep 1
      return 0
    fi
    sleep 1
  done
  echo "The $scene scene did not finish loading within ${timeout}s." >&2
  adb logcat -d -t 200 >&2 || true
  exit 1
}

dismiss_system_dialogs() {
  for _ in 1 2 3; do
    if adb shell dumpsys window | grep -qiE "Application Not Responding|isn't responding"; then
      adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null
      adb shell input keyevent KEYCODE_ENTER
      sleep 2
    else
      return 0
    fi
  done
  if adb shell dumpsys window | grep -qiE "Application Not Responding|isn't responding"; then
    echo "A system dialog is still on screen; refusing to capture a broken screenshot." >&2
    exit 1
  fi
}

# Fails when the sample is not in front or the expected text is missing from the screen.
# A failing UI dump (it happens on busy emulators) only warns; the image checks below still run.
expect_on_screen() {
  local text="$1"
  local resumed
  resumed="$(adb shell dumpsys activity activities | grep -E "ResumedActivity" || true)"
  if [ -n "$resumed" ] && ! printf '%s' "$resumed" | grep -q "$PKG"; then
    echo "The sample app is not in front; refusing to capture." >&2
    exit 1
  fi
  local xml=""
  for _ in 1 2 3; do
    if adb shell uiautomator dump /sdcard/window.xml > /dev/null 2>&1; then
      xml="$(adb exec-out cat /sdcard/window.xml)"
      break
    fi
    sleep 2
  done
  if [ -z "$xml" ]; then
    echo "Warning: could not dump the UI to look for \"$text\"." >&2
  elif ! printf '%s' "$xml" | grep -qF "$text"; then
    echo "\"$text\" is not on screen; the scene did not appear." >&2
    exit 1
  fi
}

# Fails on a missing, tiny, blank or single colored PNG.
check_png() {
  python3 - "$1" <<'PY'
import struct, sys, zlib

path = sys.argv[1]
with open(path, "rb") as f:
    data = f.read()
if len(data) < 12_000 or not data.startswith(b"\x89PNG\r\n\x1a\n"):
    sys.exit(f"{path}: not a PNG or too small ({len(data)} bytes)")
width, height = struct.unpack(">II", data[16:24])
idat, pos = b"", 8
while pos < len(data):
    length, kind = struct.unpack(">I4s", data[pos:pos + 8])
    if kind == b"IDAT":
        idat += data[pos + 8:pos + 8 + length]
    pos += 12 + length
raw = zlib.decompress(idat)
# Filtered scanlines of a blank screen are almost all zeros; real content has many different bytes.
distinct = len(set(raw[::97]))
nonzero = sum(1 for b in raw[::211] if b) / max(1, len(raw[::211]))
# A light screen with a sparse chart filters to mostly zeros too, so a low non-zero share only
# counts as blank when the content also has few distinct bytes.
if width < 300 or height < 300 or distinct < 24 or (nonzero < 0.01 and distinct < 120):
    sys.exit(f"{path}: looks blank ({width}x{height}, {distinct} distinct samples, {nonzero:.3f} non-zero)")
print(f"{path}: {width}x{height}, {len(data) // 1024} KB, {distinct} distinct samples")
PY
}

capture() {
  local name="$1" expected="$2"
  dismiss_system_dialogs
  expect_on_screen "$expected"
  adb exec-out screencap -p > "$OUT/$name.png"
  if ! check_png "$OUT/$name.png"; then
    rm -f "$OUT/$name.png"
    exit 1
  fi
  echo "Captured $name"
}

# Locks the display in landscape (tablet shots). The Pixel Tablet's natural orientation is landscape, but some
# system images boot in portrait, so try both rotations and check the current display size.
ensure_landscape() {
  adb shell settings put system accelerometer_rotation 0
  local rotation size w h
  for rotation in 0 1; do
    adb shell settings put system user_rotation "$rotation"
    sleep 3
    size="$(adb shell dumpsys window displays | grep -oE 'cur=[0-9]+x[0-9]+' | head -1 | cut -d= -f2 | tr -d '\r' || true)"
    if [ -z "$size" ]; then
      echo "Warning: could not read the display size; keeping rotation $rotation." >&2
      return 0
    fi
    w="${size%x*}"
    h="${size#*x}"
    if [ "$w" -gt "$h" ]; then
      echo "Display is landscape ($size, rotation $rotation)"
      return 0
    fi
  done
  echo "Could not put the display in landscape ($size)." >&2
  exit 1
}
