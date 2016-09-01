#geotrellis.spark.io

The underlying purpose of this package is to provide reading and writing capability for instances of `RDD[(K, V)] with Metadata[M]` into one of the distributed storage formats.

# Writing Layers

## Writing a Layer

GeoTrellis provides an abstraction for writing layers, `LayerWriter`, that the various backends implement.
There are a set of overloads that you can call when writing layers,
but generally you need to have the target `LayerId` that you will be writing to, and the `RDD[(K, V)] with Metadata[M]` that you want to write.
Note that the `K`, `V`, and `M` concrete types need to have all of the context bounds satisfied;
see the method signature in code or look to the implicit argument list in the ScalaDocs to find what the context bounds are (although if you are not using custom types, on the required imports should be necessary to satisfy these conditions).
The overloaded methods allow you to optionally specify how the key index will be created, or to supply your own `KeyIndex`.

## Key Index

A `KeyIndex` determines how your N-dimensional key (the `K` in `RDD[(K, V)] with Metadtaa[M]`) will be translated to a space filling curve index, represented by a `Long`.
It also determines how N-dimensional queries (represented by `KeyBounds` with some minimum key and maximum key) will translate to a set of ranges of `Long` index values.

There are two types of key indexes that GeoTrellis supports, which represent the two types of space filling curves supported: Z-order Curves and Hilbert Curves.
The Z-order curves can be used for 2 and 3 dimensional spaces (e.g. those represented by `SpatialKey`s or `SpaceTimeKey`s).
Hilbert curves can represent N-dimensions, although there is currently a limitation in place that requires the index to fit into a single `Long` value.

In order to index the space of an `RDD[(K, V)] with Metadata[M]`, we need to know the bounds of the space, as well as the index method to use.

The LayerWriter methods that do not take a `KeyIndex` will derive the bounds of the layer to be written by the layer itself.
This is fine if the layer elements span the entire space that the layer will ever need to write to.
If you have a larger space that represents the layer,
for instance if you want to write elements to the layer that will be outside the bounds of the original layer RDD,
you will need to create a `KeyIndex` manually that represents the entire area of the space.

For example, say we have a spatio-temporal raster layer that only contains elements that partially inhabit the date range for which we will want the layer to encompass.
We can use the `TileLayout` from the layer in combination with a date range that we know to be sufficient, and create a key index.

```scala
  import geotrellis.raster.Tile
  import geotrellis.spark._
  import geotrellis.spark.io._
  import geotrellis.spark.io.index.ZCurveKeyIndexMethod
  import geotrellis.util._
  import org.apache.spark.rdd.RDD
  import org.joda.time.DateTime

  val layer: RDD[(SpaceTimeKey, Tile)] with Metadata[TileLayerMetadata[SpaceTimeKey]] = ???

  // Create the key index with our date range
  val minDate: DateTime = new DateTime(2010, 12, 1, 0, 0)
  val maxDate: DateTime = new DateTime(2010, 12, 1, 0, 0)

  val indexKeyBounds: KeyBounds[SpaceTimeKey] = {
    val KeyBounds(minKey, maxKey) = layer.metadata.bounds.get // assuming non-empty layer
    KeyBounds(
      minKey.setComponent[TemporalKey](minDate),
      maxKey.setComponent[TemporalKey](maxDate)
    )
  }

  val keyIndex =
    ZCurveKeyIndexMethod.byMonth
      .createIndex(indexKeyBounds)

  val writer: LayerWriter[LayerId] = ???
  val layerId: LayerId = ???

  writer.write(layerId, layer, keyIndex)
```

## Reindexing a layer

If a layer was written with bounds on a key index that needs to be expanded, you can reindex that layer.
The `LayerReindexer` implementation of the backend you are using can be passed in a `KeyIndex`, which can be constructed similarly to the example above.

# Reading Layers

## Layer Readers

