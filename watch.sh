#!/usr/bin/env bash
# watch.sh — automatically rebuild constructivist_source.jar whenever a
#            .java source file under java/ is created, modified, or deleted.
#
# Requirements:
#   inotifywait  (part of inotify-tools on Linux)
#   Install:  sudo apt-get install inotify-tools   (Debian/Ubuntu)
#             sudo dnf install inotify-tools        (Fedora/RHEL)
#
# Usage:
#   ./watch.sh          — watch and auto-rebuild
#   Ctrl-C to stop.

set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
JAVA_DIR="$ROOT/java"

if ! command -v inotifywait &>/dev/null; then
  echo "Error: inotifywait not found."
  echo "  Install inotify-tools first (see the comment at the top of this script)."
  exit 1
fi

echo "[watch] Watching $JAVA_DIR for changes..."
echo "[watch] Press Ctrl-C to stop."

# Run an initial build so the jar is up-to-date before watching starts.
"$ROOT/build.sh"

while inotifywait -r -e modify,create,delete \
      --include '\.java$' --quiet "$JAVA_DIR"; do
  echo ""
  echo "[watch] Change detected — rebuilding..."
  "$ROOT/build.sh" || echo "[watch] Build failed — fix the error and save again."
done
