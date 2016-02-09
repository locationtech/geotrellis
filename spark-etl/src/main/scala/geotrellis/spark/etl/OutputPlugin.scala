package geotrellis.spark.etl

import geotrellis.spark.io.{Writer, AttributeStore}
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.{Metadata, LayerId, RasterRDD}
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

trait OutputPlugin[K, V, M] extends Plugin {
  def name: String

  def attributes(props: Parameters): AttributeStore[JsonFormat]

  def writer(method: KeyIndexMethod[K], props: Parameters): Writer[LayerId, RDD[(K, V)] with Metadata[M]]

  def apply(id: LayerId, rdd: RDD[(K, V)] with Metadata[M], method: KeyIndexMethod[K], props: Map[String, String]): Unit =
    writer(method, props).write(id, rdd)

  def validate(props: Map[String, String]) =
    requireKeys(name, props, requiredKeys)

  def suitableFor(name: String): Boolean =
    name.toLowerCase == this.name
}