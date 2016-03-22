package geotrellis.spark.io.s3

import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD


class SaveToS3Methods[K](val self: RDD[(K, Array[Byte])]) extends MethodExtensions[RDD[(K, Array[Byte])]] {

  /**
    * Saves each RDD value to an S3 key.
    *
    * @param keyToPath A function from K (a key) to an S3 URI
    */
  def saveToS3(keyToPath: K => String): Unit = {
    SaveToS3(keyToPath, self, { () => S3Client.default })
  }
}
