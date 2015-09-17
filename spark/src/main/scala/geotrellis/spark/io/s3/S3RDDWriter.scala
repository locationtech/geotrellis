package geotrellis.spark.io.s3

import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import org.apache.spark.rdd.RDD

import java.io.ByteArrayInputStream
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

class S3RDDWriter [K: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](bucket: String, getS3Client: ()=>S3Client) {

  def write(rdd: RDD[(K, V)], keyIndex: KeyIndex[K], keyPath: Long => String, oneToOne: Boolean): Unit = {
    implicit val sc = rdd.sparkContext
    val bucket = this.bucket
    val codec  = KeyValueRecordCodec[K, V]

    val BC = KryoWrapper((getS3Client, codec))

    if (oneToOne) {
      rdd.map { case row => keyIndex.toIndex(row._1) -> Vector(row) }
    } else {
      rdd.groupBy { row => keyIndex.toIndex(row._1) }
    }.foreachPartition { partition =>
      import geotrellis.spark.utils.TaskUtils._
      val (getS3Client, recsCodec) = BC.value
      val s3client: S3Client = getS3Client()

      val requests: Process[Task, PutObjectRequest] =
        Process.unfold(partition){ iter =>
          if (iter.hasNext) {
            val recs = iter.next()
            val index = recs._1
            val pairs = recs._2.toVector
            val bytes = AvroEncoder.toBinary(pairs)(recsCodec)
            val metadata = new ObjectMetadata()
            metadata.setContentLength(bytes.length)
            val is = new ByteArrayInputStream(bytes)
            val request = new PutObjectRequest(bucket, keyPath(index), is, metadata)
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

