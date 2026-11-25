#!/usr/bin/env bash
# Smoke test for a running deployment: talks to the Service through kubectl port-forward
# and walks the main API flow (admin login, start draw, register, buy ticket, read results).
set -euo pipefail

NS="${NAMESPACE:-lottofun}"
PORT="${LOCAL_PORT:-18080}"
BASE="http://localhost:${PORT}"

kubectl -n "$NS" port-forward svc/lottofun "${PORT}:80" >/dev/null 2>&1 &
PF_PID=$!
trap 'kill "$PF_PID" 2>/dev/null || true' EXIT

for _ in $(seq 1 30); do
  curl -sf "$BASE/actuator/health/readiness" >/dev/null && break
  sleep 1
done

check() { # check <description> <expected-status> <actual-status>
  if [ "$2" != "$3" ]; then echo "FAIL: $1 (expected $2, got $3)"; exit 1; fi
  echo "ok:   $1 ($3)"
}

ready=$(kubectl -n "$NS" get deployment lottofun -o jsonpath='{.status.readyReplicas}')
check "both replicas ready" 2 "${ready:-0}"

check "readiness probe" 200 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/actuator/health/readiness")"
check "protected endpoint without token" 401 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/draws/history")"

secret=$(kubectl -n "$NS" get secrets -o name | grep lottofun-secrets | head -1)
admin_password=$(kubectl -n "$NS" get "$secret" -o jsonpath='{.data.ADMIN_PASSWORD}' | base64 -d)

json() { python3 -c 'import json,sys; print(json.load(sys.stdin)[sys.argv[1]])' "$1"; }
post() { curl -s -w '\n%{http_code}' -X POST "$BASE$1" -H 'Content-Type: application/json' "${@:2}"; }

admin=$(post /api/v1/auth/login -d "{\"username\":\"admin\",\"password\":\"$admin_password\"}")
check "admin login" 200 "$(tail -1 <<<"$admin")"
admin_token=$(head -1 <<<"$admin" | json token)

start_status=$(post /api/v1/draws/start -H "Authorization: Bearer $admin_token" | tail -1)
# 409 means a draw is already open, for example on a second run.
if [ "$start_status" != 201 ] && [ "$start_status" != 409 ]; then check "start draw" 201 "$start_status"; fi
echo "ok:   start draw ($start_status)"

user="smoke$RANDOM$RANDOM"
reg=$(post /api/v1/auth/register -d "{\"username\":\"$user\",\"password\":\"secret123\"}")
check "register user" 200 "$(tail -1 <<<"$reg")"
user_token=$(head -1 <<<"$reg" | json token)

buy=$(post /api/v1/tickets/buy -H "Authorization: Bearer $user_token" -d '{"selectedNumbers":[7,14,21,35,49]}')
check "buy ticket" 201 "$(tail -1 <<<"$buy")"

results=$(curl -s -H "Authorization: Bearer $user_token" "$BASE/api/v1/tickets/my-results")
count=$(python3 -c 'import json,sys; print(len(json.load(sys.stdin)))' <<<"$results")
check "ticket visible in my-results" 1 "$count"

echo "Smoke test passed."
