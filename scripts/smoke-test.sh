#!/usr/bin/env bash
# Runs against the real Docker setup (app + PostgreSQL) in CI: opens accounts, moves money, checks the rules.
set -euo pipefail
BASE=${BASE:-http://localhost:8080}

echo "Waiting for the app to start..."
for i in $(seq 1 60); do
  if curl -fs "$BASE/actuator/health" | grep -q UP; then break; fi
  sleep 2
done
curl -fs "$BASE/actuator/health" | grep -q UP

post() { curl -s -o /tmp/body -w "%{http_code}" -H "Content-Type: application/json" -d "$2" "$BASE$1"; }

code=$(post /api/accounts '{"ownerName":"Alice","openingBalance":10000.00}'); [ "$code" = 201 ]; A=$(jq .id /tmp/body)
code=$(post /api/accounts '{"ownerName":"Bob","openingBalance":0}');          [ "$code" = 201 ]; B=$(jq .id /tmp/body)

code=$(post /api/transfers "{\"fromAccountId\":$A,\"toAccountId\":$B,\"amount\":25.50}")
[ "$code" = 201 ] || { echo "transfer failed: $code $(cat /tmp/body)"; exit 1; }

code=$(post /api/transfers "{\"fromAccountId\":$B,\"toAccountId\":$A,\"amount\":1000}")
[ "$code" = 422 ] || { echo "overdraft wasn't refused: $code"; exit 1; }

code=$(post /api/transfers "{\"fromAccountId\":$A,\"toAccountId\":$B,\"amount\":-5}")
[ "$code" = 400 ] || { echo "negative amount wasn't refused: $code"; exit 1; }

code=$(post /api/transfers "{\"fromAccountId\":$A,\"toAccountId\":$B,\"amount\":6000}")
[ "$code" = 201 ] && jq -e '.fraudFlags | index("LARGE_AMOUNT")' /tmp/body > /dev/null

[ "$(curl -fs "$BASE/api/accounts/$B" | jq -r .balance)" = "6025.5" ] || [ "$(curl -fs "$BASE/api/accounts/$B" | jq -r .balance)" = "6025.50" ]
[ "$(curl -fs "$BASE/api/accounts/$A/transfers" | jq length)" = 3 ]
curl -fs "$BASE/v3/api-docs" > /dev/null

echo "Smoke test passed against PostgreSQL"
