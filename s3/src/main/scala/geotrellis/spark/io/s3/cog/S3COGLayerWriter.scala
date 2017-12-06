package geotrellis.spark.io.s3.cog

import geotrellis.raster._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.cog.COGLayer.ContextGeoTiff
import geotrellis.spark.io.index.{Index, KeyIndexMethod}
import geotrellis.spark.io.s3.{S3AttributeStore, S3RDDWriter, makePath}
import geotrellis.spark.util._

import org.apache.spark.rdd._
import spray.json.JsonFormat
import com.amazonaws.services.s3.model.{AmazonS3Exception, ObjectMetadata, PutObjectRequest, PutObjectResult}

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors

import scala.reflect.{ClassTag, classTag}
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}

class S3COGLayerWriter(
  val getAttributeStore: () => S3AttributeStore,
  bucket: String,
  keyPrefix: String,
  threads: Int = S3RDDWriter.DefaultThreadCount
) extends Serializable {

  def write[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: ClassTag
  ](cogs: RDD[(K, ContextGeoTiff[K, V])])(id: LayerId, keyIndexMethod: KeyIndexMethod[K]) = {
    // schema for compatability purposes
    val schema = KryoWrapper(KeyValueRecordCodec[SpatialKey, Tile].schema)
    val kwFomat = KryoWrapper(implicitly[JsonFormat[K]])

    // headers would be stored AS IS
    // right now we are duplicating data
    // metadata can be stored more effificent
    // as a single JSON for each partial pyramid
    cogs.foreachPartition { partition: Iterator[(K, ContextGeoTiff[K, V])] =>
      import geotrellis.spark.util.TaskUtils._

      implicit val format: JsonFormat[K] = kwFomat.value
      implicit val metadataFormat: JsonFormat[TileLayerMetadata[K]] =
        tileLayerMetadataFormat[K](implicitly[SpatialComponent[K]], format)

      val attributeStore = getAttributeStore()
      val s3Client = attributeStore.s3Client
      val pool = Executors.newFixedThreadPool(threads)

      val requests: Process[Task, PutObjectRequest] =
        Process.unfold(partition)({ iter =>
          if (iter.hasNext) {
            val (key, tiff) = iter.next()
            val zoomRanges =
              tiff.zoomRanges.getOrElse(throw new Exception(s"No zoomRanges for the key: $key"))

            // prefix to the real file destination, it's a partial pyramid
            val prefix = makePath(keyPrefix, s"${id.name}/${zoomRanges._1}_${zoomRanges._2}")

            // header of all layers which corresponds to the current cog
            val header = S3COGLayerHeader(
              keyClass     = classTag[K].toString(),
              valueClass   = classTag[V].toString(),
              bucket       = bucket,
              key          = prefix,
              zoomRanges   = zoomRanges,
              layoutScheme = tiff.layoutScheme
            )

            // base layer attributes
            val metadata = tiff.metadata
            val keyBounds = metadata.bounds match {
              case kb: KeyBounds[K] => kb
              case EmptyBounds      => throw new Exception("Can't write an empty COG Layer.")
            }
            val keyIndex = keyIndexMethod.createIndex(keyBounds)
            attributeStore.writeLayerAttributes(id.copy(zoom = tiff.zoom), header, metadata, keyIndex, schema.value)

            // overviews attributes
            tiff.overviews.foreach { case (zoom, ovrMetadata) =>
              val ovrKeyBounds = ovrMetadata.bounds match {
                case kb: KeyBounds[K] => kb
                case EmptyBounds      => throw new Exception("Can't write an empty COG Layer.")
              }

              val keyIndex = keyIndexMethod.createIndex(ovrKeyBounds)
              attributeStore.writeLayerAttributes(id.copy(zoom = zoom), header, ovrMetadata, keyIndex, schema.value)
            }

            val bytes = GeoTiffWriter.write(tiff.geoTiff, true)
            val objectMetadata = new ObjectMetadata()
            objectMetadata.setContentLength(bytes.length)

            val is = new ByteArrayInputStream(bytes)
            val lastKeyIndex =
              if(tiff.overviews.nonEmpty) {
                val lastKeyBounds = tiff.overviews.last._2.bounds match {
                  case kb: KeyBounds[K] => kb
                  case EmptyBounds      => throw new Exception("Can't write an empty COG Layer.")
                }
                keyIndexMethod.createIndex(lastKeyBounds)
              }
              else keyIndexMethod.createIndex(keyBounds)

            val maxWidth = Index.digits(lastKeyIndex.toIndex(lastKeyIndex.keyBounds.maxKey))
            val keyPath = (key: K) => makePath(prefix, Index.encode(lastKeyIndex.toIndex(key), maxWidth))

            val p = new PutObjectRequest(bucket, s"${keyPath(key)}.tiff", is, objectMetadata)
            Some((p, iter))
          } else None
        })

      val write: PutObjectRequest => Process[Task, PutObjectResult] = { request =>
        Process eval Task {
          request.getInputStream.reset() // reset in case of retransmission to avoid 400 error
          s3Client.putObject(request)
        }(pool).retryEBO {
          case e: AmazonS3Exception if e.getStatusCode == 503 => true
          case _ => false
        }
      }

      val results = nondeterminism.njoin(maxOpen = threads, maxQueued = threads) { requests map write }(Strategy.Executor(pool))
      results.run.unsafePerformSync
      pool.shutdown()
    }
  }
}
