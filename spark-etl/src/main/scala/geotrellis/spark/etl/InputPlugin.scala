package geotrellis.spark.etl

import geotrellis.spark.etl.config.EtlConf

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

trait InputPlugin[I, V] extends Plugin with Serializable {
  def name: String
  def format: String

  def suitableFor(name: String, format: String): Boolean =
    (name.toLowerCase, format.toLowerCase) == (this.name, this.format)

  def apply(conf: EtlConf)(implicit sc: SparkContext): RDD[(I, V)]
}



