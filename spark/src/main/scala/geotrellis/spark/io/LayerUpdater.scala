package geotrellis.spark.io

import geotrellis.spark._

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.json._

import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect.ClassTag

abstract class LayerUpdater[ID] {
  protected def _update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, rdd: RDD[(K, V)] with Metadata[M], keyBounds: KeyBounds[K]): Unit

  protected def schemaHasChanged[K: AvroRecordCodec, V: AvroRecordCodec](writerSchema: Schema): Boolean = {
    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema
    !schema.fingerprintMatches(writerSchema)
  }

  def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, rdd: RDD[(K, V)] with Metadata[M]): Unit =
    rdd.metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        _update(id, rdd, keyBounds)
      case EmptyBounds =>
        throw new EmptyBoundsError(s"Cannot update layer $id with a layer with empty bounds.")
    }

  def mergeUpdate[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ] (id: ID, reader: FilteringLayerReader[ID], rdd: RDD[(K, V)] with Metadata[M])
    (merge: (RDD[(K, V)] with Metadata[M], RDD[(K, V)] with Metadata[M]) => RDD[(K, V)] with Metadata[M]) =
    rdd.metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        val existing =
          reader
            .query[K, V, M](id)
            .where(Intersects(keyBounds))
            .toRDD
        update(id, merge(existing, rdd))
      case EmptyBounds =>
        throw new EmptyBoundsError(s"Cannot update layer $id with a layer with empty bounds.")
    }
}
