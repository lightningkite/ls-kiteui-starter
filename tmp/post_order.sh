#!/usr/bin/env bash
ENV_FILE="$(dirname "$0")/../lifefile-sandbox.env"
set -a; source "$ENV_FILE"; set +a
BASE="${FullAPIBaseURL%/}"
BODY_TEMPLATE="$(dirname "$0")/order_attempt.json"
BODY_FILE="$(dirname "$0")/order_attempt.expanded.json"
RESP="$(dirname "$0")/order_resp.json"
# Substitute PracticeID without printing it.
sed "s/__PRACTICE_ID__/${PracticeID}/g" "$BODY_TEMPLATE" > "$BODY_FILE"

echo "POST ${BASE}/order"
echo "Headers: X-Vendor-ID, X-Location-ID=${LocationID}, X-API-Network-ID=${APINetworkID}"
echo
HTTP=$(curl -sS -m 30 -o "$RESP" -w '%{http_code}' \
  -u "${APIUsername}:${APIPassword}" \
  -H "X-Vendor-ID: ${VendorID}" \
  -H "X-Location-ID: ${LocationID}" \
  -H "X-API-Network-ID: ${APINetworkID}" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -X POST --data-binary @"$BODY_FILE" \
  "${BASE}/order")
echo "HTTP $HTTP"
echo "----- response body -----"
cat "$RESP"; echo
echo "-------------------------"
