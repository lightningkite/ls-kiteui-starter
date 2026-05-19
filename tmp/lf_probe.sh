#!/usr/bin/env bash
# Probe Life File sandbox for undocumented GET endpoints.
# Sources lifefile-sandbox.env but NEVER prints credential values.

ENV_FILE="$(dirname "$0")/../lifefile-sandbox.env"
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

for v in FullAPIBaseURL APIUsername APIPassword VendorID LocationID APINetworkID; do
  if [ -z "${!v:-}" ]; then
    echo "MISSING: $v" >&2; exit 1
  fi
done

BASE="${FullAPIBaseURL%/}"
BODY="$(dirname "$0")/lf_body.tmp"

echo "Base host: $(echo "$BASE" | sed -E 's#^(https?://[^/]+).*#\1#')"
echo "Base path: $(echo "$BASE" | sed -E 's#^https?://[^/]+##')"
echo

probe() {
  local method="$1"
  local path="$2"
  local url="${BASE}${path}"
  local code
  code=$(curl -sS -m 15 -o "$BODY" -w '%{http_code}' \
    -u "${APIUsername}:${APIPassword}" \
    -H "X-Vendor-ID: ${VendorID}" \
    -H "X-Location-ID: ${LocationID}" \
    -H "X-API-Network-ID: ${APINetworkID}" \
    -H "Accept: application/json" \
    -X "$method" "$url")
  local size
  size=$(wc -c < "$BODY" | tr -d ' ')
  printf '%-6s %-40s -> %s  (%s bytes)\n' "$method" "$path" "$code" "$size"
  # Show body for anything not 404/401 with non-empty short body.
  if [ "$code" != "404" ] && [ "$code" != "401" ] && [ "$size" -gt 0 ] && [ "$size" -lt 6000 ]; then
    echo "----- body -----"
    cat "$BODY"
    echo
    echo "----------------"
  fi
}

echo "=== Probing read endpoints ==="
# Discovery
probe GET /
probe GET /openapi.json
probe GET /openapi
probe GET /swagger.json
probe GET /swagger.yaml
probe GET /swagger
probe GET /docs
probe GET /api-docs
probe GET /spec

# Catalog candidates
probe GET /product
probe GET /products
probe GET /catalog
probe GET /catalogs
probe GET /drug
probe GET /drugs
probe GET /formulary
probe GET /lfproduct
probe GET /lf-product
probe GET /pharmacy/products
probe GET /pharmacy/catalog

# Order GET candidates
probe GET /order
probe GET /orders
probe GET /order/24200716
probe GET /order/1

# Shipping services list
probe GET /shipping
probe GET /shipping/services
probe GET /shipping-service
probe GET /service
probe GET /services

# Statuses list
probe GET /order/status
probe GET /order-status
probe GET /status
probe GET /statuses

# Practice/vendor/location/network
probe GET /practice
probe GET "/practice/${PracticeID}"
probe GET /vendor
probe GET /location
probe GET /network

rm -f "$BODY"
