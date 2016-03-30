package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.json._
import geotrellis.util._

import spray.json._

import scala.reflect.ClassTag

class GenericLayerMover[ID](layerCopier: LayerCopier[ID], layerDeleter: LayerDeleter[ID]) extends LayerMover[ID] {
  def move[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](from: ID, to: ID): Unit = {
    layerCopier.copy[K, V, M](from, to)
    layerDeleter.delete(from)
  }
}

object GenericLayerMover {
  def apply[ID](layerCopier: LayerCopier[ID], layerDeleter: LayerDeleter[ID]) =
    new GenericLayerMover(layerCopier, layerDeleter)
}
