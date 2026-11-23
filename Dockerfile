# Build stage
FROM maven:3.10.0-eclipse-temurin-17 AS build
WORKDIR /build
COPY /external /build/external
COPY /refresh-cwlib-worker /build/refresh-cwlib-worker
COPY /pom.xml /build/pom.xml
RUN mvn -f pom.xml clean package

# Final running container

FROM eclipse-temurin:11-jdk-alpine AS final

# Add non-root user
RUN set -eux && \
apk add su-exec && \
su-exec nobody true && \
addgroup -gid 1001 refresh && \
adduser -m --home /refresh -uid 1001 --ingroup refresh refresh &&\
mkdir -p /refresh/data && \
mkdir -p /refresh/app

COPY --from=build /build/target/refresh-cwlib-worker-*.jar /refresh/app/worker.jar

RUN chown -R refresh:refresh /refresh

ENTRYPOINT java -jar /refresh/app/worker.jar
