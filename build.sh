#!/usr/bin/env bash
# build.sh — compile all sources and package constructivist_source.jar
# Usage:
#   ./build.sh          — compile + package
#   ./build.sh run      — compile, package, then launch the interactive CLI

set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
BIN="$ROOT/bin"
JAR="$ROOT/constructivist_source.jar"

mkdir -p "$BIN"

echo "[build] Compiling sources..."
find "$ROOT/java" -name "*.java" -print0 | xargs -0 javac -d "$BIN"

echo "[build] Packaging $JAR..."
jar cfe "$JAR" danexcodr.ai.Main -C "$BIN" .

echo "[build] Done — $JAR updated."

if [ "${1:-}" = "run" ]; then
  echo "[build] Launching CLI..."
  java -jar "$JAR"
fi
