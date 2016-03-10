package geotrellis.spark.io.s3

import geotrellis.spark.render._
import geotrellis.spark.{LayerId, GridKey}

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import java.net.URI

import com.amazonaws.services.s3.model.{PutObjectRequest, PutObjectResult, ObjectMetadata}
import com.amazonaws.services.s3.model.AmazonS3Exception

import org.apache.spark.rdd.RDD

import scalaz.stream._
import scalaz.concurrent.Task


object SaveToS3Methods {
  /**
    * @param id           A Layer ID
    * @param pathTemplate The template used to convert a Layer ID and a GridKey into an S3 URI
    * @return             A functon which takes a spatial key and returns an S3 URI
    */
  def spatialKeyToPath(id: LayerId, pathTemplate: String): (GridKey => String) = {
    // Return λ
    { key =>
      pathTemplate
        .replace("{x}", key.col.toString)
        .replace("{y}", key.row.toString)
        .replace("{z}", id.zoom.toString)
        .replace("{name}", id.name)
    }
  }

  /**
    * @param bucket    name of the S3 bucket
    * @param keyToPath maps each key to full path in the bucket
    * @param rdd       An RDD of K, Byte-Array pairs (where the byte-arrays contains image data) to send to S3
    * @param s3Maker   A function which returns an S3 Client (real or mock) into-which to save the data
    */
  def apply[K](
    bucket: String,
    keyToPath: K => String,
    rdd: RDD[(K, Array[Byte])],
    s3Maker: () => S3Client
  ): Unit = {
    rdd.persist() .foreachPartition { partition =>
      val s3Client = s3Maker()
      val requests: Process[Task, PutObjectRequest] =
        Process.unfold(partition) { iter =>
          if (iter.hasNext) {
            val (key, bytes) = iter.next()
            val metadata = new ObjectMetadata()
            metadata.setContentLength(bytes.length)
            val is = new ByteArrayInputStream(bytes)
            val path = keyToPath(key)
            val request = new PutObjectRequest(bucket, path, is, metadata)
            Some(request, iter)
          } else {
            None
          }
        }

      val pool = Executors.newFixedThreadPool(8)

      import geotrellis.spark.util.TaskUtils._
      val write: PutObjectRequest => Process[Task, PutObjectResult] = { request =>
        Process eval Task {
          request.getInputStream.reset() // reset in case of retransmission to avoid 400 error
          s3Client.putObject(request)
        }(pool).retryEBO {
          case e: AmazonS3Exception if e.getStatusCode == 503 => true
          case _ => false
        }
      }

      val results = nondeterminism.njoin(maxOpen = 8, maxQueued = 8) {
        requests map write
      }

      results.run.run
      pool.shutdown()
    }
  }
}

class SaveToS3Methods[K](rdd: RDD[(K, Array[Byte])]) {
  /**
    * @param keyToPath A function from K (a key) to an S3 URI
    * @param s3Client  An S3 Client (real or mock) into-which to save the data
    */
  def saveToS3(keyToPath: K => String): Unit = {
    val bucket = new URI(keyToPath(rdd.first._1)).getAuthority

    SaveToS3Methods(bucket, keyToPath, rdd, { () => S3Client.default })
  }
}
