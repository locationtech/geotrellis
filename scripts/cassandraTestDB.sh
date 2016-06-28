#!/bin/bash

docker pull cassandra:latest

docker run \
  --rm \
  --net=host \
  -m 1G \
  --memory-swap -1 \
  --env="MAX_HEAP_SIZE=500M" \
  --env="HEAP_NEWSIZE=100M" \
  --env="CASSANDRA_LISTEN_ADDRESS=127.0.0.1" cassandra:latest
