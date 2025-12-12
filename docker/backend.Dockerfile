# Arguments
ARG JAVA_VERSION=25
ARG GRADLE_VERSION=9.2.1
ARG BACKEND_PORT=8080
ARG BACKEND_DEBUG_PORT=5005

# Base stage
FROM eclipse-temurin:${JAVA_VERSION}-jdk AS base

WORKDIR /app

# Install Gradle
ARG GRADLE_VERSION
RUN apt-get update && apt-get install -y wget unzip && \
    wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -P /tmp && \
    unzip -d /opt/gradle /tmp/gradle-${GRADLE_VERSION}-bin.zip && \
    ln -s /opt/gradle/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle && \
    rm /tmp/gradle-${GRADLE_VERSION}-bin.zip && \
    apt-get remove -y wget unzip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Development stage
FROM base AS development

WORKDIR /app

ARG BACKEND_PORT
ARG BACKEND_DEBUG_PORT

EXPOSE ${BACKEND_PORT}
EXPOSE ${BACKEND_DEBUG_PORT}

# Run the application with devtools and remote debugging using Gradle
CMD ["gradle", "bootRun", "--args=--spring-boot.run.jvmArguments='-XX:TieredStopAtLevel=1 -Dspring.devtools.restart.enabled=true -Dspring.devtools.restart.poll-interval=2s -Dspring.devtools.restart.quiet-period=1s -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'"]

# Build stage
FROM base AS build

WORKDIR /app

COPY fastprod-backend .
RUN gradle clean bootJar -x test -x spotlessCheck -x spotlessApply

# Production stage
FROM eclipse-temurin:${JAVA_VERSION}-jre AS production

WORKDIR /app

ARG BACKEND_PORT
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE ${BACKEND_PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]