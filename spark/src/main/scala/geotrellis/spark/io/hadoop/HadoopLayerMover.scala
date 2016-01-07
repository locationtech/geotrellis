package geotrellis.spark.io.hadoop

import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.accumulo.AccumuloLayerHeader
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.{KeyBounds, LayerId}
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

import spray.json.{JsObject, JsonFormat}
import spray.json.DefaultJsonProtocol._

import scala.reflect.ClassTag

class HadoopLayerMover[
  K: JsonFormat: ClassTag, V: ClassTag,
  M: JsonFormat, I <: KeyIndex[K]: JsonFormat]
  (rootPath: Path, val attributeStore: AttributeStore[JsonFormat])
  (implicit sc: SparkContext) extends LayerMover[LayerId] {

  override def move(from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    println(s"attributeStore.readLayerAttribute[AccumuloLayerHeader](from, Fields.metaData): ${attributeStore.readLayerAttribute[JsObject](from, "keyIndex")}")
    println(s"implicitly[JsonFormat[I]]: ${implicitly[JsonFormat[I]]}")

    val (header, metadata, keyBounds, keyIndex, _) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, M, KeyBounds[K], I, Unit](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerMoveError(from, to).initCause(e)
    }
    val newPath = new Path(rootPath,  s"${to.name}/${to.zoom}")
    HdfsUtils.renamePath(header.path, newPath, sc.hadoopConfiguration)
    attributeStore.writeLayerAttributes(
      to, header.copy(path = newPath), metadata, keyBounds, keyIndex, Option.empty[Schema]
    )
    attributeStore.delete(from)
    attributeStore.clearCache()
  }
}

object HadoopLayerMover {
  def apply[
    K: JsonFormat: ClassTag, V: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat]
    (rootPath: Path, attributeStore: AttributeStore[JsonFormat])
    (implicit sc: SparkContext): HadoopLayerMover[K, V, M, I] =
      new HadoopLayerMover[K, V, M, I](rootPath, attributeStore)

  def apply[
    K: JsonFormat: ClassTag, V: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat]
    (rootPath: Path)(implicit sc: SparkContext): HadoopLayerMover[K, V, M, I] =
    apply[K, V, M, I](rootPath, HadoopAttributeStore(new Path(rootPath, "attributes"), new Configuration))
}
