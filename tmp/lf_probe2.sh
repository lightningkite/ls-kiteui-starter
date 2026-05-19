#!/usr/bin/env bash
ENV_FILE="$(dirname "$0")/../lifefile-sandbox.env"
set -a; source "$ENV_FILE"; set +a
BASE="${FullAPIBaseURL%/}"
BODY="$(dirname "$0")/lf_body.tmp"

probe() {
  local method="$1" path="$2"
  local code
  code=$(curl -sS -m 15 -o "$BODY" -w '%{http_code}' \
    -u "${APIUsername}:${APIPassword}" \
    -H "X-Vendor-ID: ${VendorID}" \
    -H "X-Location-ID: ${LocationID}" \
    -H "X-API-Network-ID: ${APINetworkID}" \
    -H "Accept: application/json" \
    -X "$method" "${BASE}${path}")
  local size; size=$(wc -c < "$BODY" | tr -d ' ')
  printf '%-6s %-45s -> %s  (%s bytes)\n' "$method" "$path" "$code" "$size"
  if [ "$code" != "404" ] && [ "$code" != "401" ] && [ "$size" -gt 0 ] && [ "$size" -lt 6000 ]; then
    echo "----- body -----"; cat "$BODY"; echo; echo "----------------"
  fi
}

echo "=== Probe more endpoints / methods ==="
# Confirm the documented routes exist (will return 405 if path is wrong method)
probe GET    /order/1/status
probe GET    /order/1/shipping
probe GET    /order/link
probe POST   /order/link
probe GET    /order/24200716/status
probe GET    /order/24200716/shipping
# Verify 405 on POST link sub-resource (already documented)
probe PUT    /order/1/status
probe PUT    /order/1/shipping

# Speculative
probe GET    /order/list
probe GET    /order/search
probe POST   /order/search
probe POST   /order/list
probe GET    /master-order
probe GET    /pharmacy
probe GET    /pharmacy/1
probe GET    /me
probe GET    /user
probe GET    /account
probe GET    /ping
probe GET    /health
probe GET    /version
probe GET    /v1
probe GET    /apitest
probe GET    /apitest/pharmacy

# Try root-level absolute paths in case routes aren't under /lfapi/v1
HOST="$(echo "$BASE" | sed -E 's#^(https?://[^/]+).*#\1#')"
for p in /lfapi /lfapi/v1/openapi /lfapi/openapi /api /api/v1 /api/v1/product /api/v1/order /api/v1/catalog; do
  code=$(curl -sS -m 10 -o "$BODY" -w '%{http_code}' \
    -u "${APIUsername}:${APIPassword}" -X GET "${HOST}${p}")
  size=$(wc -c < "$BODY" | tr -d ' ')
  printf '%-6s %-45s -> %s  (%s bytes)\n' GET "$p" "$code" "$size"
  if [ "$code" != "404" ] && [ "$code" != "401" ] && [ "$size" -gt 0 ] && [ "$size" -lt 6000 ]; then
    echo "----- body -----"; cat "$BODY"; echo; echo "----------------"
  fi
done

rm -f "$BODY"
