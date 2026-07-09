# AGENTS.md

## Cursor Cloud specific instructions

### What this project is
`OAIPLABA` ("BioGarden") is a small **Windows / Visual Studio (MSVC, toolset v143)**
C++ console application — a student lab that manages a catalog of plants
(`Plants`: name, amount, type, plot number) via a text menu, persisting data to
`BioGarden.txt`. All console I/O and source string literals are **CP-1251
(Cyrillic)** encoded (the app calls `SetConsoleCP/SetConsoleOutputCP(1251)`).
There is no database, no package manager, and no network services.

The canonical build is `OAIPLABA.sln` opened in Visual Studio on Windows. There
is **no automated test suite and no configured linter**; the compiler build (with
its warnings) is the closest available check.

### Repository history note
`master` currently has an **empty working tree** — the latest commit
(`d991826` "Clear repository contents.") removed every file. The actual sources
live in commit `7bd2d4d`. If you land on an empty tree and need the project,
restore it with `git checkout 7bd2d4d -- .`.

### Building & running on this Linux VM
The project is Windows-only, so on Linux it is cross-compiled with **MinGW-w64**
and run under **Wine**. Helper scripts live in `build-linux/` and the original
source files are left completely unchanged — the two MSVC incompatibilities
(`#include <Windows.h>` casing and the single-argument `gets_s` overload) are
handled entirely by force-included compat shims in `build-linux/compat/`, wired
in only through compiler flags.

- Build: `./build-linux/build.sh` → produces `./OAIPLABA.exe` (git-ignored).
- Run: `./build-linux/run.sh` (sets `WINEPREFIX=$HOME/.wine-oaip`, `WINEDEBUG=-all`).

### Non-obvious gotchas when running the app
- **CP-1251 I/O.** Encode stdin and decode stdout, otherwise Cyrillic is garbled:
  ```
  printf '1\n3\n' | iconv -f utf8 -t cp1251 \
      | ./build-linux/run.sh 2>/dev/null | iconv -f cp1251 -t utf8
  ```
- **The app never exits on its own.** `Menu()` only returns choices 1-7 (the
  "0 = exit" branch is unreachable). On stdin EOF it busy-loops reprinting the
  menu. Bound runs with `timeout` and keep stdin open so it blocks waiting for
  input instead of spinning, e.g. `{ cat input.cp1251; sleep 20; } | timeout 18 wine ...`.
- **Do not pipe input through `iconv` while holding stdin open.** `iconv`
  buffers its output and will not deliver bytes to the app until its own stdin
  closes, so the app appears to hang. Pre-encode input to a file and `cat` it
  into the app instead (then a trailing `sleep` keeps the pipe open).
- **stdout is line-buffered when redirected**, and `timeout` kills the process
  without a final flush — any partial (newline-less) prompt like `Ввод: ` may be
  missing from captured output. This is expected.
- Menu options exercised for a smoke test: `1` read file, `3` list plants,
  `5` add N plants, `2` write file.
