FROM java:8

USER root

RUN apt-get install git

RUN useradd -ms /bin/bash geotrellis

USER geotrellis

RUN set -x \
    && cd /home/geotrellis \
    && git clone https://github.com/locationtech/geotrellis /home/geotrellis/geotrellis \
    && cd /home/geotrellis/geotrellis \
    && git checkout 1.1

WORKDIR /home/geotrellis/geotrellis

COPY .credentials /home/geotrellis/.ivy2/.credentials
COPY ./gnupg/ /home/geotrellis/.gnupg/
COPY local.sbt /home/geotrellis/.sbt/0.13/local.sbt
COPY plugins.sbt /home/geotrellis/.sbt/0.13/plugins/local.sbt

USER root

RUN chown -R geotrellis:geotrellis /home/geotrellis

USER geotrellis
