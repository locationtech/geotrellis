Writing a Custom `KeyIndex`
===========================

Want to dive right into code? See:
`doc-examples/src/main/scala/geotrellis/doc/examples/ShardingKeyIndex.scala`

### The `KeyIndex` trait

The `KeyIndex` trait is high-level representation of Space Filling Curves,
and for us it is critical to Tile layer input/output. As of GeoTrellis
`0.10.2`, its subclasses are:

- `ZSpatialKeyIndex`
- `ZSpaceTimeKeyIndex`
- `HilbertSpatialKeyIndex`
- `HilbertSpaceTimeKeyIndex`
- `RowMajorSpatialKeyIndex`

While the subclass constructors can be used directly when creating an index,
we always reference them generically elsewhere as `KeyIndex`. For instance,
when we write an `RDD`, we need to supply a generic `KeyIndex`:

```scala
S3LayerWriter.write[K, V, M]: (LayerId, RDD[(K, V)] with Metadata[M], KeyIndex[K]) => Unit
```

but when we read or update, we don't:

```scala
S3LayerReader.read[K, V, M]: LayerId => RDD[(K, V)] with Metadata[M]

S3LayerUpdater.update[K, V, M]: (LayerId, RDD[(K, V)] with Metadata[M]) => Unit
```

Luckily for the end user of GeoTrellis, this means they don't need to keep
track of which `KeyIndex` subclass they used when they initially wrote the
layer. The `KeyIndex` itself is stored a JSON, and critically,
**(de)serialized generically**. Meaning:

```scala
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
```

### Extending `KeyIndex`

To achieve the above, GeoTrellis has a central `JsonFormat` registry for the `KeyIndex`
subclasses. When creating a new KeyIndex type, we need to:

0. Write the index type itself, extending `KeyIndex`
1. Write a standard `spray.json.JsonFormat` for it
2. Write a *Registrator* class that registers our new Format with GeoTrellis

A full example of this can be found at
`doc-examples/src/main/scala/geotrellis/doc/examples/ShardingKeyIndex.scala`.

To extend `KeyIndex`, we need to supply implementations for three methods:

```scala
/* Most often passed in as an argument ''val'' */
def keyBounds: KeyBounds[K] = ???

/* The 1-dimensional index in the SFC of a given key */
def toIndex(key: K): Long = ???

/* Ranges of results of `toIndex` */
def indexRanges(keyRange: (K, K)): Seq[(Long, Long)] = ???
```

where `K` will typically be hard-coded as either `SpatialKey` or
`SpaceTimeKey`, unless you've defined some custom key type for your
application. `K` is generic in our example `ShardingKeyIndex`, since it
holds an inner `KeyIndex`:

```scala
class ShardingKeyIndex[K](val inner: KeyIndex[K], val shardCount: Int) extends KeyIndex[K] { ... }
```

### Writing and Registering a `JsonFormat`

Supplying a `JsonFormat` for our new type is fairly ordinary, with a few
caveats:

```scala
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
```

**Note:** Our `Format` here only has a `K` constraint because of our inner
`KeyIndex`. Yours likely won't.

Now for the final piece of the puzzle, the format Registrator. With the
above in place, it's quite simple:

```scala
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
```

At its simplest for an Index with a hard-coded key type, a registrator could look like:

```scala
class MyKeyIndexRegistrator extends KeyIndexRegistrator {
  def register(keyIndexRegistry: KeyIndexRegistry): Unit = {
    implicit val format = new MyKeyIndexFormat()

    keyIndexRegistry.register(
      KeyIndexFormatEntry[SpatialKey, MyKeyIndex](format.TYPE_NAME)
    )
  }
}
```

### Plugging a Registrator in

GeoTrellis needs to know about your new Registrator. This is done through an
`application.conf` in `your-project/src/main/resources/`:

```
// in `application.conf`
geotrellis.spark.io.index.registrator="geotrellis.doc.examples.spark.ShardingKeyIndexRegistrator"
```

GeoTrellis will automatically detect the presence of this file, and use your
Registrator.

### Testing

Writing unit tests for your new Format is the best way to ensure you've set
up everything correctly. Tests for `ShardingKeyIndex` can be found in
`doc-examples/src/test/scala/geotrellis/doc/examples/spark/ShardingKeyIndexSpec.scala`,
and can be ran in sbt with:
```
geotrellis > project doc-examples
doc-examples > testOnly geotrellis.doc.examples.spark.ShardingKeyIndexSpec
```
