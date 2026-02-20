ARG GRADLE_VERSION=9.3.1
ARG JAVA_VERSION=25

# Build stage
FROM gradle:${GRADLE_VERSION}-jdk${JAVA_VERSION}-alpine AS build
WORKDIR /app

# Copy the entire backend folder (including modules, gradlew, and kts files)
COPY freshly-backend/ .

# Add execute permissions to gradlew
RUN chmod +x gradlew

# Build application JAR
RUN ./gradlew bootJar --no-daemon --parallel

# Production stage
FROM azul/zulu-openjdk-alpine:${JAVA_VERSION}-jre AS production
WORKDIR /app

# Install system dependencies, create a non-root user, and set permissions for uploads directory
RUN apk add --no-cache \
    bash \
    netcat-openbsd \
    curl \
    dos2unix \
    postgresql-client && \
    addgroup --system --gid 1001 appuser && \
    adduser --system --uid 1001 --ingroup appuser appuser && \
    mkdir -p /var/app/uploads && \
    chown -R appuser:appuser /var/app/uploads

# Copy the built JAR from the build stage
COPY --from=build /app/modules/app/build/libs/*.jar app.jar

USER appuser

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

CMD ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]