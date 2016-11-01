package geotrellis.spark.etl

import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.{LayerId, Metadata}
import geotrellis.spark.io.{AttributeStore, Writer}

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

trait OutputPlugin[K, V, M] extends Plugin {
  import Etl.PostSaveHook

  def name: String

  def attributes(conf: EtlConf): AttributeStore

  def writer(conf: EtlConf)(implicit sc: SparkContext): Writer[LayerId, RDD[(K, V)] with Metadata[M]]

  def apply(
    id: LayerId,
    rdd: RDD[(K, V)] with Metadata[M],
    conf: EtlConf,
    postSave: PostSaveHook[K, V, M] = PostSaveHook.EMPTY[K, V, M]
  ): Unit = {
    implicit val sc = rdd.sparkContext
    val w = writer(conf)
    w.write(id, rdd)
    postSave(attributes(conf), w, id, rdd)
  }

  def suitableFor(name: String): Boolean =
    name.toLowerCase == this.name
}
