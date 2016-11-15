package geotrellis.spark.io

import org.apache.spark.rdd.RDD

package object s3 {
  private[s3]
  def makePath(chunks: String*) =
    chunks.filter(_.nonEmpty).mkString("/")

  implicit class withSaveToS3Methods[K](rdd: RDD[(K, Array[Byte])]) extends SaveToS3Methods(rdd)
}
