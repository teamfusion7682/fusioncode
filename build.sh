#!/usr/bin/env bash
# Cekirdek kutuphane + sunucuyu derler. Gereksinim: JDK 17+ (javac, jar).
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_TOOL_OPTIONS=""

rm -rf build/core-classes build/server-classes
mkdir -p build/core-classes build/server-classes build/work

find core/src -name '*.java' > build/core-sources.txt
javac --release 17 -Xlint:all -d build/core-classes @build/core-sources.txt
jar cf build/frcsim-core.jar -C build/core-classes .

find server/src -name '*.java' > build/server-sources.txt
javac --release 17 -Xlint:all -cp build/frcsim-core.jar -d build/server-classes @build/server-sources.txt

echo "Derleme tamam: build/frcsim-core.jar, build/server-classes"
