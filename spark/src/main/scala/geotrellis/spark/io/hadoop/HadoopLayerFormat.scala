package geotrellis.spark.io.hadoop

import geotrellis.raster.mosaic.MergeView
import geotrellis.spark.io._
import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import geotrellis.spark.io.json._
import geotrellis.spark.{SpatialComponent, Boundable, KeyBounds, LayerId}
import geotrellis.spark.mosaic._
import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.DefaultJsonProtocol._
import spray.json._
import scala.reflect._

class HadoopLayerFormat[K: Boundable: JsonFormat: ClassTag, V: MergeView: ClassTag, Container](
  val attributeStore: AttributeStore[JsonFormat],
      keyIndexMethod: KeyIndexMethod[K],
      rootPath      : Path,
      rddReader     : HadoopRDDReader[K, V],
      rddWriter     : HadoopRDDWriter[K, V])
  (implicit sc: SparkContext, val cons: ContainerConstructor[K, V, Container]) extends LayerFormat[LayerId, K, V, Container] {

  lazy val layerReader = new HadoopLayerReader(attributeStore, rddReader)
  lazy val layerWriter = new HadoopLayerWriter(rootPath, attributeStore, rddWriter, keyIndexMethod)

  type MetaDataType  = cons.MetaDataType

  val defaultNumPartitions = sc.defaultParallelism

  def update(id: LayerId, rdd: Container with RDD[(K, V)], numPartitions: Int) = {
    try {
      if(!attributeStore.layerExists(id)) throw new LayerNotExistsError(id)
      implicit val mdFormat = cons.metaDataFormat
      val layerPath = new Path(rootPath,  s"${id.name}/${id.zoom}")
      val header =
        HadoopLayerHeader(
          keyClass = classTag[K].toString(),
          valueClass = classTag[V].toString(),
          path = layerPath
        )
      val metaData = cons.getMetaData(rdd)
      val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd.asInstanceOf[RDD[(K, V)]])
      val keyIndex = keyIndexMethod.createIndex(keyBounds)

      val (existingHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) =
        attributeStore.readLayerAttributes[HadoopLayerHeader, MetaDataType, KeyBounds[K], KeyIndex[K], Unit](id)

      val rasterQuery = new RDDQuery[K, MetaDataType].where(Intersects(keyBounds))
      val queryKeyBounds = rasterQuery(existingMetaData, existingKeyBounds)

      val existing: RDD[(K, V)] =
        if (queryKeyBounds == Seq(existingKeyBounds)) {
          rddReader.readFully(layerPath)
        } else {
          val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
          rddReader.readFiltered(layerPath, queryKeyBounds, decompose)
        }

      val combinedMetaData = cons.combineMetaData(existingMetaData, metaData)
      val combinedKeyBounds = implicitly[Boundable[K]].combine(existingKeyBounds, keyBounds)
      val combinedRdd = existing merge rdd

      attributeStore.writeLayerAttributes(id, existingHeader, combinedMetaData, combinedKeyBounds, existingKeyIndex, Option.empty[Schema])
      rddWriter.write(combinedRdd, layerPath, existingKeyIndex)
    } catch {
      case e: LayerNotExistsError => throw new LayerNotExistsError(id).initCause(e)
      case e: Exception => throw new LayerUpdateError(id).initCause(e)
    }
  }
}

object HadoopLayerFormat {
  def apply[K: Boundable: JsonFormat: ClassTag, V: MergeView: ClassTag, Container[_]](
    attributeStore: HadoopAttributeStore,
    indexMethod: KeyIndexMethod[K],
    rootPath: Path,
    rddReader: HadoopRDDReader[K, V],
    rddWriter: HadoopRDDWriter[K, V])
  (implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerFormat[K, V, Container[K]] =
    new HadoopLayerFormat[K, V, Container[K]](
      attributeStore = attributeStore,
      keyIndexMethod = indexMethod,
      rootPath       = rootPath,
      rddReader      = rddReader,
      rddWriter      = rddWriter
    )

  def apply[K: Boundable: JsonFormat: ClassTag, V: MergeView: ClassTag, Container[_]](
    indexMethod: KeyIndexMethod[K],
    rootPath: Path,
    rddReader: HadoopRDDReader[K, V],
    rddWriter: HadoopRDDWriter[K, V])
  (implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerFormat[K, V, Container[K]] =
    apply(
      attributeStore = HadoopAttributeStore(new Path(rootPath, "attributes"), new Configuration),
      indexMethod    = indexMethod,
      rootPath       = rootPath,
      rddReader      = rddReader,
      rddWriter      = rddWriter
    )

  def apply[K: Boundable: JsonFormat: ClassTag, V: MergeView: ClassTag, Container[_]](
    rootPath: Path,
    indexMethod: KeyIndexMethod[K])
  (implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container[K]], format: HadoopFormat[K, V]): HadoopLayerFormat[K, V, Container[K]] =
    apply(
      indexMethod = indexMethod,
      rootPath    = rootPath,
      rddReader   = new HadoopRDDReader[K, V](HadoopCatalogConfig.DEFAULT),
      rddWriter   = new HadoopRDDWriter[K, V](HadoopCatalogConfig.DEFAULT)
    )
}
