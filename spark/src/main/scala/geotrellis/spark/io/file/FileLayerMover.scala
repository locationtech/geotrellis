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

object FileLayerMover {
  def custom[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, I <: KeyIndex[K]: JsonFormat](sourceAttributeStore: FileAttributeStore, targetAttributeStore: FileAttributeStore): LayerMover[LayerId] =
    new LayerMover[LayerId] {
      def move(from: LayerId, to: LayerId): Unit = {
        if(targetAttributeStore.layerExists(to))
          throw new LayerExistsError(to)

        val sourceMetadataFile = sourceAttributeStore.attributeFile(from, Fields.metaData)
        if(!sourceMetadataFile.exists) throw new LayerNotFoundError(from)

        // Read the metadata file out.
        val (header, metadata, keyBounds, keyIndex, writerSchema) = try {
          sourceAttributeStore.readLayerAttributes[FileLayerHeader, M, KeyBounds[K], I, Schema](from)
        } catch {
          case e: AttributeNotFoundError => throw new LayerReadError(from).initCause(e)
        }

        // Move over any other attributes
        for((attributeName, file) <- sourceAttributeStore.attributeFiles(to)) {
          if(file.getAbsolutePath != sourceMetadataFile.getAbsolutePath) {
            val source = file.getAbsolutePath
            val target = targetAttributeStore.attributeFile(to, attributeName).getAbsolutePath
            Filesystem.move(source, target)
          }
        }

        val sourceLayerPath = new File(sourceAttributeStore.catalogPath, header.path)
        val targetHeader = header.copy(path = LayerPath(to))

        targetAttributeStore.writeLayerAttributes(to, targetHeader, metadata, keyBounds, keyIndex, writerSchema)

        // Delete the metadata file in the source
        sourceMetadataFile.delete()

        // Move all the elements
        val targetLayerPath = Filesystem.ensureDirectory(LayerPath(targetAttributeStore.catalogPath, to))
        sourceLayerPath
          .listFiles()
          .foreach { f =>
            val target = new File(targetLayerPath, f.getName)
            Filesystem.move(f, target)
          }

        // Clear the caches
        sourceAttributeStore.clearCache()
        targetAttributeStore.clearCache()
      }
    }

  def custom[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, I <: KeyIndex[K]: JsonFormat](catalogPath: String): LayerMover[LayerId] =
    custom[K, V, M, I](FileAttributeStore(catalogPath))

  def custom[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, I <: KeyIndex[K]: JsonFormat](attributeStore: FileAttributeStore): LayerMover[LayerId] =
    custom[K, V, M, I](attributeStore, attributeStore)

  def custom[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, I <: KeyIndex[K]: JsonFormat](sourceCatalogPath: String, targetCatalogPath: String): LayerMover[LayerId] =
    custom[K, V, M, I](FileAttributeStore(sourceCatalogPath), FileAttributeStore(targetCatalogPath))

  def apply[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](sourceAttributeStore: FileAttributeStore, targetAttributeStore: FileAttributeStore): LayerMover[LayerId] =
    custom[K, V, M, KeyIndex[K]](sourceAttributeStore, targetAttributeStore)

  def apply[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](catalogPath: String): LayerMover[LayerId] =
    apply[K, V, M](FileAttributeStore(catalogPath))

  def apply[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](attributeStore: FileAttributeStore): LayerMover[LayerId] =
    apply[K, V, M](attributeStore, attributeStore)

  def apply[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](sourceCatalogPath: String, targetCatalogPath: String): LayerMover[LayerId] =
    apply[K, V, M](FileAttributeStore(sourceCatalogPath), FileAttributeStore(targetCatalogPath))
}
