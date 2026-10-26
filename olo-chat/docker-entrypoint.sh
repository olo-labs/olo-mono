#!/bin/sh
# Copyright (c) 2026 Olo Labs
# SPDX-License-Identifier: Apache-2.0
set -e

: "${OLO_BACKEND_URL:=http://olo:7080}"
OLO_BACKEND_URL=$(echo "$OLO_BACKEND_URL" | sed 's#/$##')
export OLO_BACKEND_URL

envsubst '${OLO_BACKEND_URL}' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf

exec "$@"
