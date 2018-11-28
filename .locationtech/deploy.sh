#!/usr/bin/env bash

 set -e
 set -x

./.locationtech/deploy-211.sh && ./.locationtech/deploy-212.sh
