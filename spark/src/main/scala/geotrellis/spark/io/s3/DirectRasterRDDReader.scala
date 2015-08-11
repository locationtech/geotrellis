package geotrellis.spark.io.s3

import com.amazonaws.services.s3.model.AmazonS3Exception
import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import org.apache.spark.SparkContext
import com.typesafe.scalalogging.slf4j._
import scala.reflect.ClassTag
import geotrellis.spark.io.avro._
import org.apache.avro._
import spray.json.DefaultJsonProtocol._
import spray.json._

class DirectRasterRDDReader[K: AvroRecordCodec: Boundable: ClassTag] extends RasterRDDReader[K] with LazyLogging {
  /** Converting lower bound of Range to first Key for Marker */
  override val indexToPath: (Long, Int) => String = encodeIndex

  override def read(
    attributes: S3AttributeStore,
    s3client: ()=>S3Client,
    layerMetaData: S3LayerMetaData,
    keyBounds: KeyBounds[K],
    keyIndex: KeyIndex[K],
    numPartitions: Int
    )
    (layerId: LayerId, queryKeyBounds: Seq[KeyBounds[K]])
    (implicit sc: SparkContext): RasterRDD[K] = {
    val bucket = layerMetaData.bucket
    val dir = layerMetaData.key
    val rasterMetaData = layerMetaData.rasterMetaData

    // TODO: now that we don't have to LIST, can we prefix key with a hash to get better throughput ?
    val ranges = queryKeyBounds.flatMap(keyIndex.indexRanges(_))
    val bins = RasterRDDReader.balancedBin(ranges, numPartitions)
    logger.debug(s"Loading layer from $bucket $dir, ${ranges.length} ranges split into ${bins.length} bins")

    // Broadcast the functions and objects we can use in the closure
    val writerSchema: Schema = (new Schema.Parser).parse(attributes.read[JsObject](layerId, "schema").toString())

    val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
    val BC = sc.broadcast((
      s3client,
      { (index: Long) => indexToPath(index, maxWidth) },
      { (key: K) => queryKeyBounds.includeKey(key) },
      KeyValueRecordCodec[K, Tile],
      writerSchema
      ))

    val rdd =
      sc.parallelize(bins, bins.size)
        .mapPartitions { partition: Iterator[Seq[(Long, Long)]] =>
          val (fS3client, toPath, includeKey, recCodec, schema) = BC.value
          val s3client = fS3client()

          val tileSeq: Iterator[Seq[(K, Tile)]] =
            for{
              rangeList <- partition // Unpack the one element of this partition, the rangeList.
              range <- rangeList
              index <- range._1 to range._2
            } yield {
              val path = List(dir, toPath(index)).filter(_.nonEmpty).mkString("/")

              try {
                val is = s3client.getObject(bucket, path).getObjectContent
                val bytes = org.apache.commons.io.IOUtils.toByteArray(is)
                val recs = AvroEncoder.fromBinary(schema, bytes)(recCodec)
                recs.filter { row => includeKey(row._1) }
              } catch {
                case e: AmazonS3Exception if e.getStatusCode == 404 => Seq.empty
              }
            }

          tileSeq.flatten
        }

    new RasterRDD[K](rdd, rasterMetaData)
  }
}
