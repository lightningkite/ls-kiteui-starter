#!/bin/bash
# Call the test backend as the seeded admin, exchanging testing/.admin-token for an access token first
# Usage: ./testing/api.sh METHOD PATH [JSON_BODY]
#   ./testing/api.sh GET /auth/session/self
#   ./testing/api.sh POST /users/rest/query '{}'

set -euo pipefail
cd "$(dirname "$0")/.."

BASE_URL="http://localhost:8081"
METHOD=$1
API_PATH=$2

ACCESS=$(curl -sf -X POST "$BASE_URL/auth/session/token/simple" -H 'Content-Type: application/json' -d "\"$(cat testing/.admin-token)\"" | tr -d '"')

if [[ $# -ge 3 ]]; then
    curl -s -X "$METHOD" "$BASE_URL$API_PATH" -H "Authorization: $ACCESS" -H 'Content-Type: application/json' -d "$3"
else
    curl -s -X "$METHOD" "$BASE_URL$API_PATH" -H "Authorization: $ACCESS"
fi
echo
