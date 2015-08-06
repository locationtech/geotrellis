package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io.AttributeStore
import geotrellis.spark.io.index._
import org.apache.spark.SparkContext
import java.io.ByteArrayInputStream
import com.amazonaws.services.s3.model.{PutObjectRequest, PutObjectResult, ObjectMetadata}
import com.typesafe.scalalogging.slf4j._
import com.amazonaws.services.s3.model.AmazonS3Exception
import scala.reflect.ClassTag
import java.util.concurrent.Executors
import scalaz.stream._
import scalaz.concurrent.Task
import geotrellis.spark.io.avro._
import spray.json.DefaultJsonProtocol._

class RasterRDDWriter[K: AvroRecordCodec: Boundable: ClassTag] extends LazyLogging {
  def write(
    attributes: S3AttributeStore,
    s3client: ()=>S3Client, 
    bucket: String, 
    layerPath: String,
    keyBounds: KeyBounds[K],
    keyIndex: KeyIndex[K],
    clobber: Boolean
    )
  (layerId: LayerId, rdd: RasterRDD[K])
  (implicit sc: SparkContext): Unit = {
//    if (s3client().listObjectsIterator(bucket, layerPath, 1).hasNext && ! clobber)
//      throw new LayerWriteError(layerId, s"Directory already exists: $layerPath")

    val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))

    val catalogBucket = bucket
    val dir = layerPath

    val toPath = (index: Long) => encodeIndex(index, maxWidth)
    val codec = recordCodec(implicitly[AvroRecordCodec[K]], tileUnionCodec)

    attributes.write(layerId,"schema", codec.schema.toString)

    val BC = sc.broadcast((
      s3client,
      codec
    ))

    logger.info(s"Saving RasterRDD ${rdd.name} to $bucket  $layerPath")

    rdd
      .groupBy{ row => keyIndex.toIndex(row._1) } // TODO: this can be a map in spatial case
      .foreachPartition { partition =>
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
              val path = List(dir, toPath(index)).filter(_.nonEmpty).mkString("/")
              val request = new PutObjectRequest(catalogBucket, path, is, metadata)
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

    logger.info(s"Finished saving tiles to $layerPath")
  }   
}
