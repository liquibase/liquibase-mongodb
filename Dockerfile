FROM ubuntu:20.04

RUN apt-get update && apt-get install -y default-jre curl coreutils

WORKDIR /liquibase
RUN curl https://github.com/liquibase/liquibase/releases/download/v4.0.0/liquibase-4.0.0.tar.gz -L -O
RUN tar -xzf liquibase-4.0.0.tar.gz
WORKDIR /liquibase/lib
RUN curl https://repo1.maven.org/maven2/org/mongodb/mongodb-driver-sync/4.1.0/mongodb-driver-sync-4.1.0.jar -L -O
RUN curl https://repo1.maven.org/maven2/org/mongodb/mongo-java-driver/3.12.7/mongo-java-driver-3.12.7.jar -L -O
RUN curl https://repo1.maven.org/maven2/org/mongodb/mongodb-driver/3.12.7/mongodb-driver-3.12.7.jar -L -O
RUN curl https://repo1.maven.org/maven2/org/mongodb/bson/3.12.6/bson-3.12.7.jar -L -O
RUN curl https://repo1.maven.org/maven2/org/mongodb/mongodb-driver-core/3.12.7/mongodb-driver-core-3.12.7.jar -L -O
COPY target/liquibase-mongodb-4.0.1-SNAPSHOT.jar ./

WORKDIR /liquibase

RUN mkdir changelog

COPY docker-entrypoint.sh /docker-entrypoint.sh
COPY src/test/resources/liquibase/ext/*.xml ./changelog/

ENTRYPOINT "/docker-entrypoint.sh"