package geotrellis.spark.io.s3

import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.util._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.s3.util.S3BytesStreamer

import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import com.typesafe.config.ConfigFactory

import com.amazonaws.services.s3.model._

object GeoTiffRDD {
  import GeoTiffReaderExtensions.Reader

  def apply[K, T: Reader](
    bucket: String,
    prefix: String,
    maxTileDimensions: Option[(Int, Int)])
    (f: (String, String, Reader[T]) => K)
    (implicit s3Client: S3Client, sc: SparkContext): RDD[(K, T)] =
    maxTileDimensions match {
      case None =>
        sc.parallelize(s3Client.listKeys(bucket, prefix))
          .map { value =>
            val byteStream = S3BytesStreamer(bucket, value, s3Client)
            val reader = implicitly[Reader[T]]
            val geoTiff = reader.read(bytesStream)
            val key = f(bucket, value, geoTiff)
            (key, geoTiff.tile)
          }

      case Some(md) => 
        sc.parallelize(s3Client.listKeys(bucket, prefix))
          .map { value =>
            val byteStream = S3BytesStreamer(bucket, value, s3Client)
            val reader = implicitly[Reader[T]]
            reader.read(bytesStream, md)
              .flatMap { newKey =>
                while(newKey.hasNext) {
                  val geoTiff = newKey.next
                  val key = f(bucket, _, geoTiff)
                  (key, geoTiff.tile)
                }
              }
          }
    }
}
