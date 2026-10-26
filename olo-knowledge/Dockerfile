# Build from D:\olo after publishing the pinned ToolGate SDK to its local repositories.
FROM gradle:8.12-jdk21 AS build
WORKDIR /workspace/olo-knowledge
COPY olo-toolgate/build/sdk-repo /workspace/olo-toolgate/build/sdk-repo
COPY olo-toolgate/.dev/maven /workspace/olo-toolgate/.dev/maven
COPY olo-knowledge /workspace/olo-knowledge
RUN gradle --no-daemon :olo-memory:installDist :olo-rag:installDist

FROM eclipse-temurin:21-jre AS memory
WORKDIR /app
COPY --from=build /workspace/olo-knowledge/olo-memory/build/install/olo-memory /app
USER 10001:10001
EXPOSE 8443
ENTRYPOINT ["/app/bin/olo-memory"]

FROM eclipse-temurin:21-jre AS rag
WORKDIR /app
COPY --from=build /workspace/olo-knowledge/olo-rag/build/install/olo-rag /app
USER 10002:10002
EXPOSE 8443
ENTRYPOINT ["/app/bin/olo-rag"]
