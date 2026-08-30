#!/usr/bin/env bash
set -euo pipefail

TLS_DIR=${SHOP_TLS_DIR:-/etc/shop/tls}
DOMAINS=(console.nexbyte.top conapi.nexbyte.top miniapi.nexbyte.top)

if [[ ! -d "$TLS_DIR" ]]; then
  echo "TLS directory does not exist: $TLS_DIR" >&2
  exit 1
fi

for domain in "${DOMAINS[@]}"; do
  cert="$TLS_DIR/$domain.pem"
  key="$TLS_DIR/$domain.key"

  [[ -r "$cert" ]] || { echo "Missing or unreadable certificate: $cert" >&2; exit 1; }
  [[ -r "$key" ]] || { echo "Missing or unreadable private key: $key" >&2; exit 1; }

  cert_pub=$(mktemp)
  key_pub=$(mktemp)
  trap 'rm -f "$cert_pub" "$key_pub"' EXIT
  openssl x509 -in "$cert" -pubkey -noout > "$cert_pub"
  openssl pkey -in "$key" -pubout > "$key_pub"
  cmp -s "$cert_pub" "$key_pub" || {
    echo "Certificate/private key mismatch: $domain" >&2
    exit 1
  }
  rm -f "$cert_pub" "$key_pub"
  trap - EXIT
done

echo "TLS preflight passed: $TLS_DIR"
