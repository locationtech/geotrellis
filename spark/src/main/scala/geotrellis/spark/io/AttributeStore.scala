package geotrellis.spark.io

import geotrellis.spark._

trait AttributeStore extends AttributeCaching {
  type ReadableWritable[T]

  def read[T: ReadableWritable](layerId: LayerId, attributeName: String): T
  def readAll[T: ReadableWritable](attributeName: String): Map[LayerId, T]
  def write[T: ReadableWritable](layerId: LayerId, attributeName: String, value: T): Unit
  def layerExists(layerId: LayerId): Boolean
}

object AttributeStore {
  type Aux[F[_]] = AttributeStore {
    type ReadableWritable[T] = F[T]
  }

  object Fields {
    val layerMetaData = "metadata"
    val keyBounds = "keyBounds"
    val keyIndex = "keyIndex"
    val rddMetadata = "rddMetadata"
  }
}