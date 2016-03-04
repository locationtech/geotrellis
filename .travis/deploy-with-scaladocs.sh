#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Deploy
$DIR/deploy.sh

# Publish scaladocs
$DIR/scaladocs.sh
