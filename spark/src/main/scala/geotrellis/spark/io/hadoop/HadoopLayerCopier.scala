package geotrellis.spark.io.hadoop

import geotrellis.spark.LayerId
import geotrellis.spark.io._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._

class HadoopLayerCopier(rootPath: Path, attributeStore: AttributeStore[JsonFormat])
                       (implicit sc: SparkContext) extends LayerCopier[LayerId] {
  def copy(from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)
    val (header, _, _, _, _) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, Unit, Unit, Unit, Unit](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerDeleteError(from).initCause(e)
    }
    HdfsUtils.copyPath(header.path, new Path(rootPath,  s"${to.name}/${to.zoom}"), sc.hadoopConfiguration)
    attributeStore.copy(from, to)
  }
}

object HadoopLayerCopier {
  def apply(rootPath: Path, attributeStore: AttributeStore[JsonFormat])(implicit sc: SparkContext): HadoopLayerCopier =
    new HadoopLayerCopier(rootPath, attributeStore)

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopLayerCopier =
    apply(rootPath, HadoopAttributeStore(new Path(rootPath, "attributes"), new Configuration))
}
