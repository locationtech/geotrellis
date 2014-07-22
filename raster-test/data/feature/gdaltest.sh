#!/bin/bash
arg="$1"

## determine how many cells have values in a gdal rasterization of this geojson

gdal_rasterize -ts 300 300 -te 0 0 300 300 -of GTiff -burn 255 $arg.json $arg.tif

gdal_translate -of xyz $arg.tif $arg.xyz

echo "cells included in rasterization:"
egrep 255$ $arg.xyz | wc -l

rm $arg.xyz
