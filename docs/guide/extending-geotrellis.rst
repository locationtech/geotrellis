Extending GeoTrellis Types
**************************

Custom Keys
===========

*Want to jump straight to a code example? See*
`VoxelKey.scala <https://github.com/geotrellis/geotrellis/tree/master/doc-examples/src/main/scala/geotrellis/doc/examples/spark/VoxelKey.scala>`__

Keys are used to index (or "give a position to") tiles in a tile layer.
Typically these tiles are arranged in some conceptual grid, for instance in
a two-dimensional matrix via a `SpatialKey
<https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/SpatialKey.scala>`__.
There is also a `SpaceTimeKey
<https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/SpaceTimeKey.scala>`__,
which arranges tiles in a cube of two spatial dimensions and one time
dimension.

In this way, keys define how a tile layer is shaped. Here, we provide an
example of how to define a new key type, should you want a custom one
for your application.

The VoxelKey type
-----------------

A voxel is the 3D analogue to a 2D pixel. By defining a new ``VoxelKey``
type, we can create grids of tiles that have a 3D spatial relationship.
The class definition itself is simple:

.. code:: scala

    case class VoxelKey(x: Int, y: Int, z: Int)

Key usage in many GeoTrellis operations is done generically with a ``K``
type parameter, for instance in the ``S3LayerReader`` class:

.. code:: scala

    /* Read a tile layer from S3 via a given `LayerId`. Function signature slightly simplified. */
    S3LayerReader.read[K: Boundable: JsonFormat, V, M]: LayerId => RDD[(K, V)] with Metadata[M]

Where the pattern ``[A: Trait1: Trait2: ...]`` means that for whichever
``A`` you end up using, it must have an implicit instance of ``Trait1``
and ``Trait2`` (and any others) in scope. Really it's just syntactic
sugar for ``[A](implicit ev0: Trait1[A], ev1: Trait2[A], ...)``. The
``read`` method above would be used in real life like:

.. code:: scala

    val reader: S3LayerReader = ...

    // The type on `rdd` is often left off for brevity.
    val rdd: RDD[(SpatialKey, MultibandTile)] with Metadata[LayoutDefinition] =
        reader.read[SpatialKey, MultibandTile, LayoutDefinition]("someLayer")

`Boundable
<https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/Boundable.scala>`__,
``SpatialComponent``, and ``JsonFormat`` are frequent constraints on keys.
Let's give those typeclasses some implementations:

.. code:: scala

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

With these, ``VoxelKey`` is now (almost) usable as a key type in
GeoTrellis.

A Z-Curve SFC for VoxelKey
--------------------------

Many operations require a `KeyIndex
<https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/io/index/KeyIndex.scala>`__
as well, which are usually implemented with some hardcoded key type.
``VoxelKey`` would need one as well, which we will back by a Z-Curve for
this example:

.. code:: scala

    /** A [[KeyIndex]] based on [[VoxelKey]]. */
    class ZVoxelKeyIndex(val keyBounds: KeyBounds[VoxelKey]) extends KeyIndex[VoxelKey] {
      /* ''Z3'' here is a convenient shorthand for any 3-dimensional key. */
      private def toZ(k: VoxelKey): Z3 = Z3(k.x, k.y, k.z)

      def toIndex(k: VoxelKey): Long = toZ(k).z

      def indexRanges(keyRange: (VoxelKey, VoxelKey)): Seq[(Long, Long)] =
        Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
    }

And with a ``KeyIndex`` written, it will of course need its own
``JsonFormat``, which demands some additional glue to make fully functional.
For more details, see `ShardingKeyIndex.scala
<https://github.com/geotrellis/geotrellis/blob/master/doc-examples/src/main/scala/geotrellis/doc/examples/spark/ShardingKeyIndex.scala>`__.

We now have a new fully functional key type which defines a tile cube of
three spatial dimensions. Of course, there is nothing stopping you from
defining a key in any way you like: it could have three spatial and one
time dimension (``EinsteinKey``?) or even ten spatial dimensions
(``StringTheoryKey``?). Happy tiling.

Custom KeyIndexes
=================

Want to dive right into code? See:
`ShardingKeyIndex.scala <https://github.com/geotrellis/geotrellis/tree/master/doc-examples/src/main/scala/geotrellis/doc/examples/spark/ShardingKeyIndex.scala>`__

The KeyIndex trait
------------------

The ``KeyIndex`` trait is high-level representation of Space Filling
Curves, and for us it is critical to Tile layer input/output. As of
GeoTrellis ``1.0.0``, its subclasses are:

-  ``ZSpatialKeyIndex``
-  ``ZSpaceTimeKeyIndex``
-  ``HilbertSpatialKeyIndex``
-  ``HilbertSpaceTimeKeyIndex``
-  ``RowMajorSpatialKeyIndex``

While the subclass constructors can be used directly when creating an
index, we always reference them generically elsewhere as ``KeyIndex``.
For instance, when we write an ``RDD``, we need to supply a generic
``KeyIndex``:

.. code:: scala

    S3LayerWriter.write[K, V, M]: (LayerId, RDD[(K, V)] with Metadata[M], KeyIndex[K]) => Unit

but when we read or update, we don't:

.. code:: scala

    S3LayerReader.read[K, V, M]: LayerId => RDD[(K, V)] with Metadata[M]

    S3LayerWriter.update[K, V, M]: (LayerId, RDD[(K, V)] with Metadata[M]) => Unit

Luckily for the end user of GeoTrellis, this means they don't need to
keep track of which ``KeyIndex`` subclass they used when they initially
wrote the layer. The ``KeyIndex`` itself is stored a JSON, and
critically, **(de)serialized generically**. Meaning:

