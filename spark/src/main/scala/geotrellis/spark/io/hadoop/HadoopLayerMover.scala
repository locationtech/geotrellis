package geotrellis.spark.io.hadoop

import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.{KeyBounds, LayerId}
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import spray.json.DefaultJsonProtocol._
import spray.json.JsonFormat
import scala.reflect.ClassTag

class HadoopLayerMover[K: JsonFormat: ClassTag, V: ClassTag, Container]
  (rootPath: Path, val attributeStore: AttributeStore[JsonFormat])
  (implicit sc: SparkContext,
          cons: ContainerConstructor[K, V, Container]) extends LayerMover[LayerId] {

  // dirty workaround
  val layerCopier = null
  val layerDeleter = null

  def headerUpdate(id: LayerId, header: HadoopLayerHeader): HadoopLayerHeader =
    header.copy(path = new Path(rootPath, s"${id.name}/${id.zoom}"))

  override def move(from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)
    implicit val mdFormat = cons.metaDataFormat
    val (header, metadata, keyBounds, keyIndex, _) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, cons.MetaDataType, KeyBounds[K], KeyIndex[K], Unit](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(from).initCause(e)
    }
    HdfsUtils.renamePath(header.path, new Path(rootPath,  s"${to.name}/${to.zoom}"), sc.hadoopConfiguration)
    attributeStore.writeLayerAttributes(
      to, headerUpdate(to, header), metadata, keyBounds, keyIndex, Option.empty[Schema]
    )
    attributeStore.delete(from)
    attributeStore.clearCache()
  }
}

object HadoopLayerMover {
  def apply[K: JsonFormat: ClassTag, V: ClassTag, Container[_]]
    (rootPath: Path, attributeStore: AttributeStore[JsonFormat])
    (implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerMover[K, V, Container[K]] =
      new HadoopLayerMover[K, V, Container[K]](rootPath, attributeStore)

  def apply[K: JsonFormat: ClassTag, V: ClassTag, Container[_]]
    (rootPath: Path)(implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerMover[K, V, Container[K]] =
    apply[K, V, Container](rootPath, HadoopAttributeStore(new Path(rootPath, "attributes"), new Configuration))
}
