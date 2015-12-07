package geotrellis.spark.io.hadoop

import geotrellis.spark.LayerId
import geotrellis.spark.io._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import spray.json.DefaultJsonProtocol._
import spray.json.JsonFormat

class HadoopLayerMover(rootPath: Path, attributeStore: AttributeStore[JsonFormat])
                      (implicit sc: SparkContext) extends LayerMover[LayerId] {
  def move(from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)
    val (header, _, _, _, _) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, Unit, Unit, Unit, Unit](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerDeleteError(from).initCause(e)
    }
    HdfsUtils.renamePath(header.path, new Path(rootPath,  s"${to.name}/${to.zoom}"), sc.hadoopConfiguration)
    attributeStore.move(from, to)
  }
}

object HadoopLayerMover {
  def apply(rootPath: Path, attributeStore: AttributeStore[JsonFormat])(implicit sc: SparkContext): HadoopLayerMover =
    new HadoopLayerMover(rootPath, attributeStore)

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopLayerMover =
    apply(rootPath, HadoopAttributeStore(new Path(rootPath, "attributes"), new Configuration))
}
