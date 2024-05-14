# docker buildx create --use --platform=linux/arm64,linux/amd64 --name multi-platform-builder
# docker buildx inspect --bootstrap
# 
# docker buildx build --build-arg="JAVA_VERSION=11.0.23_9" --platform linux/amd64,linux/arm64 --push --tag daunnc/gdal-jdk11:3.8.5 .
# docker buildx build --build-arg="JAVA_VERSION=17.0.11_9" --platform linux/amd64,linux/arm64 --push --tag daunnc/gdal-jdk17:3.8.5 .
# docker buildx build --build-arg="JAVA_VERSION=21.0.3_9" --platform linux/amd64,linux/arm64 --push --tag daunnc/gdal-jdk21:3.8.5 .

ARG JAVA_VERSION=11.0.23_9
FROM eclipse-temurin:${JAVA_VERSION}-jdk-jammy

LABEL Grigory Pomadchin <daunnc@gmail.com>

ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update && apt-get install -y libgdal-dev gdal-bin build-essential curl pkg-config libclang-dev

RUN ln -s /usr/lib/libgdal.so.30 /usr/lib/libgdal.so.34

# Install SBT
RUN apt-get install -y curl gnupg && \
 echo 'deb https://repo.scala-sbt.org/scalasbt/debian all main' | tee /etc/apt/sources.list.d/sbt.list && \
 echo 'deb https://repo.scala-sbt.org/scalasbt/debian /' | tee /etc/apt/sources.list.d/sbt_old.list && \
 curl -sL 'https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823' | apt-key add - && \
 apt-get update && \
 apt-get install -y sbt