.. code:: scala

    /* Instantiate as the parent trait */
    val index0: KeyIndex[SpatialKey] = new ZSpatialKeyIndex(KeyBounds(
        SpatialKey(0, 0),
        SpatialKey(9, 9)
    ))

    /* Serializes at the trait level, not the subclass */
    val json: JsValue = index0.toJson

    /* Deserialize generically */
    val index1: KeyIndex[SpatialKey] = json.convertTo[KeyIndex[SpatialKey]]

    index0 == index1  // true

Extending KeyIndex
------------------

To achieve the above, GeoTrellis has a central ``JsonFormat`` registry
for the ``KeyIndex`` subclasses. When creating a new KeyIndex type, we
need to:

0. Write the index type itself, extending ``KeyIndex``
1. Write a standard ``spray.json.JsonFormat`` for it
2. Write a *Registrator* class that registers our new Format with
   GeoTrellis

To extend ``KeyIndex``, we need to supply implementations for three
methods:

.. code:: scala

    /* Most often passed in as an argument ''val'' */
    def keyBounds: KeyBounds[K] = ???

    /* The 1-dimensional index in the SFC of a given key */
    def toIndex(key: K): Long = ???

    /* Ranges of results of `toIndex` */
    def indexRanges(keyRange: (K, K)): Seq[(Long, Long)] = ???

where ``K`` will typically be hard-coded as either ``SpatialKey`` or
``SpaceTimeKey``, unless you've defined some custom key type for your
application. ``K`` is generic in our example ``ShardingKeyIndex``, since
it holds an inner ``KeyIndex``:

.. code:: scala

    class ShardingKeyIndex[K](val inner: KeyIndex[K], val shardCount: Int) extends KeyIndex[K] { ... }

Writing and Registering a JsonFormat
------------------------------------

Supplying a ``JsonFormat`` for our new type is fairly ordinary, with a
few caveats:

.. code:: scala

    import spray.json._

    class ShardingKeyIndexFormat[K: JsonFormat: ClassTag] extends RootJsonFormat[ShardingKeyIndex[K]] {
      /* This is the foundation of the reflection-based deserialization process */
      val TYPE_NAME = "sharding"

      /* Your `write` function must follow this format, with two fields
       * `type` and `properties`. The `properties` JsObject can contain anything.
       */
      def write(index: ShardingKeyIndex[K]): JsValue = {
        JsObject(
          "type" -> JsString(TYPE_NAME),
          "properties" -> JsObject(
            "inner" -> index.inner.toJson,
            "shardCount" -> JsNumber(index.shardCount)
          )
        )
      }

      /* You should check the deserialized `typeName` matches the original */
      def read(value: JsValue): ShardingKeyIndex[K] = {
        value.asJsObject.getFields("type", "properties") match {
          case Seq(JsString(typeName), properties) if typeName == TYPE_NAME => {
            properties.asJsObject.getFields("inner", "shardCount") match {
              case Seq(inner, JsNumber(shardCount)) =>
                new ShardingKeyIndex(inner.convertTo[KeyIndex[K]], shardCount.toInt)
              case _ => throw new DeserializationException("Couldn't deserialize ShardingKeyIndex.")
            }
          }
          case _ => throw new DeserializationException("Wrong KeyIndex type: ShardingKeyIndex expected.")
        }
      }
    }

.. note::  Our ``Format`` here only has a ``K`` constraint because of our
           inner ``KeyIndex``. Yours likely won't.

Now for the final piece of the puzzle, the format Registrator. With the
above in place, it's quite simple:

.. code:: scala

    import geotrellis.spark.io.json._

    /* This class must have no arguments! */
    class ShardingKeyIndexRegistrator extends KeyIndexRegistrator {
      def register(keyIndexRegistry: KeyIndexRegistry): Unit = {
        implicit val spaceFormat = new ShardingKeyIndexFormat[SpatialKey]()
        implicit val timeFormat = new ShardingKeyIndexFormat[SpaceTimeKey]()

        keyIndexRegistry.register(
          KeyIndexFormatEntry[SpatialKey, ShardingKeyIndex[SpatialKey]](spaceFormat.TYPE_NAME)
        )
        keyIndexRegistry.register(
          KeyIndexFormatEntry[SpaceTimeKey, ShardingKeyIndex[SpaceTimeKey]](timeFormat.TYPE_NAME)
        )
      }
    }

At its simplest for an Index with a hard-coded key type, a registrator
could look like:

.. code:: scala

    class MyKeyIndexRegistrator extends KeyIndexRegistrator {
      def register(keyIndexRegistry: KeyIndexRegistry): Unit = {
        implicit val format = new MyKeyIndexFormat()

        keyIndexRegistry.register(
          KeyIndexFormatEntry[SpatialKey, MyKeyIndex](format.TYPE_NAME)
        )
      }
    }

Plugging a Registrator in
-------------------------

GeoTrellis needs to know about your new Registrator. This is done
through an ``application.conf`` in ``your-project/src/main/resources/``:

::

    // in `application.conf`
    geotrellis.spark.io.index.registrator="geotrellis.doc.examples.spark.ShardingKeyIndexRegistrator"

GeoTrellis will automatically detect the presence of this file, and use
your Registrator.

Testing
-------

Writing unit tests for your new Format is the best way to ensure you've
set up everything correctly. Tests for ``ShardingKeyIndex`` can be found
in
``doc-examples/src/test/scala/geotrellis/doc/examples/spark/ShardingKeyIndexSpec.scala``,
and can be ran in sbt with:

::

    geotrellis > project doc-examples
    doc-examples > testOnly geotrellis.doc.examples.spark.ShardingKeyIndexSpec
