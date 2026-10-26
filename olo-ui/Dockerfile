# Combined image: olo-be (Spring Boot) + olo-ui (nginx). Both run in one container.
# Build from repo root. CI copies olo-core dist/catalog and current-active workflows into olo-be before build.

# ---- Build backend (olo-be) ----
FROM gradle:8.5-jdk17-alpine AS be-builder
WORKDIR /build

COPY olo-be/ olo-be/
RUN cd olo-be && gradle build --no-daemon -x test

# ---- Build frontend (olo-ui) ----
# Use Debian-based image: Node Gradle plugin downloads glibc Node binaries; Alpine (musl) cannot run them.
FROM gradle:8.5-jdk21 AS ui-builder
WORKDIR /build

COPY olo-ui/ olo-ui/
RUN cd olo-ui && gradle build --no-daemon -x test

# ---- Runtime: JRE + nginx, run both ----
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache nginx wget

WORKDIR /app

# Backend jar (Gradle outputs to build/libs/)
COPY --from=be-builder /build/olo-be/build/libs/olo-be-*.jar /app/olo-be.jar

# Workflow presets (committed in olo-be/docker/runtime-configuration; CI may refresh from olo-mono)
COPY olo-be/docker/runtime-configuration /app/olo-configuration

# Frontend static files
COPY --from=ui-builder /build/olo-ui/dist /usr/share/nginx/html

# Nginx: serve UI and proxy /api to backend on 127.0.0.1:8082
RUN mkdir -p /etc/nginx/http.d
COPY docker-nginx.conf /etc/nginx/http.d/default.conf

COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENV OLO_CONFIGURATION_DIRECTORY=/app/olo-configuration
ENV OLO_LOG_DIRECTORY=/app/olo-configuration/log

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]
