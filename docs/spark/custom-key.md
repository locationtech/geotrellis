Writing a Custom Key Type
=========================

*Want to jump straight to a code example? See*
[VoxelKey.scala](https://github.com/geotrellis/geotrellis/tree/master/doc-examples/src/main/scala/geotrellis/doc/examples/spark/VoxelKey.scala)

Intro
-----

Keys are used to index (or "give a position to") tiles in a tile layer.
Typically these tiles are arranged in some conceptual grid, for instance in
a two-dimensional matrix via a
[`SpatialKey`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/SpatialKey.scala).
There is also a
[`SpaceTimeKey`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/SpaceTimeKey.scala),
which arranges tiles in a cube of two spatial dimensions and one time
dimension.

In this way, keys define how a tile layer is shaped. Here, we provide an example of how
to define a new key type, should you want a custom one for your application.

The `VoxelKey` type
-------------------

A voxel is the 3D analogue to a 2D pixel. By defining a new `VoxelKey` type,
we can create grids of tiles that have a 3D spatial relationship. The class
definition itself is simple:

```scala
case class VoxelKey(x: Int, y: Int, z: Int)
```

Key usage in many GeoTrellis operations is done generically with a `K` type
parameter, for instance in the `S3LayerReader` class:

```scala
/* Read a tile layer from S3 via a given `LayerId`. Function signature slightly simplified. */
S3LayerReader.read[K: Boundable: JsonFormat, V, M]: LayerId => RDD[(K, V)] with Metadata[M]
```

Where the pattern `[A: Trait1: Trait2: ...]` means that for whichever `A`
you end up using, it must have an implicit instance of `Trait1` and `Trait2`
(and any others) in scope. Really it's just syntactic sugar for
`[A](implicit ev0: Trait1[A], ev1: Trait2[A], ...)`. The `read` method above
would be used in real life like:

```scala
val reader: S3LayerReader = ...

// The type on `rdd` is often left off for brevity.
val rdd: RDD[(SpatialKey, MultibandTile)] with Metadata[LayoutDefinition] =
    reader.read[SpatialKey, MultibandTile, LayoutDefinition]("someLayer")
```

[`Boundable`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/Boundable.scala),
`SpatialComponent`,  and `JsonFormat` are frequent constraints on keys.
Let's give those typeclasses some implementations:

```scala
import geotrellis.spark._
import spray.json._

// A companion object is a good place for typeclass instances.
object VoxelKey {

  // What are the minimum and maximum possible keys in the key space?
  implicit object Boundable extends Boundable[VoxelKey] {
    def minBound(a: VoxelKey, b: VoxelKey) = {
      VoxelKey(math.min(a.x, b.x), math.min(a.y, b.y), math.min(a.z, b.z))
    }

    def maxBound(a: VoxelKey, b: VoxelKey) = {
      VoxelKey(math.max(a.x, b.x), math.max(a.y, b.y), math.max(a.z, b.z))
    }
  }

  /** JSON Conversion */
  implicit object VoxelKeyFormat extends RootJsonFormat[VoxelKey] {
    // See full example for real code.
    def write(k: VoxelKey) = ...

    def read(value: JsValue) = ...
  }

  /** Since [[VoxelKey]] has x and y coordinates, it can take advantage of
    * the [[SpatialComponent]] lens. Lenses are essentially "getters and setters"
    * that can be used in highly generic code.
    */
  implicit val spatialComponent = {
    Component[VoxelKey, SpatialKey](
      /* "get" a SpatialKey from VoxelKey */
      k => SpatialKey(k.x, k.y),
      /* "set" (x,y) spatial elements of a VoxelKey */
      (k, sk) => VoxelKey(sk.col, sk.row, k.z)
    )
  }
}
```

With these, `VoxelKey` is now (almost) usable as a key type in GeoTrellis.

A Z-Curve SFC for `VoxelKey`
----------------------------

Many operations require a
[`KeyIndex`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/io/index/KeyIndex.scala)
as well, which are usually implemented with some hardcoded key type.
`VoxelKey` would need one as well, which we will back by a Z-Curve for this
example:

```scala
/** A [[KeyIndex]] based on [[VoxelKey]]. */
class ZVoxelKeyIndex(val keyBounds: KeyBounds[VoxelKey]) extends KeyIndex[VoxelKey] {
  /* ''Z3'' here is a convenient shorthand for any 3-dimensional key. */
  private def toZ(k: VoxelKey): Z3 = Z3(k.x, k.y, k.z)

  def toIndex(k: VoxelKey): Long = toZ(k).z

  def indexRanges(keyRange: (VoxelKey, VoxelKey)): Seq[(Long, Long)] =
    Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
}
```

And with a `KeyIndex` written, it will of course need its own `JsonFormat`,
which demands some additional glue to make fully functional. For more
details, see
[ShardingKeyIndex.scala](https://github.com/geotrellis/geotrellis/blob/master/doc-examples/src/main/scala/geotrellis/doc/examples/spark/ShardingKeyIndex.scala).

We now have a new fully functional key type which defines a tile cube of three
spatial dimensions. Of course, there is nothing stopping you from defining a
key in any way you like: it could have three spatial and one time dimension (`EinsteinKey`?)
or even ten spatial dimensions (`StringTheoryKey` :wink: ). Happy tiling.
