# Values in geotrellis.spark

## `V` (value-type - often `Tile`)

We'll take a look at `V` first, because `K` and `M` make more sense when
the place of `V` is clear.  

`V` is the value on which computations are to be performed through spark
jobs. Because GeoTrellis is a library focused on raster operations, type
`V` will mostly just be `Tile`. As such, you'll see a lot of code with
the type `RDD[(K, Tile)]`. In fact, you can even find a type alias at the
top of [`geotrellis.spark`'s
package.scala](../../spark/src/main/scala/geotrellis/spark/package.scala)
which provides some type sugar for working with this instance of `V`:

```scala
type TileLayerRDD[K] = RDD[(K, Tile)] with Metadata[TileLayerMetadata]
```

This makes sense: we want to provide a ay to extend the functionality
of `geotrellis.raster` to a distributed context so much of the
code in `geotrellis.spark` is written with `RDD[(K, Tile)] with
Metadata[TileLayerMetadata]` in mind. Note that `K` remains generic.
This suggests that, when working with `Tile`s, much depends on `K`.
In the next section, we'll see why that is the case.  
