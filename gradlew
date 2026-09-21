#!/bin/sh
set -e
DIST="$HOME/.gradle/wrapper/dists/gradle-8.9-bin.zip"
DIR="$HOME/.gradle/wrapper/dists/gradle-8.9-bin"
if [ ! -x "$DIR/gradle-8.9/bin/gradle" ]; then
  mkdir -p "$HOME/.gradle/wrapper/dists"
  curl -L --fail https://services.gradle.org/distributions/gradle-8.9-bin.zip -o "$DIST"
  rm -rf "$DIR"
  mkdir -p "$DIR"
  unzip -q "$DIST" -d "$DIR"
fi
exec "$DIR/gradle-8.9/bin/gradle" "$@"
