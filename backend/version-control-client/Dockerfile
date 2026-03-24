FROM maven:3.9-eclipse-temurin-21
WORKDIR /build

ENV VCS_SERVER_URL=http://host.docker.internal:8000/api/v1/vcs \
    KEYCLOAK_URL=http://host.docker.internal:8085/realms/cilab-realm/protocol/openid-connect/token \
    CLIENT_ID=cilab-client

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package

RUN echo '#!/bin/sh' > /usr/local/bin/myvcs && \
    echo 'java -jar /build/target/version-control-client-1.0-SNAPSHOT.jar "$@"' >> /usr/local/bin/myvcs && \
    chmod +x /usr/local/bin/myvcs