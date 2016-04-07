# Geotrellis Vector Tile

## 1. Purpose

The Geotrellis Vector Tile library provides scala classes for interfacing with
the [Mapbox Vector Tile Specification](
https://github.com/mapbox/vector-tile-spec/tree/master/2.1). It provides
functionality to decode, manipulate, and encode in the Vector Tile Format.

## 2. Instructions

TODO

## 3. Examples

TODO

## 4. Notes and References

TODO

* There is some uncertainty about what kinds of inputs are useful to decode.
For instance, should it be valid to close a polygon when it is a single
line? How about if it is a single point? We have take a rather lenient approach,
prioritizing speed and accept the commands as written and leave it to jts to
decide whether it is valid or not.

