package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.spark.io._
import geotrellis.spark.utils.KryoSerializer
import geotrellis.spark.io.index._
import geotrellis.spark.io.s3._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import java.io.ByteArrayInputStream
import com.amazonaws.services.s3.model.{PutObjectRequest, PutObjectResult}
import com.amazonaws.auth.{AWSCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.s3.model.ObjectMetadata
import geotrellis.index.zcurve.Z2
import com.typesafe.scalalogging.slf4j._
import scala.collection.mutable.ArrayBuffer
import com.amazonaws.services.s3.model.AmazonS3Exception
import scala.reflect.ClassTag

class RasterRDDWriter[K: ClassTag] extends LazyLogging {
  def write(
    s3client: ()=>S3Client, 
    bucket: String, 
    layerPath: String,
    keyIndex: KeyIndex[K],
    clobber: Boolean)
  (layerId: LayerId, rdd: RasterRDD[K])
  (implicit sc: SparkContext): Unit = {
    // TODO: Check if I am clobbering things        
    logger.info(s"Saving RasterRDD for $layerId to ${layerPath}")
    
    val bcClient = sc.broadcast(s3client)
    val catalogBucket = bucket
    val path = layerPath
    
    rdd
      .foreachPartition { partition =>
        val s3client: S3Client = bcClient.value.apply

        val requests = partition.map{ row =>
          val index = keyIndex.toIndex(row._1) 
          val bytes = KryoSerializer.serialize[(K, Tile)](row)
          val metadata = new ObjectMetadata()
          metadata.setContentLength(bytes.length);              
          val is = new ByteArrayInputStream(bytes)
          new PutObjectRequest(catalogBucket, f"$path/${index}%019d", is, metadata)
        }

        requests.foreach{ r =>
          s3client.putObjectWithBackoff(r)
        }
      }

    logger.info(s"Finished saving tiles to ${layerPath}")
  }   
}