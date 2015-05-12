package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io.index._
import geotrellis.raster._
import geotrellis.spark.utils._
import org.apache.spark.SparkContext
import scala.collection.JavaConversions._
import com.amazonaws.services.s3.model.AmazonS3Exception

trait TileReader[K] {
  val encodeKey: (K, KeyIndex[K], Int) => String 

  def read(
    client: S3Client,
    layerId: LayerId,
    lmd: S3LayerMetaData,
    index: KeyIndex[K],
    keyBounds: KeyBounds[K])
  (key: K): Tile = {

    val maxLen = { // lets find out the widest key we can possibly have
      def digits(x: Long): Int = if (x < 10) 1 else 1 + digits(x/10)
      digits(index.toIndex(keyBounds.maxKey))
    }
    val path = s"${lmd.key}/${encodeKey(key, index, maxLen)}"

    val is = 
    try {
      client.getObject(lmd.bucket, path).getObjectContent    
    } catch {
      case e: AmazonS3Exception if e.getStatusCode == 404 =>
        sys.error(s"Tile with key $key not found for layer $layerId")
    }

    val (storedKey, tile) = KryoSerializer.deserializeStream[(K, Tile)](is)

    require(storedKey == key, "They key requested must match the key retreived.")    

    tile
  }
}
