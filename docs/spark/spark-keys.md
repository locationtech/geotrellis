# Keys in geotrellis.spark

## `K` (key-type - often `GridKey` or `GridTimeKey`)

Type `K` can be just about anything you like - it serve a role
analogous to the 'key' in a hash-map. A simple `K` might be `Int`, which
would allow reference to a `V` by way of the integer with which it is
associated. More useful cases of `K` are defined for us: `GridKey` and
`GridTimeKey`, which serve as two-dimensional (spatial) and three-
dimensional (spatio-temporal) keys respectively.  

A tiling server (which creates images for consumption by something like
[Leaflet](http://leafletjs.com/) or [OpenLayers](http://openlayers.org/)
might represent its rows and columns of tile-images with the `GridKey`.
[Upon closer inspection](../../spark/src/main/scala/geotrellis/spark/GridKey.scala)),
we can see that `GridKey` is little more than a `Tuple2`.  

### Establishing boundaries

Unlike a simple tuple, `GridKey` implements the typeclass `Boundable`.
This extension provides logic related to a key's boundaries which can be
useful when querying and partitioning RDDs. If we had the points (1, 2)
and (3, 1) on the Cartesian coordinate plane, we would implement our
`Boundable` typeclass for these 'keys' such that their `minBound` is
(1, 1) and their `maxBound` is (3, 2).  

For merely spatial (2d) representations, `GridKey` will be sufficient
for most purposes. Sometimes, though, it will be useful to carry out
calculations over the same set of tiles *at different points in time*.
In such instances, the `GridTimeKey` is more useful: it represents a
column, a row, and an instant and can produce either a `GridKey` or a
`TimeKey` on demand. And, as with `GridKey`, it is `Boundable`.  

### Operations on `V`s depend on `K`s

We saw in the section on `V` types that even after we've settled on working
with `Tile`s and `TileLayerMetadata`, `K` remains generic. This is
because that type instance is very important for determining what can be
done to a set of tiles. Imagine what follows from using `Int` as a `K`:
what operations are possible which can vary according to different
values of `Int`? Perhaps we're certain that our tiles all sit in a neat
line - in that case, we could implement functions that carry out
space-dependent operations.  

Of course, that's just not the case usually. It is far, far more common
to represent a tile layout in terms of lat/long or x/y Cartesian products.
Another way of putting this is that spatially aware functions vary over
two dimensions - for a robust set of spatial operations, we should
ensure access to values for both of these dimensions. That's precisely
what we `SpatialKey` provides us.  

If you look to the function signatures in
`geotrellis.spark.mapalgebra.focal`, you'll notice that `K` is, more
often than not, required to implement the typeclass `SpatialComponent`.
This typeclass  proves that an instance of `K` has the method
`spatialComponent` which provides a `SpatialKey` instance which can be
used for operations which require spatial orientation of a tile. With
this in mind, it is perhaps unsurprising that we find such a guarantee
in the focal operations of `geotrellis.spark`: focal operations require
contextual information about neighboring cells - neighbors which might
well exist on a different `Tile` altogether, thus necessitating some way
of conveniently determining which other `Tile`s an operation might need
to query.  

### Ingest Keys

> TODO: Difference between ingest and regular keys

### Keys as Lenses

> TODO: Explanation of KeyComponent[A, B] pattern
