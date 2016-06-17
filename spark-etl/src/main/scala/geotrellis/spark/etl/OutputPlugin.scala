package geotrellis.spark.etl

import geotrellis.spark.etl.config.backend.Backend
import geotrellis.spark.{LayerId, Metadata}
import geotrellis.spark.io.{AttributeStore, Writer}
import geotrellis.spark.io.index.KeyIndexMethod

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

trait OutputPlugin[K, V, M] extends Plugin {
  def name: String

  def attributes(props: Parameters, credentials: Option[Backend]): AttributeStore

  def writer(method: KeyIndexMethod[K], props: Parameters, credentials: Option[Backend])(implicit sc: SparkContext): Writer[LayerId, RDD[(K, V)] with Metadata[M]]

  def apply(id: LayerId, rdd: RDD[(K, V)] with Metadata[M], method: KeyIndexMethod[K], props: Map[String, String], credentials: Option[Backend]): Unit = {
    implicit val sc = rdd.sparkContext
    writer(method, props, credentials).write(id, rdd)
  }

  def validate(props: Map[String, String]) =
    requireKeys(name, props, requiredKeys)

  def suitableFor(name: String): Boolean =
    name.toLowerCase == this.name
}
