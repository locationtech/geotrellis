package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._
import geotrellis.util.Filesystem
import AttributeStore.Fields

import org.apache.spark.SparkContext
import spray.json.JsonFormat
import org.apache.avro.Schema

import scala.reflect.ClassTag
import java.io.File


object FileLayerReindexer {
  def apply(attributeStore: FileAttributeStore)(implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val layerReader  = FileLayerReader(attributeStore)
    val layerWriter  = FileLayerWriter(attributeStore)
    val layerDeleter = FileLayerDeleter(attributeStore)
    val layerCopier  = FileLayerCopier(attributeStore)

    GenericLayerReindexer[FileLayerHeader](attributeStore, layerReader, layerWriter, layerDeleter, layerCopier)
  }

  def apply(catalogPath: String)(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply(FileAttributeStore(catalogPath))

}
