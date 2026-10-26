# Multi-stage build: compile with Gradle, run with JRE
# Build context: olo-mono repository root.
FROM gradle:8-jdk21 AS builder
WORKDIR /workspace/olo

COPY gradle ../gradle
COPY olo-spi ../olo-spi
COPY olo-annotation ../olo-annotation
COPY olo-definition ../olo-definition
COPY olo-workflow-input ../olo-workflow-input
COPY olo/olo-temporal-sdk olo-temporal-sdk
COPY olo-toolgate-client ../olo-toolgate-client
COPY olo/gradle gradle
COPY olo/gradlew olo/gradlew.bat olo/settings.gradle olo/build.gradle olo/gradle.properties ./
COPY olo/docker/olo-mono-ci.init.gradle /tmp/olo-mono-ci.init.gradle
COPY olo/src src
RUN find /workspace -name gradlew -exec chmod +x {} +

RUN cd ../olo-spi \
    && gradle -I /tmp/olo-mono-ci.init.gradle -PoloPublishBuildDir=../build/publish-work/olo-spi publishMavenPublicationToOloMonoRepository -x test --no-daemon \
    && test -f ../build/repo/org/olo/olo-spi/0.1.0-SNAPSHOT/maven-metadata.xml \
    && cd ../olo-annotation \
    && gradle -I /tmp/olo-mono-ci.init.gradle -PoloPublishBuildDir=../build/publish-work/olo-annotation publishMavenPublicationToOloMonoRepository -x test --no-daemon \
    && test -f ../build/repo/org/olo/olo-annotation/0.1.0-SNAPSHOT/maven-metadata.xml \
    && cd ../olo-definition \
    && gradle -I /tmp/olo-mono-ci.init.gradle -PoloPublishBuildDir=../build/publish-work/olo-definition :olo-definition:publishMavenPublicationToOloMonoRepository -x test --no-daemon \
    && test -f ../build/repo/org/olo/olo-definition/0.1.0-SNAPSHOT/maven-metadata.xml \
    && cd ../olo-workflow-input \
    && gradle -I /tmp/olo-mono-ci.init.gradle -PoloPublishBuildDir=../build/publish-work/olo-workflow-input publishMavenPublicationToOloMonoRepository -x test --no-daemon \
    && test -f ../build/repo/org/olo/olo-workflow-input/0.1.0-SNAPSHOT/maven-metadata.xml \
    && cd /workspace/olo \
    && chmod +x gradlew \
    && gradle -I /tmp/olo-mono-ci.init.gradle bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN adduser -D -h /app appuser
USER appuser

COPY --from=builder /workspace/olo/build/libs/olo-backend-*.jar app.jar
COPY olo-definition/olo-configuration /app/olo-configuration

ENV OLO_CONFIGURATION_DIR=/app/olo-configuration

EXPOSE 7080
ENTRYPOINT ["java", "-jar", "app.jar"]
