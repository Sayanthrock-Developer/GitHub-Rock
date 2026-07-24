#!/usr/bin/env bash
set -euo pipefail

certificate_output="$(cat)"
certificate_sha256="$(
  printf '%s\n' "$certificate_output" |
    sed -n -E \
      -e 's/^Signer #[0-9]+ certificate SHA-256 digest:[[:space:]]*//p' \
      -e 's/^V[0-9.]+ Signer: certificate SHA-256 digest:[[:space:]]*//p' |
    head -n 1
)"

normalized="$(
  printf '%s' "$certificate_sha256" |
    tr -d '[:space:]:' |
    tr '[:upper:]' '[:lower:]'
)"

if [[ ! "$normalized" =~ ^[0-9a-f]{64}$ ]]; then
  echo "Unable to read a valid signer certificate SHA-256 fingerprint." >&2
  exit 1
fi

printf '%s\n' "$normalized"
