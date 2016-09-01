package geotrellis.spark.io.s3

import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec

import com.amazonaws.services.s3.model.{AmazonS3Exception, ObjectMetadata, PutObjectRequest, PutObjectResult}
import org.apache.spark.rdd.RDD
import com.typesafe.config.ConfigFactory
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors

import scala.reflect._

trait S3RDDWriter {

  def getS3Client: () => S3Client

  def write[K: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    rdd: RDD[(K, V)],
    bucket: String,
    keyPath: K => String,
    putObjectModifier: PutObjectRequest => PutObjectRequest = { p => p },
    threads: Int = ConfigFactory.load().getThreads("geotrellis.s3.threads.rdd.write")
  ): Unit = {
    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    implicit val sc = rdd.sparkContext

    val _getS3Client = getS3Client
    val _codec = codec

    val pathsToTiles =
      // Call groupBy with numPartitions; if called without that argument or a partitioner,
      // groupBy will reuse the partitioner on the parent RDD if it is set, which could be typed
      // on a key type that may no longer by valid for the key type of the resulting RDD.
      rdd.groupBy({ row => keyPath(row._1) }, numPartitions = rdd.partitions.length)

    pathsToTiles.foreachPartition { partition =>
      import geotrellis.spark.util.TaskUtils._
      val getS3Client = _getS3Client
      val s3client: S3Client = getS3Client()

      val requests: Process[Task, PutObjectRequest] =
        Process.unfold(partition){ iter =>
          if (iter.hasNext) {
            val recs = iter.next()
            val key = recs._1
            val pairs = recs._2.toVector
            val bytes = AvroEncoder.toBinary(pairs)(_codec)
            val metadata = new ObjectMetadata()
            metadata.setContentLength(bytes.length)
            val is = new ByteArrayInputStream(bytes)
            val request = putObjectModifier(new PutObjectRequest(bucket, key, is, metadata))
            Some(request, iter)
          } else  {
            None
          }
        }

      val pool = Executors.newFixedThreadPool(threads)

      val write: PutObjectRequest => Process[Task, PutObjectResult] = { request =>
        Process eval Task {
          request.getInputStream.reset() // reset in case of retransmission to avoid 400 error
          s3client.putObject(request)
        }(pool).retryEBO {
          case e: AmazonS3Exception if e.getStatusCode == 503 => true
          case _ => false
        }
      }

      val results = nondeterminism.njoin(maxOpen = threads, maxQueued = threads) { requests map write } (Strategy.Executor(pool))
      results.run.unsafePerformSync
      pool.shutdown()
    }
  }
}

object S3RDDWriter extends S3RDDWriter {
  def getS3Client: () => S3Client = () => S3Client.default
}
