#!/usr/bin/env bash
# Sunucuyu baslatir. Ornek: ./run.sh --port 8080
# Internete acmadan once README "Guvenlik" bolumunu okuyun (varsayilan: yalniz 127.0.0.1).
set -euo pipefail
cd "$(dirname "$0")"
[ -f build/frcsim-core.jar ] || bash build.sh
exec java -cp build/frcsim-core.jar:build/server-classes frcsim.server.Server --root . "$@"
