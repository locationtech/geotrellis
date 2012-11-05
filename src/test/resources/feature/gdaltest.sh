#!/bin/bash
arg="$1"

## determine how many cells have values in a gdal rasterization of this geojson

gdal_rasterize -ts 300 300 -te 0 0 300 300 -of GTiff -burn 255 $arg.json $arg.tif

echo "result is:" 
gdal2xyz.py $arg.tif | egrep 255$ | wc -l
