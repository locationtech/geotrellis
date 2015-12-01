package geotrellis.spark.io.hadoop

import geotrellis.spark.{Boundable, LayerId}
import geotrellis.spark.io.{AttributeStore, LayerDeleter}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import spray.json.JsonFormat
import scala.reflect.ClassTag
import spray.json.DefaultJsonProtocol._

class HadoopLayerDeleter[K: Boundable: JsonFormat: ClassTag]
  (attributeStore: AttributeStore[JsonFormat], conf: Configuration) extends LayerDeleter[LayerId] {
  def delete(id: LayerId): Unit = {
    val (header, _, _, _, _) =
      attributeStore.readLayerAttributes[HadoopLayerHeader, Unit, Unit, Unit, Unit](id)
    HdfsUtils.deletePath(header.path, conf)
    attributeStore.delete(id)
  }
}

object HadoopLayerDeleter {
  def apply[K: Boundable: JsonFormat: ClassTag](rootPath: Path) =
    new HadoopLayerDeleter[K](HadoopAttributeStore(new Path(rootPath, "attributes"), new Configuration), new Configuration)

  def apply[K: Boundable: JsonFormat: ClassTag](rootPath: Path, conf: Configuration) =
    new HadoopLayerDeleter[K](HadoopAttributeStore(new Path(rootPath, "attributes"), conf), conf)
}
