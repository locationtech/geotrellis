package geotrellis.spark.testkit

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import org.apache.spark._

import scala.reflect.ClassTag
import spray.json._

class TestAttributeStore extends AttributeStore {
  type ReadableWritable[T] = ClassTag[T]
  private var data = Map.empty[(LayerId, String), Any]

  def read[T: ClassTag](layerId: LayerId, attributeName: String): T =
    data(layerId -> attributeName).asInstanceOf[T]

  def readAll[T: ClassTag](attributeName: String): Map[LayerId, T] =
    data
      .filter { case ((id,name), v) => name == attributeName}
      .map { case ((id, name), v) => id -> v.asInstanceOf[T]}

  def write[T: ClassTag](layerId: LayerId, attributeName: String, value: T): Unit =
    data = data updated (layerId -> attributeName, value)
}