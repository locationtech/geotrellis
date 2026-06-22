#!/bin/bash

# This docker container must be running in order to run tests that interface with S3
docker run -d --restart=always \
    -p 9091:9000 \
    -e MINIO_ROOT_USER=minio -e MINIO_ROOT_PASSWORD=password \
    minio/minio:RELEASE.2025-09-07T16-13-09Z \
    server /data