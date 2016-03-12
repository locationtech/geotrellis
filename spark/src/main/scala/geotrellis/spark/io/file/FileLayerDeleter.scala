package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.index._

import spray.json.JsonFormat
import org.apache.avro.Schema

import scala.reflect.ClassTag
import java.io.File

object FileLayerDeleter {
  def apply(attributeStore: FileAttributeStore): LayerDeleter[LayerId] =
    new LayerDeleter[LayerId] {
      def delete(layerId: LayerId): Unit = {
        // Read the metadata file out.
        val header =
          try {
            attributeStore.readHeader[FileLayerHeader](layerId)
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

  def apply(catalogPath: String): LayerDeleter[LayerId] =
    apply(FileAttributeStore(catalogPath))
}
