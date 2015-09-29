package geotrellis.spark.io.hadoop

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io._
import geotrellis.spark.{KeyBounds, LayerId, Boundable}
import geotrellis.spark.io.json._
import org.apache.avro.Schema
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.{JsonFormat, JsObject}

import scala.reflect.ClassTag


/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V       Type of RDD Value (ex: Tile or MultiBandTile )
 * @tparam Container      Type of RDD Container that composes RDD and it's metadata (ex: RasterRDD or MultiBandRasterRDD)
 */
class HadoopLayerReader[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container](
  val attributeStore: HadoopAttributeStore,
  rddReader: HadoopRDDReader[K, V])
  (implicit sc: SparkContext, val cons: ContainerConstructor[K, V, Container])
  extends FilteringLayerReader[LayerId, K, Container] with LazyLogging {

  type MetaDataType  = cons.MetaDataType

  val defaultNumPartitions = sc.defaultParallelism

  def read(id: LayerId, rasterQuery: RDDQuery[K, MetaDataType], numPartitions: Int): Container = {
    try {
      val header = attributeStore.cacheRead[HadoopLayerHeader](id, Fields.header)
      val metadata = attributeStore.cacheRead[cons.MetaDataType](id, Fields.metaData)(cons.metaDataFormat)
      val keyBounds = attributeStore.cacheRead[KeyBounds[K]](id, Fields.keyBounds)

      val layerPath = header.path
      val queryKeyBounds = rasterQuery(metadata, keyBounds)

      //val writerSchema: Schema = (new Schema.Parser).parse(attributeStore.cacheRead[JsObject](id, "schema").toString())

      val rdd: RDD[(K, V)] =
        if (queryKeyBounds == Seq(keyBounds)) {
          rddReader.readFully(layerPath)
        } else {
          val keyIndex = attributeStore.cacheRead[KeyIndex[K]](id, Fields.keyIndex)
          val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
          rddReader.readFiltered(layerPath, queryKeyBounds, decompose)
        }

      cons.makeContainer(rdd, keyBounds, metadata)
    } catch {
      case e: Exception => throw new LayerReadError(id).initCause(e)
    }
  }
}

object HadoopLayerReader {
  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_]](
    attributeStore: HadoopAttributeStore,
    rddReader: HadoopRDDReader[K, V])
  (implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container[K]]) =
  new HadoopLayerReader[K, V, Container[K]](attributeStore, rddReader)

  def apply[K: Boundable: JsonFormat: ClassTag, TileType: ClassTag, Container[_]](
    rootPath: Path)
  (implicit
    sc: SparkContext,
    format: HadoopFormat[K, TileType],
    cons: ContainerConstructor[K, TileType, Container[K]]): HadoopLayerReader[K, TileType, Container[K]] =
    apply(HadoopAttributeStore(new Path(rootPath, "attributes")), new HadoopRDDReader[K, TileType](HadoopCatalogConfig.DEFAULT))



}