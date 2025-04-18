# docker buildx create --use --platform=linux/arm64,linux/amd64 --name multi-platform-builder
# docker buildx inspect --bootstrap
#
# docker buildx build --build-arg="JAVA_VERSION=11" --build-arg="GDAL_VERSION=3.10.3" --platform linux/amd64,linux/arm64 --push --tag daunnc/osgeo-gdal-jdk11:3.10.3 .
# docker buildx build --build-arg="JAVA_VERSION=17" --build-arg="GDAL_VERSION=3.10.3" --platform linux/amd64,linux/arm64 --push --tag daunnc/osgeo-gdal-jdk17:3.10.3 .
# docker buildx build --build-arg="JAVA_VERSION=21" --build-arg="GDAL_VERSION=3.10.3" --platform linux/amd64,linux/arm64 --push --tag daunnc/osgeo-gdal-jdk21:3.10.3 .

ARG GDAL_VERSION=3.10.3
FROM ghcr.io/osgeo/gdal:ubuntu-full-${GDAL_VERSION}

ARG JAVA_VERSION=21

LABEL Grigory Pomadchin <daunnc@gmail.com>

ENV DEBIAN_FRONTEND noninteractive

RUN set -ex && \
    apt update -y && \
    apt install --no-install-recommends -y openjdk-${JAVA_VERSION}-jdk

ENV JAVA_HOME /usr/lib/jvm/java-${JAVA_VERSION}-openjdk-amd64
RUN update-alternatives --set java `update-alternatives --list java | grep java-${JAVA_VERSION}`

ENV LD_LIBRARY_PATH "/lib/x86_64-linux-gnu/:/lib/x86_64-linux-gnu/jni/:/lib/aarch64-linux-gnu/:/lib/aarch64-linux-gnu/jni/:$LD_LIBRARY_PATH"

RUN apt-get update && apt-get -y install bash wget unzip gpg software-properties-common

# Install SBT
RUN apt-get install -y curl gnupg && \
 echo 'deb https://repo.scala-sbt.org/scalasbt/debian all main' | tee /etc/apt/sources.list.d/sbt.list && \
 echo 'deb https://repo.scala-sbt.org/scalasbt/debian /' | tee /etc/apt/sources.list.d/sbt_old.list && \
 curl -sL 'https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823' | apt-key add - && \
 apt-get update && \
 apt-get install -y sbt
