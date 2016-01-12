package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._

import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol.UnitJsonFormat

import scala.reflect.ClassTag
import java.io.File

object FileLayerDeleter {
  def apply(attributeStore: FileAttributeStore): LayerDeleter[LayerId] =
    new LayerDeleter[LayerId] {
      def delete(layerId: LayerId): Unit = {
        // Read the metadata file out.
        val (header, _, _, _, _) = try {
          attributeStore.readLayerAttributes[FileLayerHeader, Unit, Unit, Unit, Unit](layerId)
        } catch {
          case e: AttributeNotFoundError => 
            throw new LayerNotFoundError(layerId).initCause(e)
        }

        // Delete the attributes
        for((_, file) <- attributeStore.attributeFiles(layerId)) {
          file.delete()
        }

        // Delete the elements
        val sourceLayerPath = new File(attributeStore.catalogPath, header.path)
        sourceLayerPath
          .listFiles()
          .foreach(_.delete())
      }
    }

  def apply[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](catalogPath: String): LayerDeleter[LayerId] =
    apply(FileAttributeStore(catalogPath))
}
