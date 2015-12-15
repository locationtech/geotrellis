package geotrellis.spark.io.s3

import geotrellis.raster.{MultiBandTile, Tile}
import geotrellis.spark._
import geotrellis.spark.io.ContainerConstructor
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.utils.cache.Cache
import org.apache.spark.SparkContext

import scala.reflect.ClassTag

case class SpaceTimeS3LayerReader(
  bucket: String,
  prefix: String,
  getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None)
(implicit sc: SparkContext, cons: ContainerConstructor[SpaceTimeKey, Tile, RasterMetaData, RasterRDD[SpaceTimeKey]])
  extends S3LayerReader[SpaceTimeKey, Tile, RasterMetaData, RasterRDD[SpaceTimeKey]](
  new S3AttributeStore(bucket, prefix),
  new S3RDDReader[SpaceTimeKey, Tile],
  getCache
)

case class SpaceTimeMultiBandS3LayerReader(
  bucket: String,
  prefix: String,
  getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None)
(implicit sc: SparkContext, cons: ContainerConstructor[SpaceTimeKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpaceTimeKey]])
extends S3LayerReader[SpaceTimeKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpaceTimeKey]](
  new S3AttributeStore(bucket, prefix),
  new S3RDDReader[SpaceTimeKey, MultiBandTile],
  getCache
)