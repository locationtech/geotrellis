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

class SpatialS3LayerReader(
  bucket: String,
  prefix: String,
  getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None)
(implicit sc: SparkContext, cons: ContainerConstructor[SpatialKey, Tile, RasterMetaData, RasterRDD[SpatialKey]])
  extends S3LayerReader[SpatialKey, Tile, RasterMetaData, RasterRDD[SpatialKey]](
  new S3AttributeStore(bucket, prefix),
  new S3RDDReader[SpatialKey, Tile],
  getCache
)

class SpatialMultiBandS3LayerReader(
  bucket: String,
  prefix: String,
  getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None)
(implicit sc: SparkContext, cons: ContainerConstructor[SpatialKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpatialKey]])
  extends S3LayerReader[SpatialKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpatialKey]](
  new S3AttributeStore(bucket, prefix),
  new S3RDDReader[SpatialKey, MultiBandTile],
  getCache
)

