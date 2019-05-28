package geotrellis.spark.store.s3

import org.apache.spark.rdd.RDD


object Implicits extends Implicits

trait Implicits {
  implicit class withSaveToS3Methods[K](rdd: RDD[(K, Array[Byte])]) extends SaveToS3Methods(rdd)
}
