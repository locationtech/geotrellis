package geotrellis.spark.io

import geotrellis.spark._

trait AttributeCatalog {
  type ReadableWritable[T]

  def load[T: ReadableWritable](layerId: LayerId, attributeName: String): T
  def save[T: ReadableWritable](layerId: LayerId, attributeName: String, value: T): Unit
}
