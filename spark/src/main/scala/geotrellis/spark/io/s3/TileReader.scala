package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.{KeyValueRecordCodec, AvroEncoder, AvroRecordCodec}
import geotrellis.spark.io.index._
import geotrellis.raster._
import com.amazonaws.services.s3.model.AmazonS3Exception

class TileReader[K: AvroRecordCodec] {
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
    val path = s"${lmd.key}/${encodeIndex(index.toIndex(key), maxLen)}"

    val is = 
    try {
      client.getObject(lmd.bucket, path).getObjectContent    
    } catch {
      case e: AmazonS3Exception if e.getStatusCode == 404 =>
        throw new TileNotFoundError(key, layerId)        
    }

    val bytes = org.apache.commons.io.IOUtils.toByteArray(is)
    val recs = AvroEncoder.fromBinary(bytes)(KeyValueRecordCodec[K, Tile])

    recs
      .find( row => row._1 == key )
      .map { row => row._2 }
      .get
  }
}
