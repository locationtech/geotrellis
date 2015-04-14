package geotrellis.spark.io.s3.spatial

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.spark.io._
import geotrellis.spark.io.s3._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import java.io.ByteArrayInputStream
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.auth.{AWSCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import geotrellis.index.zcurve.Z2
import com.typesafe.scalalogging.slf4j._
import scala.collection.mutable.ArrayBuffer
import com.amazonaws.services.s3.model.AmazonS3Exception


object SpatialRasterRDDWriterProvider extends RasterRDDWriterProvider[SpatialKey] with LazyLogging {
  def writer(credentialsProvider: AWSCredentialsProvider, bucket: String, layerPath: String, clobber: Boolean = true)(implicit sc: SparkContext) =
    new RasterRDDWriter[SpatialKey] {
      def write(layerId: LayerId, rdd: RasterRDD[SpatialKey]): Unit = {
        // TODO: Check if I am clobbering things        
        logger.info(s"Saving RasterRDD for $layerId to ${layerPath}")

        val bcCredentials = sc.broadcast(credentialsProvider.getCredentials)
        val catalogBucket = bucket
        val path = layerPath
        
        rdd
          .foreachPartition { partition =>
            val s3client = new AmazonS3Client(bcCredentials.value);

            val requests = partition.map{ case (key: SpatialKey, tile: Tile) =>
              val index = Z2(key.col, key.row).z
              val bytes = tile.toBytes
              val metadata = new ObjectMetadata()
              metadata.setContentLength(bytes.length);              
              val is = new ByteArrayInputStream(bytes)
              new PutObjectRequest(catalogBucket, f"$path/${index}%019d", is, metadata)
            }

            requests.foreach{ r =>
              var backoff = 0;

              do {
                try {
                  if (backoff > 0) Thread.sleep(backoff)
                  s3client.putObject(r)
                  backoff = 0
                } catch {
                  case e: AmazonS3Exception =>
                    backoff = math.max(5, backoff*2)
                }
              } while (backoff > 0)
            }
          }

        logger.info(s"Finished saving tiles to ${layerPath}")
      }
    }
}
