package geotrellis.spark.io

import org.apache.spark.rdd.RDD

package object s3 {
  private[s3]
  def makePath(chunks: String*) =
    chunks.filter(_.nonEmpty).mkString("/")

  implicit class withSaveToS3Methods[K](rdd: RDD[(K, Array[Byte])]) {
    /**
      * Saves each RDD value to an S3 key.
      *
      * @param keyToPath A function from K (a key) to an S3 URI
      */
    def saveToS3(keyToPath: K => String): Unit = {
      SaveToS3(keyToPath, rdd, { () => S3Client.default })
    }
  }
}
