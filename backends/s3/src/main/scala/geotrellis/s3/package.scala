package geotrellis.spark.io

import org.apache.spark.rdd.RDD

package object s3 {
  private[s3]
  def makePath(chunks: String*) =
    chunks.filter(_.nonEmpty).mkString("/")

  implicit class S3RDDExtensions[K,V](rdd: RDD[(K,V)]) extends SaveToS3Methods[K, V](rdd)
}
