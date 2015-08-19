package geotrellis.spark.io.s3

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import com.amazonaws.services.s3.model.{AmazonS3Exception, PutObjectResult, ObjectMetadata, PutObjectRequest}
import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.{AvroRecordCodec, AvroEncoder, KeyValueRecordCodec}
import geotrellis.spark.io.index.{ZCurveKeyIndexMethod, KeyIndexMethod, KeyIndex}
import geotrellis.spark.utils.KryoWrapper
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect._
import scalaz.concurrent.Task
import scalaz.stream.{Process, nondeterminism}
import com.typesafe.scalalogging.slf4j._

class RasterRDDWriter[K: SpatialComponent: Boundable: AvroRecordCodec: JsonFormat: ClassTag](
  bucket: String,
  keyPrefix: String,
  keyIndexMethod: KeyIndexMethod[K],
  clobber: Boolean = true)
(val attributeStore: S3AttributeStore = S3AttributeStore(bucket, keyPrefix))
  extends Writer[LayerId, RasterRDD[K]] with AttributeCaching[S3LayerMetaData] with LazyLogging {

  val getS3Client: ()=>S3Client = () => S3Client.default

  def write(id: LayerId, rdd: RasterRDD[K]) = {
    implicit val sc = rdd.sparkContext
    require(clobber, "S3 writer does not yet perform a clobber check") // TODO: Check for clobber

    val bucket = this.bucket
    val prefix = makePath(keyPrefix, S3RasterCatalog.layerPath(id))

    val metadata = S3LayerMetaData(
      layerId = id,
      keyClass = classTag[K].toString(),
      rasterMetaData = rdd.metaData,
      bucket = bucket,
      key = prefix)

    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)

    val keyIndex = keyIndexMethod.createIndex(
      KeyBounds(
        keyBounds.minKey.updateSpatialComponent(SpatialKey(0, 0)),
        keyBounds.maxKey.updateSpatialComponent(SpatialKey(rdd.metaData.tileLayout.layoutCols - 1, rdd.metaData.tileLayout.layoutRows - 1))
      )
    )

    setLayerMetadata(id, metadata)
    setLayerKeyBounds(id, keyBounds)
    setLayerKeyIndex(id, keyIndex)

    val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
    val keyPath = (index: Long) => encodeIndex(index, maxWidth)
    val codec = KeyValueRecordCodec[K, Tile]
    attributeStore.write(id,"schema", codec.schema.toString.parseJson)

    logger.info(s"Saving RasterRDD ${rdd.name} to $bucket  $prefix")
    val BC = KryoWrapper((getS3Client, codec))
    rdd
      .groupBy { row => keyIndex.toIndex(row._1) } // TODO: this can be a map in spatial case
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
              val request = new PutObjectRequest(bucket, makePath(prefix, keyPath(index)), is, metadata)
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

    logger.info(s"Finished saving tiles to $prefix")
  }
}