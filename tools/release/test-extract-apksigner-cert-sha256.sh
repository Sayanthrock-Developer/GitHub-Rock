#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
extractor="$script_dir/extract-apksigner-cert-sha256.sh"
expected="6b6a6a12d0ea137261cc4f09f21f5f37e1d408ef2d28d41bb8e5c67b5bf66d95"

modern="$(
  printf '%s\n' \
    "Signer #1 certificate SHA-256 digest: 6B:6A:6A:12:D0:EA:13:72:61:CC:4F:09:F2:1F:5F:37:E1:D4:08:EF:2D:28:D4:1B:B8:E5:C6:7B:5B:F6:6D:95" |
    bash "$extractor"
)"
legacy="$(
  printf '%s\n' \
    "V2 Signer: certificate SHA-256 digest: $expected" |
    bash "$extractor"
)"

test "$modern" = "$expected"
test "$legacy" = "$expected"

if printf '%s\n' "certificate unavailable" | bash "$extractor" >/dev/null 2>&1; then
  echo "The certificate parser accepted invalid apksigner output." >&2
  exit 1
fi

echo "apksigner certificate parser tests passed."
