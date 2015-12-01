package geotrellis.spark.io

import geotrellis.spark._

trait AttributeStore[F[_]] extends AttributeCaching[F] {
  type Format[T] = F[T]

  def read[T: Format](layerId: LayerId, attributeName: String): T
  def readAll[T: Format](attributeName: String): Map[LayerId, T]
  def write[T: Format](layerId: LayerId, attributeName: String, value: T): Unit
  def layerExists(layerId: LayerId): Boolean
  def delete(layerId: Option[LayerId], attributeName: Option[String]): Unit
  def delete(layerId: LayerId, attributeName: String): Unit =
    delete(Some(layerId), Some(attributeName))
  def delete(layerId: LayerId): Unit =
    delete(Some(layerId), None)
  def delete(attributeName: String): Unit =
    delete(None, Some(attributeName))
}

object AttributeStore {
  object Fields {
    val header = "header"
    val keyBounds = "keyBounds"
    val keyIndex = "keyIndex"
    val metaData = "metadata"
    val schema = "schema"
  }
}