Layer readers read either whole or a portion of the persisted layer back into `RDD[(K, V)] with Metadata[M]`. All layer readers extend the [`FilteringLayerReader`](../../spark/src/main/scala/geotrellis/spark/io/FilteringLayerReader.scala) trait which in turn extends [`LayerReader`](../../spark/src/main/scala/geotrellis/spark/io/LayerReader.scala). The former type should be used when abstracting over the specific back-end implementation of a reader with region query support while the latter when referring to a reader that may only read the layers fully.

In order to read a layer correctly some metadata regarding the type and format of the values must be stored as well as metadata regarding layer properties. All layer readers lean on instances of [`AttributeStore`](../../spark/src/main/scala/geotrellis/spark/io/AttributeStore.scala) to provide this functionality. As a convenience each concrete type of a `LayerReader` will provide a constructor that will instantiate an `AttributeStore` of the same type with reasonable defaults. For instance `S3LayerReader` constructor, which requires S3 bucket and prefix parameters, would instantiate an `S3AttributeStore` in with the bucket and prefix.

### LayerReader

```scala
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.s3._

val reader: FilteringLayerReader[LayerId] = S3LayerReader("my-bucket", "catalog-prefix")

val rdd: RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]] =
  reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](LayerId("NLCD", 10))
```
Type signature of `rdd` variable can be inferred from the assignment and may be omitted but the type parameters for the `read` method can not be inferred and are required. Furthermore, the `reader.read` method will use these explicitly provided type parameters to find implicit type class instances that will allow it to read records of that format.

It's important to note that as a result of call to `reader.read` some IO will happen right away in order to read the layer attributes from the `AttributeStore`. However, the result of the call is an RDD, a description of the distributed collection at some point in the future. Consequently the distributed store (like HDFS or S3) will not touched until some spark "action" is called on either `rdd` or one of it's decedents.

But what happens when IO gremlins strike and the type of the record stored does not match the type parameter? It depends. The layer reader will do its best to read the layer as instructed, possibly failing. Most likely this effort will result in `org.apache.avro.AvroTypeException` if the Avro schema of the specified value does not match the schema of the stored value or a `spray.json.DeserializationException` if the JSON format of the metadata does not match the JSON value stored in the `AttributeStore`. This behavior is somewhat unhelpful but it future proofs the persisted data in so far that records may be reified into types that differ from their original implementations and names, as long as correct their formats are specified correctly for the records written.

If the type of the layer can not be assumed to be known it is possible to inspect the layer through `reader.attributeStore` field.

```scala
val header = reader.attributeStore.readHeader[LayerHeader]
assert(header.keyClass == "geotrellis.spark.SpatialKey")
assert(header.valueClass == "geotrellis.raster.Tile")
```

#### LayerReader.reader

In addition to `reader.read` there exists a `reader.reader` method defined as follows:

```scala
def reader[
  K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
  V: AvroRecordCodec: ClassTag,
  M: JsonFormat: GetComponent[?, Bounds[K]]
]: Reader[ID, RDD[(K, V)] with Metadata[M]] =
  new Reader[ID, RDD[(K, V)] with Metadata[M]] {
    def read(id: ID): RDD[(K, V)] with Metadata[M] =
      LayerReader.this.read[K, V, M](id)
  }
```

In effect we would be using a reader to produce a reader, but critically the `read` method on the constructed reader does not have any type class parameters. This is essentially a way to close over all of the formats for `K`, `V`, and `M` such that a "clean" reader can be passed to modules where those formats are not available in the implicit scope.


### FilteringLayerReader

```scala
import geotrellis.vector._
import geotrellis.spark.io._
import geotrellis.spark.io.s3._

val reader: FilteringLayerReader[LayerId] = S3LayerReader("my-bucket", "catalog-prefix")
val layerId = LayerId("NLCD", 10)

val rdd: RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]] =
  reader
    .query[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](LayerId("NLCD", 10))

    .result
```

When using the `.query` method the expected return types must still be provided just like we did when calling `.read`, however instead of producing an `RDD` it produced an instance of [`LayerQuery`](../../spark/src/main/scala/geotrellis/spark/io/LayerQuery.scala) which is essentially a query builder in a fluent style, allowing for multiple '.where' clauses to be specified. Only when `.result` is called will an `RDD` object be produced. When multiple `.where` clauses are used, the query specified their intersection.

