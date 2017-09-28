#!/bin/bash

docker pull cassandra:latest

docker run \
  --rm \
  -p 9160:9160 \
  -p 9042:9042 \
  -m 1G \
  --memory-swap -1 \
  --env="MAX_HEAP_SIZE=500M" \
  --env="HEAP_NEWSIZE=100M" \
  --env="CASSANDRA_LISTEN_ADDRESS=127.0.0.1" cassandra:latest
