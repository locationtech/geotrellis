package geotrellis.spark.io.s3

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index.KeyIndexMethod
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect._
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

    val prefix = makePath(keyPrefix, s"${id.name}/${id.zoom}")

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
    val keyPath = (index: Long) => makePath(prefix, encodeIndex(index, maxWidth))
    val codec = KeyValueRecordCodec[K, Tile]
    attributeStore.write(id,"schema", codec.schema.toString.parseJson)

    logger.info(s"Saving RasterRDD ${rdd.name} to $bucket  $prefix")
    new RDDWriter[K, Tile](bucket, getS3Client)
      .write(rdd, keyIndex, keyPath, oneToOne = false)
  }
}