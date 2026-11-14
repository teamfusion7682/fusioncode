#!/usr/bin/env bash
# Fusion Code - tek komutla baslat / one-command launcher (Mac & Linux)
cd "$(dirname "$0")" || exit 1
PORT="${1:-8080}"

if ! command -v javac >/dev/null 2>&1; then
  echo
  echo " [TR] Java (JDK) bulunamadi. JDK 21 kurun: https://adoptium.net (Temurin 21)"
  echo " [EN] Java (JDK) not found. Install JDK 21 from https://adoptium.net, then run again."
  echo
  exit 1
fi
[ -f build/frcsim-core.jar ] || bash build.sh || exit 1

URL="http://127.0.0.1:$PORT"
echo
echo " Fusion Code basliyor / starting: $URL"
echo " Durdurmak icin Ctrl+C / press Ctrl+C to stop."
echo
( sleep 3; (command -v open >/dev/null && open "$URL") || (command -v xdg-open >/dev/null && xdg-open "$URL") || true ) >/dev/null 2>&1 &
exec java -cp build/frcsim-core.jar:build/server-classes frcsim.server.Server --root . --port "$PORT"
