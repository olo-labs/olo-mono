#!/bin/sh
set -e

java -jar /app/olo-be.jar &
JAVA_PID=$!

for i in $(seq 1 60); do
  if wget -q -O /dev/null http://127.0.0.1:8082/api/v1/health 2>/dev/null; then
    exec nginx -g "daemon off;"
  fi
  if ! kill -0 "$JAVA_PID" 2>/dev/null; then
    echo "olo-be exited before becoming healthy" >&2
    wait "$JAVA_PID" || true
    exit 1
  fi
  sleep 1
done

echo "olo-be did not become healthy within 60s" >&2
kill "$JAVA_PID" 2>/dev/null || true
exit 1
