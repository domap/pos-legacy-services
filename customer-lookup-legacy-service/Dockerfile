# syntax=docker/dockerfile:1
#
# Build (from repo root):
#   docker build -f customer-lookup-legacy-service/Dockerfile -t customer-lookup-legacy:local customer-lookup-legacy-service
#
# Optional OpenTelemetry Java agent (pin digest in production):
#   COPY --from=ghcr.io/open-telemetry/opentelemetry-java-instrumentation/opentelemetry-javaagent:2.11.0 \
#        /opentelemetry-javaagent.jar /opt/otel/opentelemetry-javaagent.jar
#   ENV JAVA_TOOL_OPTIONS="-javaagent:/opt/otel/opentelemetry-javaagent.jar"
# OTLP endpoint is wired via OTEL_EXPORTER_OTLP_* / management.otlp.tracing.endpoint in application.yml.

FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /workspace
COPY mvnw .
COPY .mvn/wrapper .mvn/wrapper
COPY pom.xml .
COPY src src
RUN chmod +x mvnw && ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:25-jre-jammy
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --shell /usr/sbin/nologin --no-create-home app
WORKDIR /app
COPY --from=build /workspace/target/customer-lookup-legacy.jar /app/app.jar
RUN chown -R app:app /app
USER app
ENV SERVER_PORT=8081
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar /app/app.jar"]
