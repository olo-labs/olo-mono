# Copyright (c) 2026 Olo Labs
# SPDX-License-Identifier: Apache-2.0

# Build stage: install deps and build the Vite app.
# Default: no VITE_API_BASE — browser uses same-origin /api and /ws; nginx proxies to OLO_BACKEND_URL.
FROM node:20-alpine AS builder

WORKDIR /app

ARG VITE_API_BASE=
ARG VITE_WS_ACCESS_TOKEN=
ARG VITE_WS_PING_INTERVAL_SEC=10

ENV VITE_API_BASE=${VITE_API_BASE}
ENV VITE_WS_ACCESS_TOKEN=${VITE_WS_ACCESS_TOKEN}
ENV VITE_WS_PING_INTERVAL_SEC=${VITE_WS_PING_INTERVAL_SEC}

COPY package.json package-lock.json* ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

COPY . .
RUN npm run build

# Production stage: nginx serves static files and proxies /api and /ws to the backend container.
FROM nginx:alpine

RUN apk add --no-cache gettext

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80

ENV OLO_BACKEND_URL=http://olo:7080

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
