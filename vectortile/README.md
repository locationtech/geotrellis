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
its `.pbf -> Scala` bridge code. The tool itself offers an SBT plugin to do
this, but I've found it easier to run their [`spbc` tool directly](https://scalapb.github.io/).

Once you have that, generate the code as follows. From the `vectortile/` directory:

```
spbc vector_tile.proto --scala_out=.
```

This will produce some scala sources that will need to override what's present
in `/vectortile/src/main/scala/geotrellis/vectortile/internal/vector_tile`.
This should be two files, `Tile.scala` and `VectorTileProto.scala`.

To get everything to compile, you will also have to manually change the package
names in each of the files to:

```scala
package geotrellis.vectortile.internal.vector_tile
```

Please also add the Apache 2 license headers to each file.

You will have to complete this entire process whenever you choose to update the `scalapb`
dependency line in `vectortile/build.sbt`.
