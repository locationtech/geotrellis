package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.raster.io.Filesystem

import org.apache.commons.io.filefilter.WildcardFileFilter
import spray.json._
import DefaultJsonProtocol._

import java.io._

/**
 * Stores and retrieves layer attributes from the file system.
 * 
 * @param catalogPath      The directory of the base catalog
 */
class FileAttributeStore(val catalogPath: String) extends AttributeStore[JsonFormat] {
  val SEP = "__.__"

  val attributeDirectory = new File(catalogPath, "attributes")
  if(!attributeDirectory.exists)
    attributeDirectory.mkdirs()

  def attributeFile(layerId: LayerId, attributeName: String): File =
    new File(attributeDirectory, s"${layerId.name}${SEP}${layerId.zoom}${SEP}${attributeName}.json")

  def attributeFiles(layerId: LayerId): Seq[(String, File)] =
    layerAttributeFiles(layerId)
      .map { f =>
        val att = f.getName.split(SEP).last.replace(".json", "")
        (att.substring(0, att.length - 5), f)
      }
  
  def read[T: Format](file: File): (LayerId, T) =
    Filesystem.readText(file)
      .parseJson
      .convertTo[(LayerId, T)]

  def read[T: Format](layerId: LayerId, attributeName: String): T = {
    val file = attributeFile(layerId, attributeName)

    if(!file.exists)
      throw new AttributeNotFoundError(attributeName, layerId)

    read[T](attributeFile(layerId, attributeName))._2
  }

  def readAll[T: Format](attributeName: String): Map[LayerId, T] =
    attributeDirectory
      .listFiles(new WildcardFileFilter(s"*${SEP}${attributeName}.json"): FileFilter)
      .map(read[T])
      .toMap

  def write[T: Format](layerId: LayerId, attributeName: String, value: T): Unit = {
    val f = attributeFile(layerId, attributeName)
    val text = (layerId, value).toJson.compactPrint
    Filesystem.writeText(f.getAbsolutePath, text)
  }

  def layerAttributeFiles(layerId: LayerId): Seq[File] =
    attributeDirectory
      .listFiles(new WildcardFileFilter(s"${layerId.name}${SEP}${layerId.zoom}${SEP}*.json"): FileFilter)

  def layerExists(layerId: LayerId): Boolean =
    !layerAttributeFiles(layerId)
      .isEmpty

  def delete(layerId: LayerId, attributeName: String): Unit = {
    val layerFiles = 
      attributeDirectory
        .listFiles(new WildcardFileFilter(s"${layerId.name}${SEP}${layerId.zoom}${SEP}*.json"): FileFilter)
    if(layerFiles.isEmpty) throw new LayerNotFoundError(layerId)
    layerFiles.find(f => f.getAbsolutePath.endsWith(s"${SEP}${attributeName}.json")) match {
      case Some(f) => f.delete()
      case _ =>
    }
  }

  def delete(layerId: LayerId): Unit = {
    val layerFiles = 
      attributeDirectory
        .listFiles(new WildcardFileFilter(s"${layerId.name}${SEP}${layerId.zoom}${SEP}*.json"): FileFilter)
    if(layerFiles.isEmpty) throw new LayerNotFoundError(layerId)
    layerFiles.foreach { f => f.delete() }
  }

  def layerIds: Seq[LayerId] =
    attributeDirectory
      .listFiles(new WildcardFileFilter(s"*.json"): FileFilter)
      .map { f =>
        val List(name, zoomStr) = f.getName.split(SEP).take(2).toList
        LayerId(name, zoomStr.toInt)
      }
      .distinct
}

object FileAttributeStore {
  def apply(catalogPath: String) =
    new FileAttributeStore(catalogPath)
}
