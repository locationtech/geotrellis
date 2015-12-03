package geotrellis.spark.io.slippy

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.io.Filesystem
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.io.s3._

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter._
import org.apache.spark._
import org.apache.spark.rdd._
import java.io.File

class S3SlippyTileReader[T](uri: String)(fromBytes: (SpatialKey, Array[Byte]) => T) extends SlippyTileReader[T] {
  import SlippyTileReader.TilePath

  val client = S3Client.default
  val parsed = new java.net.URI(uri)
  val bucket = parsed.getHost
  val prefix = {
    val path = parsed.getPath
    path.substring(1, path.length)
  }

  def read(zoom: Int, key: SpatialKey): T = {
    val s3key = new File(prefix, s"$zoom/${key.col}/${key.row}").getPath

    S3Client.default.listKeys(bucket, s3key) match {
      case Seq() => sys.error(s"KeyNotFound: $s3key not found in bucket $bucket")
      case Seq(tileKey) => fromBytes(key, client.readBytes(bucket, tileKey))
      case _ => sys.error(s"Multiple keys found for prefix $s3key in bucket $bucket")
    }
  }

  def read(zoom: Int)(implicit sc: SparkContext): RDD[(SpatialKey, T)] = {
    val keys = {
      client.listKeys(bucket, prefix)
        .map { key =>
          key match {
            case TilePath(z, x, y) if z.toInt == zoom => Some((SpatialKey(x.toInt, y.toInt), key))
            case _ => None
          }
        }
        .flatten
        .toSeq
    }

    val numPartitions = math.min(keys.size, math.max(keys.size / 10, 50)).toInt
    sc.parallelize(keys)
      .partitionBy(new HashPartitioner(numPartitions))
      .mapPartitions({ partition =>
        val client = S3Client.default

        partition.map { case (spatialKey, s3Key) =>

          (spatialKey, fromBytes(spatialKey, client.readBytes(bucket, s3Key)))
        }
      }, preservesPartitioning = true)
  }
}
