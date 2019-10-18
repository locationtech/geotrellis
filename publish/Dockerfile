FROM java:8

USER root

# a hack that allows to set ENV var through ARGs
ARG RELEASE_TAG=v3.0.0
ENV RELEASE_TAG $RELEASE_TAG

RUN apt-get install git

RUN useradd -ms /bin/bash geotrellis

RUN mkdir /home/geotrellis/.ivy2
RUN chown -R geotrellis:geotrellis /home/geotrellis

USER geotrellis

RUN mkdir -p /home/geotrellis/.sbt/1.0/plugins && mkdir -p /home/geotrellis/.sbt/boot

RUN set -x \
    && cd /home/geotrellis \
    && git clone https://github.com/locationtech/geotrellis /home/geotrellis/geotrellis \
    && cd /home/geotrellis/geotrellis \
    && git checkout -f $RELEASE_TAG

WORKDIR /home/geotrellis/geotrellis
