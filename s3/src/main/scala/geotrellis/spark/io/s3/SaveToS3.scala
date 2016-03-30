package geotrellis.spark.io.s3

import geotrellis.spark.render._
import geotrellis.spark.{LayerId, SpatialKey}

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import java.net.URI

import com.amazonaws.services.s3.model.{PutObjectRequest, PutObjectResult, ObjectMetadata}
import com.amazonaws.services.s3.model.AmazonS3Exception

import org.apache.spark.rdd.RDD

import scalaz.stream._
import scalaz.concurrent.Task


object SaveToS3 {
  /**
    * @param id           A Layer ID
    * @param pathTemplate The template used to convert a Layer ID and a SpatialKey into an S3 URI
    * @return             A functon which takes a spatial key and returns an S3 URI
    */
  def spatialKeyToPath(id: LayerId, pathTemplate: String): (SpatialKey => String) = {
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
    * @param keyToUri  A function that maps each key to full s3 uri
    * @param rdd       An RDD of K, Byte-Array pairs (where the byte-arrays contains image data) to send to S3
    * @param putObjectModifier  Function that will be applied ot S3 PutObjectRequests, so that they can be modified (e.g. to change the ACL settings)
    * @param s3Maker   A function which returns an S3 Client (real or mock) into-which to save the data
    */
  def apply[K](
    rdd: RDD[(K, Array[Byte])],
    keyToUri: K => String,
    putObjectModifier: PutObjectRequest => PutObjectRequest = { p => p },
    s3Maker: () => S3Client = () => S3Client.default
  ): Unit = {
    val keyToPrefix: K => (String, String) = key => {
      val uri = new URI(keyToUri(key))
      require(uri.getScheme == "s3", s"SaveToS3 only supports s3 scheme: $uri")
      val bucket = uri.getAuthority
      val prefix = uri.getPath.substring(1) // drop the leading / from the prefix
      (bucket, prefix)
    }

    rdd.foreachPartition { partition =>
      val s3Client = s3Maker()
      val requests: Process[Task, PutObjectRequest] =
        Process.unfold(partition) { iter =>
          if (iter.hasNext) {
            val (key, bytes) = iter.next()
            val metadata = new ObjectMetadata()
            metadata.setContentLength(bytes.length)
            val is = new ByteArrayInputStream(bytes)
            val (bucket, path) = keyToPrefix(key)
            val request = putObjectModifier(new PutObjectRequest(bucket, path, is, metadata))
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

      results.run.unsafePerformSync
      pool.shutdown()
    }
  }
}
