package geotrellis.spark.io.hadoop

import geotrellis.spark.io.json._
import geotrellis.spark._
import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import geotrellis.spark.io._
import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.rdd.RDD
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect._

class HadoopLayerWriter[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container](
  rootPath: Path,
  val attributeStore: AttributeStore[JsonFormat],
  rddWriter: HadoopRDDWriter[K, V],
  keyIndexMethod: KeyIndexMethod[K])
(implicit val cons: ContainerConstructor[K, V, Container])
  extends Writer[LayerId, Container with RDD[(K, V)]] {

  def write(id: LayerId, rdd: Container with RDD[(K, V)]): Unit = {
    implicit val sc = rdd.sparkContext

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

    try {
      implicit val mdFormat = cons.metaDataFormat
      attributeStore.writeLayerAttributes(id, header, metaData, keyBounds, keyIndex, Option.empty[Schema])
      // TODO: Writers need to handle Schema changes

      rddWriter.write(rdd, layerPath, keyIndex)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }

  def update(id: LayerId, rdd: BoundRDD[K, V]) = {
    try {
      if (!attributeStore.layerExists(id)) throw new LayerNotExistsError(id)

      implicit val sc = rdd.sparkContext
      implicit val mdFormat = cons.metaDataFormat
      val layerPath = new Path(rootPath,  s"${id.name}/${id.zoom}")
      val header =
        HadoopLayerHeader(
          keyClass = classTag[K].toString(),
          valueClass = classTag[V].toString(),
          path = layerPath
        )

      val (existingHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) =
        attributeStore.readLayerAttributes[HadoopLayerHeader, cons.MetaDataType, KeyBounds[K], KeyIndex[K], Unit](id)

      if (existingHeader != header) throw new HeaderMatchError(id, existingHeader, header)

      val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd.asInstanceOf[RDD[(K, V)]])
      val keyIndex = keyIndexMethod.createIndex(keyBounds)

      val (existingIndexMin, existingIndexMax) = existingKeyIndex.toIndex(existingKeyBounds.minKey) -> existingKeyIndex.toIndex(existingKeyBounds.maxKey)
      val (indexMin, indexMax) = keyIndex.toIndex(keyBounds.minKey) -> keyIndex.toIndex(keyBounds.maxKey)

      if (existingIndexMin > indexMin || existingIndexMax < indexMax)
        throw new OutOfKeyBoundsError(id)

      rddWriter.write(rdd, layerPath, existingKeyIndex)
    } catch {
      case e: HeaderMatchError[_] => throw e.initCause(e)
      case e: OutOfKeyBoundsError => throw e.initCause(e)
      case e: LayerNotExistsError => throw e.initCause(e)
      case e: Exception => throw new LayerUpdateError(id).initCause(e)
    }
  }
}

object HadoopLayerWriter {

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_]](
    rootPath: Path,
    attributeStore: HadoopAttributeStore,
    rddWriter: HadoopRDDWriter[K, V],
    indexMethod: KeyIndexMethod[K])
  (implicit cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerWriter[K, V, Container[K]] =
    new HadoopLayerWriter (
      rootPath = rootPath,
      attributeStore = attributeStore,
      rddWriter = rddWriter,
      keyIndexMethod = indexMethod
    )

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_]](
    rootPath: Path,
    rddWriter: HadoopRDDWriter[K, V],
    indexMethod: KeyIndexMethod[K])
  (implicit cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerWriter[K, V, Container[K]] =
    apply(
      rootPath = rootPath,
      attributeStore = HadoopAttributeStore(new Path(rootPath, "attributes"), new Configuration),
      rddWriter = rddWriter,
      indexMethod = indexMethod)

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_]](
    rootPath: Path,
    indexMethod: KeyIndexMethod[K])
  (implicit
    format: HadoopFormat[K, V],
    cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerWriter[K, V, Container[K]] =
    apply(
      rootPath = rootPath,
      rddWriter = new HadoopRDDWriter[K, V](HadoopCatalogConfig.DEFAULT),
      indexMethod = indexMethod)
}