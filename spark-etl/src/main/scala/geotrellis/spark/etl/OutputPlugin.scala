package geotrellis.spark.etl

import geotrellis.spark.{LayerId, Metadata}
import geotrellis.spark.io.{AttributeStore, Writer}

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

trait OutputPlugin[K, V, M] extends Plugin {
  def name: String

  def attributes(job: EtlJob): AttributeStore

  def writer(job: EtlJob)(implicit sc: SparkContext): Writer[LayerId, RDD[(K, V)] with Metadata[M]]

  def apply(id: LayerId, rdd: RDD[(K, V)] with Metadata[M], job: EtlJob): Unit = {
    implicit val sc = rdd.sparkContext
    writer(job).write(id, rdd)
  }

  def suitableFor(name: String): Boolean =
    name.toLowerCase == this.name
}
