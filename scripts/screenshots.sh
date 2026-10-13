#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
# The sample compares bundled demo pictures (procedural landscapes drawn by scripts/demo-photos.py) with filters it
# applies itself. Drags and pinches can't be timed reliably through adb, so the sample opens each screen from the
# `scene` extra with the divider and zoom already set, and logs SCENE_READY once the screen has settled. A scene that
# logs SCENE_FAILED or never becomes ready fails the run. Every capture is also checked for the expected text and for
# a blank image.
#
#   bash scripts/screenshots.sh tablet   # pixel_tablet in landscape: the gallery of six comparisons
#   bash scripts/screenshots.sh phone    # pixel_7: side by side, stacked and zoomed comparisons
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

device="${1:-phone}"

# The text each scene must show; the capture fails without it.
expected_text() {
  case "$1" in
    horizontal) echo "Vivid" ;;
    vertical) echo "Mono" ;;
    zoomed) echo "Zoom 2.5x" ;;
    tablet) echo "Gallery" ;;
  esac
}

install_sample
set_night_mode light
if [ "$device" = tablet ]; then
  ensure_landscape
  fresh_launch --es scene tablet
  wait_for_scene tablet
  capture "tablet-gallery" "$(expected_text tablet)"
else
  for scene in horizontal vertical zoomed; do
    fresh_launch --es scene "$scene"
    wait_for_scene "$scene"
    capture "phone-$scene" "$(expected_text "$scene")"
  done
fi
