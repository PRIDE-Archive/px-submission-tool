455#!/usr/bin/env bash
# Quick ascp CLI diagnostic for px-submission-tool (no GUI).
# Usage:
#   ./scripts/test-ascp.sh
#
# Live upload test (PRIDE dropbox credentials from submission):
#   export PX_ASCP_LIVE_TEST=1
#   export PX_ASPERA_HOST=hx-fasp-1.ebi.ac.uk
#   export PX_ASPERA_USER=your_user
#   export PX_ASPERA_PASS=your_password
#   export PX_ASPERA_FOLDER=your_remote_folder
#   ./scripts/test-ascp.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

OS="$(uname -s)"
ARCH="$(uname -m)"
case "$OS" in
  Darwin) ASCP="$ROOT/aspera/bin/mac-intel/ascp" ;;
  Linux)
    if [[ "$ARCH" == "x86_64" || "$ARCH" == "amd64" ]]; then
      ASCP="$ROOT/aspera/bin/linux-64/ascp"
    else
      ASCP="$ROOT/aspera/bin/linux-32/ascp"
    fi
    ;;
  MINGW*|MSYS*|CYGWIN*|Windows*)
    ASCP="$ROOT/aspera/bin/windows-64/ascp.exe"
    ;;
  *)
    echo "Unsupported OS: $OS"
    exit 1
    ;;
esac

echo "=== test-ascp.sh ==="
echo "Root: $ROOT"
echo "OS: $OS ($ARCH)"
echo "ascp: $ASCP"

if [[ ! -f "$ASCP" ]]; then
  echo "FAIL: ascp not found. Copy IBM Aspera Connect binary to:"
  echo "  $ASCP"
  exit 1
fi

if [[ ! -x "$ASCP" ]]; then
  echo "Making ascp executable..."
  chmod +x "$ASCP"
fi

echo ""
echo "--- ascp version ---"
"$ASCP" -A || { echo "FAIL: ascp -A"; exit 2; }

SAMPLE="$(mktemp /tmp/px-ascp-test-XXXXXX.txt)"
echo "px-ascp test $(date)" > "$SAMPLE"
echo "Sample file: $SAMPLE"

echo ""
echo "--- example command (dry run) ---"
echo "ASPERA_SCP_PASS='***' \"$ASCP\" -v -P 33001 -O 33001 -l 950M -m 1M --policy=fair --overwrite=diff -k 3 -p -d -W PRIDE-Aspera-1-Token \\"
echo "  \"$SAMPLE\" USER@HOST:/REMOTE_FOLDER/"

if [[ "${PX_ASCP_LIVE_TEST:-}" != "1" && "${PX_ASCP_LIVE_TEST:-}" != "true" ]]; then
  echo ""
  echo "SKIP live upload. Set PX_ASCP_LIVE_TEST=1 and PX_ASPERA_HOST/USER/PASS/FOLDER to test upload."
  echo "PASS: binary checks OK"
  rm -f "$SAMPLE"
  exit 0
fi

for var in PX_ASPERA_HOST PX_ASPERA_USER PX_ASPERA_PASS PX_ASPERA_FOLDER; do
  if [[ -z "${!var:-}" ]]; then
    echo "FAIL: $var is not set"
    exit 3
  fi
done

REMOTE="${ }@${PX_ASPERA_HOST}:/${PX_ASPERA_FOLDER}/"
echo ""
echo "--- live upload to $REMOTE ---"
export ASPERA_SCP_PASS="$PX_ASPERA_PASS"
set +e
"$ASCP" -v -P 33001 -O 33001 -l 950M -m 1M --policy=fair --overwrite=diff -k 3 -p -d \
  -W PRIDE-Aspera-1-Token \
  "$SAMPLE" "$REMOTE"
EXIT=$?
set -e
rm -f "$SAMPLE"

if [[ $EXIT -eq 0 ]]; then
  echo "PASS: upload succeeded"
else
  echo "FAIL: ascp exited with $EXIT"
fi
exit "$EXIT"
