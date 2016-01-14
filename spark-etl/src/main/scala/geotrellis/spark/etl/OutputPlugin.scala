package geotrellis.spark.etl

import geotrellis.spark.io.{Writer, AttributeStore}
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.{LayerId, RasterRDD}
import spray.json.JsonFormat

trait OutputPlugin[K] {
  type Parameters = Map[String, String]
  def name: String
  def requiredKeys: Array[String]

  def attributes(props: Parameters): AttributeStore[JsonFormat]

  def writer(props: Parameters): Writer[LayerId, K, RasterRDD[K]]

  def apply(id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]): Unit =
    writer(props).write(id, rdd, method)

  def validate(props: Map[String, String]) =
    requireKeys(name, props, requiredKeys)

  def suitableFor(name: String): Boolean =
    name.toLowerCase == this.name
}