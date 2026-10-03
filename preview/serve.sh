#!/usr/bin/env bash

cd "$(dirname "$0")"

echo
echo "DriveChronik Dash Preview"
echo "========================="
echo

IP=$(hostname -I | awk '{print $1}')

echo "Mac öffnen:"
echo
echo "http://${IP}:4173"
echo

python3 -m http.server 4173 --bind 0.0.0.0
