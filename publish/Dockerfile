ARG RELEASE_TAG=2.0
FROM java:8

USER root

RUN apt-get install git

RUN useradd -ms /bin/bash geotrellis

RUN mkdir /home/geotrellis/.ivy2
COPY gpg.sbt /home/geotrellis/.sbt/1.0/plugins/gpg.sbt
RUN chown -R geotrellis:geotrellis /home/geotrellis

USER geotrellis

RUN set -x \
    && cd /home/geotrellis \
    && git clone https://github.com/locationtech/geotrellis /home/geotrellis/geotrellis \
    && cd /home/geotrellis/geotrellis \
    && git checkout $RELEASE_TAG

WORKDIR /home/geotrellis/geotrellis