#!/usr/bin/env bash
#
# Convenience wrapper to run the cross compiled Windows executable on Linux
# through Wine. Build first with ./build-linux/build.sh
#
# The program's console I/O is CP-1251 (Cyrillic) encoded, matching the
# original Windows build (SetConsoleCP/SetConsoleOutputCP(1251)). To interact
# with it from a UTF-8 terminal, encode your input to cp1251 and decode the
# output from cp1251, e.g.:
#
#   printf '1\n3\n' | iconv -f utf8 -t cp1251 \
#       | ./build-linux/run.sh 2>/dev/null | iconv -f cp1251 -t utf8
#
# NOTE: The app's menu loop only returns choices 1-7; the "0 = exit" branch is
# unreachable, so the program never terminates on its own. When stdin reaches
# EOF it busy-loops reprinting the menu. Bound runs with `timeout` and keep
# stdin open (e.g. a trailing `sleep`) so it blocks waiting for input instead.
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export WINEPREFIX="${WINEPREFIX:-$HOME/.wine-oaip}"
export WINEDEBUG="${WINEDEBUG:--all}"

exec wine "${ROOT_DIR}/OAIPLABA.exe" "$@"
