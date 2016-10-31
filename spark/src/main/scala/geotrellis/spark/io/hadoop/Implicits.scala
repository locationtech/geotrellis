package geotrellis.spark.io.hadoop

import org.apache.spark._
import org.apache.spark.rdd._

object Implicits extends Implicits

trait Implicits {
  implicit class HadoopSparkContextMethodsWrapper(val sc: SparkContext) extends HadoopSparkContextMethods
  implicit class withSaveBytesToHadoopMethods[K](rdd: RDD[(K, Array[Byte])]) extends SaveBytesToHadoopMethods[K](rdd)
  implicit class withSaveToHadoopMethods[K,V](rdd: RDD[(K,V)]) extends SaveToHadoopMethods[K, V](rdd)
}
