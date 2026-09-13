#!/bin/bash

# This docker container must be running in order to run tests that interface with S3
docker run -d --restart=always \
    -p 9091:9000 \
    -e MINIO_ROOT_USER=minio -e MINIO_ROOT_PASSWORD=password \
    pgsty/minio:RELEASE.2026-09-03T13-18-01Z \
    server /data