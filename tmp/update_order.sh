#!/usr/bin/env bash
ENV_FILE="$(dirname "$0")/../lifefile-sandbox.env"
set -a; source "$ENV_FILE"; set +a
BASE="${FullAPIBaseURL%/}"
ORDER_ID="${1:?usage: update_order.sh <orderId>}"
RESP="$(dirname "$0")/upd_resp.json"

call() {
  local method="$1" path="$2" data="$3"
  echo "$method $path"
  echo "  body: $data"
  HTTP=$(curl -sS -m 30 -o "$RESP" -w '%{http_code}' \
    -u "${APIUsername}:${APIPassword}" \
    -H "X-Vendor-ID: ${VendorID}" \
    -H "X-Location-ID: ${LocationID}" \
    -H "X-API-Network-ID: ${APINetworkID}" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -X "$method" --data "$data" "${BASE}${path}")
  echo "  HTTP $HTTP"
  echo "  $(cat "$RESP")"
  echo
}

call PUT "/order/${ORDER_ID}/status"   '{"status":"bc3f4"}'

call PUT "/order/${ORDER_ID}/shipping" '{
  "shipping": {
    "recipientType": "patient",
    "recipientLastName": "Parker",
    "recipientFirstName": "Peter",
    "recipientPhone": "(305) 555-0102",
    "recipientEmail": "peter.parker@example.com",
    "addressLine1": "20 Ingram St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "US",
    "service": 6228
  }
}'
