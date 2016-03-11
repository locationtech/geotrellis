package geotrellis.spark

import org.apache.spark.rdd._

import scala.reflect._

class ContextRDDMethods[K: ClassTag, V: ClassTag, M](val rdd: RDD[(K, V)] with Metadata[M]) extends Serializable {
  def metadata = rdd.metadata
}




