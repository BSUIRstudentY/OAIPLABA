#!/usr/bin/env bash
#
# Linux/CI build helper for the OAIPLABA "BioGarden" console app.
#
# The project is a Windows / Visual Studio (MSVC, toolset v143) C++ console
# application. The canonical build is `OAIPLABA.sln` opened in Visual Studio on
# Windows. This script lets you build the exact same, UNMODIFIED sources on a
# Linux machine by cross compiling with MinGW-w64. The produced Windows
# executable can then be run with Wine.
#
# Requirements (install once):
#   sudo apt-get install -y mingw-w64 wine
#
# Usage:
#   ./build-linux/build.sh            # builds ./OAIPLABA.exe in the repo root
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPAT_DIR="${ROOT_DIR}/build-linux/compat"
CXX="${CXX:-x86_64-w64-mingw32-g++}"
OUT="${ROOT_DIR}/OAIPLABA.exe"

echo "Cross compiling OAIPLABA.exe with ${CXX}..."
"${CXX}" \
    -std=c++17 \
    -D__STDC_WANT_LIB_EXT1__=1 \
    -I"${COMPAT_DIR}" \
    -include "${COMPAT_DIR}/msvc_compat.h" \
    -static -static-libgcc -static-libstdc++ \
    "${ROOT_DIR}/Source.cpp" \
    "${ROOT_DIR}/Functions.cpp" \
    -o "${OUT}"

echo "Built ${OUT}"
