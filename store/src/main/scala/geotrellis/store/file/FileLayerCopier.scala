package geotrellis.store.file

import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.AttributeStore.Fields
import geotrellis.store.avro.AvroRecordCodec
import geotrellis.util.{Component, Filesystem}

import _root_.io.circe._

import scala.reflect.ClassTag
import java.io.File

object FileLayerCopier {
  def apply(sourceAttributeStore: FileAttributeStore, targetAttributeStore: FileAttributeStore): LayerCopier[LayerId] =
    new LayerCopier[LayerId] {
      def copy[
        K: AvroRecordCodec: Boundable: Encoder: Decoder: ClassTag,
        V: AvroRecordCodec: ClassTag,
        M: Encoder: Decoder: Component[?, Bounds[K]]
      ](from: LayerId, to: LayerId): Unit = {
        if(targetAttributeStore.layerExists(to))
          throw new LayerExistsError(to)

        val sourceMetadataFile = sourceAttributeStore.attributeFile(from, Fields.metadata)
        if(!sourceMetadataFile.exists) throw new LayerNotFoundError(from)

        // Read the metadata file out.
        val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
          sourceAttributeStore.readLayerAttributes[FileLayerHeader, M, K](from)
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

        targetAttributeStore.writeLayerAttributes(to, targetHeader, metadata, keyIndex, writerSchema)

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

  def apply(catalogPath: String): LayerCopier[LayerId] =
    apply(FileAttributeStore(catalogPath))

  def apply(attributeStore: FileAttributeStore): LayerCopier[LayerId] =
    apply(attributeStore, attributeStore)

  def apply(sourceCatalogPath: String, targetCatalogPath: String): LayerCopier[LayerId] =
    apply(FileAttributeStore(sourceCatalogPath), FileAttributeStore(targetCatalogPath))
}
