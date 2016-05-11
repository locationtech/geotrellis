package geotrellis.spark.io

import geotrellis.spark._

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.json._
import geotrellis.spark.merge._
import geotrellis.util._

import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect.ClassTag

abstract class LayerUpdater[ID] {
  protected def _update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: ID, rdd: RDD[(K, V)] with Metadata[M], keyBounds: KeyBounds[K], mergeFunc: (V, V) => V): Unit

  protected def schemaHasChanged[K: AvroRecordCodec, V: AvroRecordCodec](writerSchema: Schema): Boolean = {
    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema
    !schema.fingerprintMatches(writerSchema)
  }

  def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: ID, rdd: RDD[(K, V)] with Metadata[M], mergeFunc: (V, V) => V): Unit =
    rdd.metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        _update(id, rdd, keyBounds, mergeFunc)
      case EmptyBounds =>
        throw new EmptyBoundsError(s"Cannot update layer $id with a layer with empty bounds.")
    }

  def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: ID, rdd: RDD[(K, V)] with Metadata[M]): Unit =
    rdd.metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        // By default, we want the updating tile to replace the existing tile.
        val mergeFunc: (V, V) => V = { (existing, updating) => updating }
        _update(id, rdd, keyBounds, mergeFunc)
      case EmptyBounds =>
        throw new EmptyBoundsError(s"Cannot update layer $id with a layer with empty bounds.")
    }
}
