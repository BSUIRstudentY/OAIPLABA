#pragma once
/*
 * Compatibility shim for building the (originally MSVC/Visual Studio) project
 * with the MinGW-w64 cross compiler on a case-sensitive (Linux) filesystem.
 *
 * The source uses `#include <Windows.h>` (MSVC is case-insensitive), but the
 * MinGW-w64 SDK ships the header as lowercase `windows.h`. This shim simply
 * forwards to the real header so the original source stays untouched.
 */
#include <windows.h>
