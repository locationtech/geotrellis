package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io.json._
import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.rdd.RDD
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.reflect._

class HadoopLayerUpdater[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container](
  val attributeStore: AttributeStore[JsonFormat],
  rddWriter: HadoopRDDWriter[K, V])
(implicit val cons: ContainerConstructor[K, V, Container])
  extends LayerUpdater[LayerId, K, V, Container with RDD[(K, V)]] {

  def update(id: LayerId, rdd: RDD[(K, V)]) = {
    try {
      if (!attributeStore.layerExists(id)) throw new LayerNotExistsError(id)

      implicit val sc = rdd.sparkContext
      implicit val mdFormat = cons.metaDataFormat

      val (existingHeader, _, existingKeyBounds, existingKeyIndex, _) =
        attributeStore.readLayerAttributes[HadoopLayerHeader, cons.MetaDataType, KeyBounds[K], KeyIndex[K], Unit](id)

      val boundable = implicitly[Boundable[K]]
      val keyBounds = boundable.getKeyBounds(rdd.asInstanceOf[RDD[(K, V)]])

      if (!boundable.includes(keyBounds.minKey, existingKeyBounds) || !boundable.includes(keyBounds.maxKey, existingKeyBounds))
        throw new OutOfKeyBoundsError(id)

      rddWriter.write(rdd, existingHeader.path, existingKeyIndex)
    } catch {
      case e: Exception => throw new LayerUpdateError(id).initCause(e)
    }
  }
}

object HadoopLayerUpdater {

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_]](
    attributeStore: HadoopAttributeStore,
    rddWriter: HadoopRDDWriter[K, V])
  (implicit cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerUpdater[K, V, Container[K]] =
    new HadoopLayerUpdater (
      attributeStore = attributeStore,
      rddWriter = rddWriter
    )

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_]](
    rootPath: Path,
    rddWriter: HadoopRDDWriter[K, V])
  (implicit cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerUpdater[K, V, Container[K]] =
    apply(
      attributeStore = HadoopAttributeStore(new Path(rootPath, "attributes"), new Configuration),
      rddWriter = rddWriter
    )

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_]](rootPath: Path)
  (implicit
    format: HadoopFormat[K, V],
    cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerUpdater[K, V, Container[K]] =
    apply(
      rootPath = rootPath,
      rddWriter = new HadoopRDDWriter[K, V](HadoopCatalogConfig.DEFAULT)
    )
}


