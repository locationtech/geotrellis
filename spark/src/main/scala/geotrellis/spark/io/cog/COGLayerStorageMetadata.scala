package geotrellis.spark.io.cog

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._

import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect._

case class COGLayerStorageMetadata[K](metadata: COGLayerMetadata[K], keyIndexes: Map[ZoomRange, KeyIndex[K]]) {
  def combine(other: COGLayerStorageMetadata[K])(implicit ev: Boundable[K]): COGLayerStorageMetadata[K] =
    COGLayerStorageMetadata(metadata.combine(other.metadata), other.keyIndexes)
}

object COGLayerStorageMetadata {
  implicit def cogLayerStorageMetadataFormat[K: SpatialComponent: JsonFormat: ClassTag] =
    new RootJsonFormat[COGLayerStorageMetadata[K]] {
      def write(sm: COGLayerStorageMetadata[K]) =
        JsObject(
          "metadata" -> sm.metadata.toJson,
          "keyIndexes" -> JsArray(sm.keyIndexes.map(_.toJson).toVector)
        )

      def read(value: JsValue): COGLayerStorageMetadata[K] =
        value.asJsObject.getFields("metadata", "keyIndexes") match {
          case Seq(metadata, JsArray(keyIndexes)) =>
            COGLayerStorageMetadata(
              metadata.convertTo[COGLayerMetadata[K]],
              keyIndexes.map(_.convertTo[(ZoomRange, KeyIndex[K])]).toMap
            )
          case v =>
            throw new DeserializationException(s"COGLayerStorageMetadata expected, got: $v")
        }
    }
}
