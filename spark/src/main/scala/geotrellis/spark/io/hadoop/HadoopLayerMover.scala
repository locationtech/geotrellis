package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import geotrellis.util._

import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._

import scala.reflect.ClassTag

class HadoopLayerMover(
  rootPath: Path,
  val attributeStore: AttributeStore
)(implicit sc: SparkContext) extends LayerMover[LayerId] {

  override def move[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, M, K](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerMoveError(from, to).initCause(e)
    }
    val (newLayerRoot, newPath) = new Path(rootPath,  s"${to.name}") -> new Path(rootPath,  s"${to.name}/${to.zoom}")
    // new layer name root has to be created before layerId renaming
    HdfsUtils.ensurePathExists(newLayerRoot, sc.hadoopConfiguration)
    HdfsUtils.renamePath(header.path, newPath, sc.hadoopConfiguration)
    attributeStore.writeLayerAttributes(
      to, header.copy(path = newPath), metadata, keyIndex, writerSchema
    )
    attributeStore.delete(from)
    attributeStore.clearCache()
  }
}

object HadoopLayerMover {
  def apply(
    rootPath: Path,
    attributeStore: AttributeStore
  )(implicit sc: SparkContext): HadoopLayerMover =
    new HadoopLayerMover(rootPath, attributeStore)

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopLayerMover =
    apply(rootPath, HadoopAttributeStore(rootPath, new Configuration))

  def apply(attributeStore: HadoopAttributeStore)(implicit sc: SparkContext): HadoopLayerMover =
    apply(attributeStore.rootPath, attributeStore)
}
