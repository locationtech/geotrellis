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

object FileLayerCopier {
  def apply[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat]
    (sourceAttributeStore: FileAttributeStore, targetAttributeStore: FileAttributeStore): LayerCopier[LayerId, K] =
    new LayerCopier[LayerId, K] {

      def copy[I <: KeyIndex[K]: JsonFormat](from: LayerId, to: LayerId, format: JsonFormat[I]): Unit = {
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
          val source = file.getAbsolutePath
          val target = targetAttributeStore.attributeFile(to, attributeName).getAbsolutePath
          Filesystem.copy(source, target)
        }

        val sourceLayerPath = new File(sourceAttributeStore.catalogPath, header.path)
        val targetHeader = header.copy(path = LayerPath(to))

        targetAttributeStore.writeLayerAttributes(to, targetHeader, metadata, keyBounds, keyIndex, writerSchema)

        // Move all the elements
        val targetLayerPath = Filesystem.ensureDirectory(LayerPath(targetAttributeStore.catalogPath, to))

        sourceLayerPath
          .listFiles()
          .foreach { f =>
            val target = new File(targetLayerPath, f.getName)
            Filesystem.copy(f, target)
          }

        // Clear the target cache
        targetAttributeStore.clearCache()
      }
    }

  def apply[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](catalogPath: String): LayerCopier[LayerId, K] =
    apply[K, V, M](FileAttributeStore(catalogPath))

  def apply[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](attributeStore: FileAttributeStore): LayerCopier[LayerId, K] =
    apply[K, V, M](attributeStore, attributeStore)

  def apply[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](sourceCatalogPath: String, targetCatalogPath: String): LayerCopier[LayerId, K] =
    apply[K, V, M](FileAttributeStore(sourceCatalogPath), FileAttributeStore(targetCatalogPath))
}
