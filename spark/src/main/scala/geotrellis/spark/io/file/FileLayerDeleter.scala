package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._
import geotrellis.raster.io.Filesystem
import AttributeStore.Fields

import spray.json.JsonFormat
import org.apache.avro.Schema

import scala.reflect.ClassTag
import java.io.File

object FileLayerDeleter {
  def apply[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](attributeStore: FileAttributeStore): LayerDeleter[LayerId] =
    new LayerDeleter[LayerId] {
      def delete(layerId: LayerId): Unit = {
        // Read the metadata file out.
        val (header, metadata, keyBounds, keyIndex, writerSchema) = try {
          attributeStore.readLayerAttributes[FileLayerHeader, M, KeyBounds[K], KeyIndex[K], Schema](layerId)
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
    apply[K, V, M](FileAttributeStore(catalogPath))
}
