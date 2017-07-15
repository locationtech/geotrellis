#!/bin/bash

echo leader localhost > /tmp/hostaliases
docker pull jamesmcclain/geowave:e62deaf
docker network create --driver bridge geowave
docker run -td --restart=always --net=geowave \
       -p 50095:5009 -p 21810:2181 -p 9997:9997 -p 9999:9999 \
       --hostname leader --name leader \
       jamesmcclain/geowave:e62deaf
