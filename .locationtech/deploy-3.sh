#!/usr/bin/env bash

set -e
set -x

# .locationtech/sbtopts keeps the heap well below the 4Gi Jenkins pod limit;
# publishing one module at a time avoids HTTP 429 from repo.eclipse.org
./sbt -sbt-opts .locationtech/sbtopts "++3" \
  'set Global / concurrentRestrictions += Tags.limit(Tags.Publish, 1)' \
  publish -no-colors -J-Drelease=locationtech
