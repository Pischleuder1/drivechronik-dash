#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

DEST="app/src/main/assets/drivechronik-voice"
URL="https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip"

if [ -f "$DEST/uuid" ] &&
   [ -d "$DEST/am" ] &&
   [ -d "$DEST/graph" ]; then
  echo "Sprachmodell bereits vorhanden."
  exit 0
fi

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

curl -fL "$URL" -o "$TMP/model.zip"
unzip -tq "$TMP/model.zip" >/dev/null
unzip -q "$TMP/model.zip" -d "$TMP"

mkdir -p "$(dirname "$DEST")"
rm -rf "$DEST"
mv "$TMP/vosk-model-small-de-0.15" "$DEST"
echo "drivechronik-de-0.15-v1" > "$DEST/uuid"

echo "OK: Deutsches Sprachmodell installiert"
