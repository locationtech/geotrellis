package geotrellis.spark.io.s3

import java.io.ByteArrayInputStream
import com.amazonaws.services.s3.model.{PutObjectRequest, PutObjectResult, ObjectMetadata}
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.spark.rdd.RDD
import java.util.concurrent.Executors
import scalaz.stream._
import scalaz.concurrent.Task

class SaveToS3Methods[K, V](rdd: RDD[(K, V)]) {
  /**
   * @param bucket    name of the S3 bucket
   * @param keyToPath maps each key to full path in the bucket
   * @param asBytes   K and V both provided in case K contains required information, like extent.
   */
  def saveToS3(bucket: String, keyToPath: K => String, asBytes: (K,V) => Array[Byte]): Unit = {
    rdd.persist() .foreachPartition { partition =>
      val s3Client = S3Client.default

      val requests: Process[Task, PutObjectRequest] =
        Process.unfold(partition) { iter =>
          if (iter.hasNext) {
            val (key, value) = iter.next()
            val bytes = asBytes(key, value)
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

      import geotrellis.spark.utils.TaskUtils._
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
