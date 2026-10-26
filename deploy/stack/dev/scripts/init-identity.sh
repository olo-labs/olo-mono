#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
umask 077
touch .env
for name in OLO_DEV_ADMIN_PASSWORD OLO_DEV_USER_PASSWORD OLO_CALLBACK_KEY; do
  if ! grep -qE "^${name}=.+$" .env; then
    value="$(openssl rand -hex 32)"
    printf '\n%s=%s\n' "$name" "$value" >> .env
  fi
done
if ! grep -qE '^OLO_CALLBACK_KEY_ID=.+$' .env; then
  printf '\nOLO_CALLBACK_KEY_ID=stack-dev\n' >> .env
fi
echo 'Local identity credentials are ready in dev/.env (not tracked by Git).'