This behavior allows us to build queries that filter on space and time independently.

```scala
import org.joda.time.DateTime

val time1: DateTime = ???
val time2: DateTime = ???

val rdd: RDD[(SpaceTimeKey, Tile)] with Metadata[TileLayerMetadata[SpaceTimeKey]] =
  reader
    .query[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](LayerId("Climate_CCSM4-RCP45-Temperature-Max", 8))
    .where(Intersects(Extent(-85.32,41.27,-80.79,43.42)))
    .where(Between(time1, time2))
    .result

```

Other query filters are supported through the  [`LayerFilter`](../../spark/src/main/scala/geotrellis/spark/io/LayerFilter.scala) type class.
Implemented instances include:

- `Contains`: Tile which contains a point
- `Between`: Tiles between two dates
- `At`: Tiles at a a specific date
- `Intersects`: Tiles intersecting ...
  - `KeyBounds`
  - `GridBounds`
  - `Extent`
  - `Polygon`


## Value Readers

Unlike layer readers, which produce a future distributed collection, an `RDD`, a tile reader for a layer is essentially a reader provider. The provided reader is able to read a single value from a specified layer.

```scala
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io.s3._

val attributeStore = S3AttributeStore("my-bucket", "catalog-prefix")
val nlcdReader: Reader[SpatialKey, Tile] = S3ValueReader[SpatialKey, Tile](attributeStore, LayerId("NLCD", 10))
val tile: Tile = nlcdReader.read(SpatialKey(1,2))
```

`ValueReader` class is very useful for creating an endpoint for a tile server because it both provides a cheap low latency access to saved tiles and does not require an instance of `SparkContext` to operate.

If you wish to abstract over the backend specific arguments but delay specification of the key and value types you may use an alternative constructor like os:

```scala
val attributeStore = S3AttributeStore("my-bucket", "catalog-prefix")
val readerProvider: ValueReader[LayerId] = S3ValueReader(attributeStore)
val nlcdReader: Reader[SpatialKey, Tile] = readerProvider.reader[SpatialKey, Tile](LayerId("NLCD", 10))
val tile: Tile = nlcdReader.read(SpatialKey(1,2))
```

The idea is similar to the `LayerReader.reader` method except in this case we're producing a reader for single tiles. Additionally it must be noted that the layer metadata is accessed during the construction of the `Reader[SpatialKey, Tile]` and saved for all future calls to read a tile.

## Readers threads

Cassandra and S3 Layer RDDReaders / RDDWriters are configurable by threads amount. It's a programm setting, that can be different for a certain machine (depends on resources available). Configuration could be set in the `reference.conf` / `application.conf` file of your app, default settings available in a `reference.conf` file of each backend subproject (we use [TypeSafe Config](https://github.com/typesafehub/config)).
For a File backend only RDDReader is configurable, For Accumulo - only RDDWriter (Socket Strategy). For all backends CollectionReaders are configurable as well.

Configuration example (defaut means to use all processors available to the Java virtual machine):

```conf
geotrellis.accumulo.threads {
  collection.read = default
  rdd.write       = default
}
geotrellis.file.threads {
  collection.read = default
  rdd.read        = default
}
geotrellis.cassandra.threads {
  collection.read = default
  rdd {
    write = default
    read  = default
  }
}
geotrellis.s3.threads {
  collection.read = default
  rdd {
    write = default
    read  = default
  }
}
```

```conf
geotrellis.accumulo.threads {
  collection.read = 32
  rdd.write       = 32
}
geotrellis.file.threads {
  collection.read = 32
  rdd.read        = 32
}
geotrellis.cassandra.threads {
  collection.read = 32
  rdd {
    write = 32
    read  = 32
  }
}
geotrellis.s3.threads {
  collection.read = 32
  rdd {
    write = 32
    read  = 32
  }
}
```
