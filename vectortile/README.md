VectorTiles
===========

Invented by [Mapbox](https://www.mapbox.com/), VectorTiles are a
combination of the ideas of finite-sized tiles and vector geometries. Mapbox
maintains the official implementation spec for VectorTile codecs. The
specification is free and open source.

VectorTiles are advantageous over raster tiles in that:
- They are typically smaller to store
- They can be easily transformed (rotated, etc.) in real time
- They allow for continuous (as opposed to step-wise) zoom in Slippy Maps

Raw VectorTile data is stored in the protobuf format. Any codec implementing
[the spec](https://github.com/mapbox/vector-tile-spec/tree/master/2.1) must
decode and encode data according to [this `.proto`
schema](https://github.com/mapbox/vector-tile-spec/blob/master/2.1/vector_tile.proto).

For more detailed information on VectorTiles, our data types, and usage
instructions for this library, see the Scaladocs.

### Updating `scalapb`

This codec uses [ScalaPB](https://github.com/scalapb/ScalaPB) to auto-generate
its `.pbf -> Scala` bridge code. All auto-generated files are placed into the `src_managed` folder 
during the `compilation` phase. The package name for the generated protocol is `geotrellis.vectortile.internal`. 
