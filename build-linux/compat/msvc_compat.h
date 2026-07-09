#pragma once
/*
 * Compatibility shim for building the (originally MSVC/Visual Studio) project
 * with the MinGW-w64 cross compiler.
 *
 * MSVC's <stdio.h> provides a C++ template overload of gets_s() that deduces
 * the destination buffer size from a char array, allowing the single argument
 * form `gets_s(buffer)` used throughout the source. MinGW-w64 only declares the
 * two argument C function `gets_s(char*, rsize_t)`, and that symbol is not even
 * present in the msvcrt import library MinGW links against, so it fails at link
 * time. We therefore provide a self contained fgets() based implementation
 * (matching gets_s semantics: read one line, strip the trailing newline) and a
 * matching single argument template overload for char arrays.
 *
 * This header is force-included via the compiler command line only; the
 * original source files are left completely unchanged.
 */
#include <cstddef>
#include <cstdio>
#include <cstring>

inline char* mingw_compat_gets_s(char* buffer, std::size_t size) {
    if (buffer == nullptr || size == 0) {
        return nullptr;
    }
    if (std::fgets(buffer, static_cast<int>(size), stdin) == nullptr) {
        buffer[0] = '\0';
        return nullptr;
    }
    std::size_t len = std::strlen(buffer);
    if (len > 0 && buffer[len - 1] == '\n') {
        buffer[len - 1] = '\0';
    }
    return buffer;
}

template <std::size_t N>
inline char* gets_s(char (&buffer)[N]) {
    return mingw_compat_gets_s(buffer, N);
}
