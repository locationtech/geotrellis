Writing a Custom Key Type
=========================

*Want to jump straight to a code example? See
`doc-examples/src/main/scala/geotrellis/doc/examples/spark/VoxelKey.scala`.*

Keys are used to index tiles in a tile layer. Typically these tiles are
arranged in some conceptual grid, for instance in a two-dimensional matrix via a
`SpatialKey`. There is also a `SpaceTimeKey`, which arranges tiles in a grid
of two spatial dimensions and one time dimension.

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
parameter, for instance in the `LayerReader` trait:

```scala
// slightly simplified
LayerReader.read[K: Boundable: JsonFormat, V, M]: LayerId => RDD[(K, V)] with Metadata[M]
```

`Boundable` and `JsonFormat` are frequent constraints on keys. Let's give those
typeclasses some implementations:

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
}
```

A Z-Curve SFC for `VoxelKey`
----------------------------

Many operations require a `KeyIndex` as well, which are usually implemented
with some hardcoded key type. `VoxelKey` would need one as well, which we will
back by a Z-Curve for this example:

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
details, see `ShardingKeyIndex.scala`.
