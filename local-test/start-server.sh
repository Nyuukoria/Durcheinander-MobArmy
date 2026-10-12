#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/server"
exec java -Xms1G -Xmx3G -jar server.jar nogui
