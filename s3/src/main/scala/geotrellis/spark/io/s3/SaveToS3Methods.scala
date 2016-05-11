package geotrellis.spark.io.s3

import geotrellis.util.MethodExtensions

import com.amazonaws.services.s3.model.PutObjectRequest
import org.apache.spark.rdd.RDD


class SaveToS3Methods[K](val self: RDD[(K, Array[Byte])]) extends MethodExtensions[RDD[(K, Array[Byte])]] {

  /**
    * Saves each RDD value to an S3 key.
    *
    * @param keyToUri A function from K (a key) to an S3 URI
    * @param putObjectModifier  Function that will be applied ot S3 PutObjectRequests, so that they can be modified (e.g. to change the ACL settings)
    */
  def saveToS3(keyToUri: K => String, putObjectModifier: PutObjectRequest => PutObjectRequest = { p => p }): Unit =
    SaveToS3(self, keyToUri, putObjectModifier)
}
