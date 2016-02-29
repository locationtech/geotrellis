package geotrellis.spark.io

import geotrellis.spark._
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect._

class SparkLayerCopier[Header: JsonFormat, K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
  val attributeStore: AttributeStore[JsonFormat],
  layerReader: FilteringLayerReader[LayerId, K, M, RDD[(K, V)] with Metadata[M]],
  layerWriter: Writer[LayerId, RDD[(K, V)] with Metadata[M]]
) extends LayerCopier[LayerId] {
  def copy(from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    try {
      layerWriter.write(to, layerReader.read(from))
    } catch {
      case e: Exception => new LayerCopyError(from, to).initCause(e)
    }
  }
}
