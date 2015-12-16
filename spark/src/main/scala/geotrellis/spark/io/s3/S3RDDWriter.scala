package geotrellis.spark.io.s3

import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import org.apache.spark.rdd.RDD

import java.io.{ObjectOutputStream, ByteArrayOutputStream, ByteArrayInputStream}
import java.util.concurrent.Executors
import com.amazonaws.services.s3.model.{AmazonS3Exception, PutObjectResult, ObjectMetadata, PutObjectRequest}
import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.{AvroRecordCodec, AvroEncoder}
import geotrellis.spark.io.index.{ZCurveKeyIndexMethod, KeyIndexMethod, KeyIndex}
import geotrellis.spark.utils.KryoWrapper
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect._
import scalaz.concurrent.Task
import scalaz.stream.{Process, nondeterminism}
import com.typesafe.scalalogging.slf4j._

class S3RDDWriter [K: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag]() {

  def getS3Client: ()=>S3Client = () => S3Client.default
  val codec  = KeyValueRecordCodec[K, V]
  val schema = codec.schema

  def write(rdd: RDD[(K, V)], bucket: String, keyPath: K => String, oneToOne: Boolean): Unit = {
    implicit val sc = rdd.sparkContext

    val _getS3Client = getS3Client
    val _codec = codec

    val pathsToTiles =
      if (oneToOne) {
        rdd.map { case row => keyPath(row._1) -> Vector(row) }
      } else {
        rdd.groupBy { row => keyPath(row._1) }
      }

    pathsToTiles.foreachPartition { partition =>
      import geotrellis.spark.utils.TaskUtils._
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
            val request = new PutObjectRequest(bucket, key, is, metadata)
            Some(request, iter)
          } else  {
            None
          }
        }

      val pool = Executors.newFixedThreadPool(8)

      val write: PutObjectRequest => Process[Task, PutObjectResult] = { request =>
        Process eval Task {
          request.getInputStream.reset() // reset in case of retransmission to avoid 400 error
          s3client.putObject(request)
        }(pool).retryEBO {
          case e: AmazonS3Exception if e.getStatusCode == 503 => true
          case _ => false
        }
      }

      val results = nondeterminism.njoin(maxOpen = 8, maxQueued = 8) { requests map write }
      results.run.run
      pool.shutdown()
    }
  }
}

