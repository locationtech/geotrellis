#!/bin/bash

# This docker container must be running in order to run tests that interface with S3
docker run -d --restart=always \
    -p 9091:9000 \
    -e MINIO_ACCESS_KEY=minio -e MINIO_SECRET_KEY=password \
    minio/minio:RELEASE.2019-05-02T19-07-09Z \
    server /data