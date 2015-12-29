package geotrellis.spark.io.hadoop

import geotrellis.spark.LayerId
import geotrellis.spark.io._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._

class HadoopLayerDeleter(val attributeStore: AttributeStore[JsonFormat], conf: Configuration) extends LayerDeleter[LayerId] {
  def delete(id: LayerId): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val (header, _, _, _, _) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, Unit, Unit, Unit, Unit](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerDeleteError(id).initCause(e)
    }
    HdfsUtils.deletePath(header.path, conf)
    attributeStore.delete(id)
    attributeStore.clearCache()
  }
}

object HadoopLayerDeleter {
  def apply(attributeStore: AttributeStore[JsonFormat], conf: Configuration): HadoopLayerDeleter =
    new HadoopLayerDeleter(attributeStore, conf)

  def apply(rootPath: Path, conf: Configuration): HadoopLayerDeleter =
    apply(HadoopAttributeStore(new Path(rootPath, "attributes"), conf), conf)

  def apply(rootPath: Path): HadoopLayerDeleter =
    apply(HadoopAttributeStore(new Path(rootPath, "attributes"), new Configuration), new Configuration)
}
