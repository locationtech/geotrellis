#!/bin/bash

docker pull quay.io/azavea/postgis:0.1.0

docker run \
  -it \
  --rm \
  -p 5432:5432 \
  -e POSTGRES_DB=slick_tests \
  quay.io/azavea/postgis:0.1.0
