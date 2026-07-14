FROM gradle:8.12.1-jdk21 AS builder

WORKDIR /build

COPY . .

RUN chmod +x publish-libs.sh && ./publish-libs.sh
RUN gradle -p olo-worker installDist --no-daemon -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/olo-worker/build/install/olo-worker/ /app/
COPY olo-worker-configuration/samples/worker-config.docker.yaml /app/worker-config.yaml
COPY olo-definition/olo-configuration/current-active/ /app/olo-configuration/current-active/

ENV OLO_WORKER_CONFIG_PATH=/app/worker-config.yaml
ENV OLO_LLM_BASE_URL=http://ollama:11434

ENTRYPOINT ["/app/bin/olo-worker"]