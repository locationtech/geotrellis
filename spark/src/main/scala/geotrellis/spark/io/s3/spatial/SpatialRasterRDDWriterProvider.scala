package geotrellis.spark.io.s3.spatial

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.spark.io._
import geotrellis.spark.io.s3._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import java.io.ByteArrayInputStream
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import geotrellis.index.zcurve.Z2
import com.typesafe.scalalogging.slf4j._

object SpatialRasterRDDWriterProvider extends RasterRDDWriterProvider[SpatialKey] with LazyLogging {
  def writer(credentialsProvider: AWSCredentialsProvider, bucket: String, layerPath: String, clobber: Boolean = true)(implicit sc: SparkContext) =
    new RasterRDDWriter[SpatialKey] {
      def write(layerId: LayerId, rdd: RasterRDD[SpatialKey]): Unit = {
        // TODO: Check if I am clobbering things        
        logger.info(s"Saving RasterRDD for $layerId to ${layerPath}")

        rdd
          .foreachPartition { iter =>
            val s3Client = new AmazonS3Client(credentialsProvider)          
            
            iter.foreach { case (key: SpatialKey, tile: Tile) =>
              val geohash = Z2(key.col, key.row).z
              val is = new ByteArrayInputStream(tile.toBytes)
              s3Client.putObject(bucket, s"$layerPath/$geohash", is, new ObjectMetadata())
            }
          }

        logger.info(s"Finished saving tiles to ${layerPath}")
      }
    }
}
