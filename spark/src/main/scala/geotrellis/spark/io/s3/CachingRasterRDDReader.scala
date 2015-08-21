package geotrellis.spark.io.s3

import java.io.{FileOutputStream, FileInputStream, File}
import org.apache.commons.io.IOUtils
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.json._
import geotrellis.spark.io.{FilteringRasterRDDReader, AttributeCaching}
import geotrellis.spark.io.avro.{AvroEncoder, KeyValueRecordCodec, AvroRecordCodec}
import geotrellis.spark.utils.KryoWrapper
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import spray.json.{JsObject, JsonFormat}
import spray.json.DefaultJsonProtocol._

import scala.reflect.ClassTag

class CachingRasterRDDReader[K: SpatialComponent: Boundable: AvroRecordCodec: JsonFormat: ClassTag]
(val attributeStore: S3AttributeStore, cacheDirectory: File)(implicit sc: SparkContext)
  extends FilteringRasterRDDReader[K] with AttributeCaching[S3LayerMetaData] with LazyLogging {

  val getS3Client: () => S3Client = () => S3Client.default
  val defaultNumPartitions = sc.defaultParallelism

  def read(id: LayerId, rasterQuery: RasterRDDQuery[K], numPartitions: Int): RasterRDD[K] = {
    val metadata  = getLayerMetadata(id)
    val keyBounds = getLayerKeyBounds[K](id)
    val keyIndex  = getLayerKeyIndex[K](id)

    val bucket = metadata.bucket
    val prefix = metadata.key

    val rasterMetadata = metadata.rasterMetaData
    val queryKeyBounds = rasterQuery(rasterMetadata, keyBounds)
    val ranges = queryKeyBounds.flatMap(keyIndex.indexRanges(_))
    val bins = RasterRDDReader.balancedBin(ranges, numPartitions)

    logger.debug(s"Loading layer from $bucket $prefix, ${ranges.length} ranges split into ${bins.length} bins")

    val writerSchema: Schema = (new Schema.Parser).parse(attributeStore.read[JsObject](id, "schema").toString())
    val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
    val recordCodec = KeyValueRecordCodec[K, Tile]
    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val toPath = (index: Long) => encodeIndex(index, maxWidth)
    val _getS3Client = getS3Client

    val BC = KryoWrapper((_getS3Client, recordCodec, writerSchema, cacheDirectory))

    val rdd =
      sc.parallelize(bins, bins.size)
        .mapPartitions { partition: Iterator[Seq[(Long, Long)]] =>
          val (getS3Client, recCodec, schema, cacheDir) = BC.value
          val s3client = getS3Client()

          val tileSeq: Iterator[Seq[(K, Tile)]] =
            for{
              rangeList <- partition // Unpack the one element of this partition, the rangeList.
              range <- rangeList
              index <- range._1 to range._2
            } yield {
              val path = makePath(prefix, toPath(index))
              val cachePath = new File(cacheDir, s"${id.name}__${id.zoom}__$index")

              try {
                // TODO: This could be abstracted with DirectRasterRDDReader, this would allow other caching strategies
                val bytes =
                  if (cachePath.exists()) {
                    IOUtils.toByteArray(new FileInputStream(cachePath))
                  } else {
                    val s3Bytes = IOUtils.toByteArray(s3client.getObject(bucket, path).getObjectContent)
                    val cacheOut = new FileOutputStream(cachePath)
                    try { cacheOut.write(s3Bytes) }
                    finally { cacheOut.close() }
                    s3Bytes
                  }
                val recs = AvroEncoder.fromBinary(schema, bytes)(recCodec)
                recs.filter { row => includeKey(row._1) }
              } catch {
                case e: AmazonS3Exception if e.getStatusCode == 404 => Seq.empty
              }
            }

          tileSeq.flatten
        }

    new RasterRDD[K](rdd, rasterMetadata)
  }
}