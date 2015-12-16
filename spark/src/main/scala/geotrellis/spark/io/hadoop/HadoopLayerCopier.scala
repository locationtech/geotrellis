package geotrellis.spark.io.hadoop

import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.{KeyBounds, LayerId}
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._
import scala.reflect.ClassTag

class HadoopLayerCopier[K: JsonFormat: ClassTag, V: ClassTag, Container](
   rootPath: Path, val attributeStore: AttributeStore[JsonFormat])
  (implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container]) extends LayerCopier[LayerId] {

  def copy(from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)
    implicit val mdFormat = cons.metaDataFormat
    val (header, metadata, keyBounds, keyIndex, _) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, cons.MetaDataType, KeyBounds[K], KeyIndex[K], Unit](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(from).initCause(e)
    }
    val newPath = new Path(rootPath,  s"${to.name}/${to.zoom}")
    HdfsUtils.copyPath(header.path, newPath, sc.hadoopConfiguration)
    attributeStore.writeLayerAttributes(
      to, header.copy(path = newPath), metadata, keyBounds, keyIndex, Option.empty[Schema]
    )
  }
}

object HadoopLayerCopier {
  def apply[K: JsonFormat: ClassTag, V: ClassTag, Container[_]](
    rootPath: Path, attributeStore: AttributeStore[JsonFormat])
  (implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerCopier[K, V, Container[K]] =
    new HadoopLayerCopier[K, V, Container[K]](rootPath, attributeStore)

  def apply[K: JsonFormat: ClassTag, V: ClassTag, Container[_]](
    rootPath: Path)
   (implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerCopier[K, V, Container[K]] =
    apply[K, V, Container](rootPath, HadoopAttributeStore(new Path(rootPath, "attributes"), new Configuration))
}
