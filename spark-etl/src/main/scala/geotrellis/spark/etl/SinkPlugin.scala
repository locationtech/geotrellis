package geotrellis.spark.etl

import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.{LayerId, RasterRDD}
import scala.reflect.ClassTag

trait SinkPlugin {
  def name: String
  def key: ClassTag[_]
  def requiredKeys: Array[String]

  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]): Unit

  def validate(props: Map[String, String]) =
    requireKeys(name, props, requiredKeys)

  def suitableFor(name: String, keyClassTag: ClassTag[_]): Boolean =
    (name.toLowerCase, keyClassTag) == (this.name, this.key)
}