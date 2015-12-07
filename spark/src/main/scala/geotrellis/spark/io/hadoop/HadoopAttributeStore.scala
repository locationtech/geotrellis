package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.utils._

import spray.json._
import DefaultJsonProtocol._
import org.apache.hadoop.fs.Path
import org.apache.spark._
import java.io.PrintWriter
import scala.reflect.ClassTag

import org.apache.hadoop.conf.Configuration

class HadoopAttributeStore(val hadoopConfiguration: Configuration, attributeDir: Path) extends AttributeStore[JsonFormat] {
  val fs = attributeDir.getFileSystem(hadoopConfiguration)

  // Create directory if it doesn't exist
  if(!fs.exists(attributeDir)) {
    fs.mkdirs(attributeDir)
  }

  def attributePath(layerId: LayerId, attributeName: String): Path = {
    val fname = s"${layerId.name}___${layerId.zoom}___${attributeName}.json"    
    new Path(attributeDir, fname)
  }

  private def _delete(layerId: LayerId, path: Path): Unit = {
    if(!layerExists(layerId)) throw new LayerNotFoundError(layerId)
    HdfsUtils
      .listFiles(new Path(attributeDir, path), hadoopConfiguration)
      .foreach(fs.delete(_, false))
  }

  private def _processFiles(from: LayerId, to: LayerId)(func: (Path, Path, Configuration) => Unit): Unit = {
    if(!layerExists(from)) throw new LayerNotFoundError(from)
    if(layerExists(to)) throw new LayerExistsError(to)
    val pattern = new Path(s"${from.name}___${from.zoom}___*.json")

    HdfsUtils
      .listFiles(new Path(attributeDir, pattern), hadoopConfiguration)
      .foreach { file =>
        func(
          file,
          new Path(
            attributeDir,
            file.getName
              .replace(
                s"${from.name}___${from.zoom}___",
                s"${to.name}___${to.zoom}___"
              )
          ),
          hadoopConfiguration
        )
      }
  }

  def attributeWildcard(attributeName: String): Path = 
    new Path(s"*___${attributeName}.json")

  private def readFile[T: Format](path: Path): Option[(LayerId, T)] = {
    HdfsUtils
      .getLineScanner(path, hadoopConfiguration)
      .map{ in =>  
        val txt = 
          try {
            in.mkString
          }
          finally {
            in.close()
          }
        txt.parseJson.convertTo[(LayerId, T)]
      }
  }

  def read[T: Format](layerId: LayerId, attributeName: String): T =
    readFile[T](attributePath(layerId, attributeName)) match {
      case Some((id, value)) => value
      case None => throw new AttributeNotFoundError(attributeName, layerId)
    }

  def readAll[T: Format](attributeName: String): Map[LayerId,T] = {
    HdfsUtils
      .listFiles( attributeWildcard(attributeName), hadoopConfiguration)    
      .map{ path: Path => 
        readFile[T](path) match {
          case Some(tup) => tup
          case None => throw new CatalogError(s"Unable to list $attributeName attributes from $path") 
        }
      }
      .toMap
  }

  def write[T: Format](layerId: LayerId, attributeName: String, value: T): Unit = {
    val path = attributePath(layerId, attributeName)

    if(fs.exists(path)) {
      fs.delete(path, false)
    }

    val fdos = fs.create(path)
    val out = new PrintWriter(fdos)
    try {
      val s = (layerId, value).toJson.toString()
      out.println(s)
    } finally {
      out.close()
      fdos.close()
    }
  }

  def layerExists(layerId: LayerId): Boolean = {
    val path = attributePath(layerId, AttributeStore.Fields.metaData)
    val fs = path.getFileSystem(hadoopConfiguration)
    fs.exists(path)
  }

  def delete(layerId: LayerId): Unit =
    _delete(layerId, new Path(s"${layerId.name}___${layerId.zoom}___*.json"))

  def delete(layerId: LayerId, attributeName: String): Unit =
    _delete(layerId, new Path(s"${layerId.name}___${layerId.zoom}___${attributeName}.json"))

  def copy(from: LayerId, to: LayerId): Unit = _processFiles(from, to)(HdfsUtils.copyPath)

  def move(from: LayerId, to: LayerId) = _processFiles(from, to)(HdfsUtils.renamePath)

}

object HadoopAttributeStore {
  def apply(rootPath: Path, config: Configuration): HadoopAttributeStore =
    new HadoopAttributeStore(config, rootPath)

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopAttributeStore =
    new HadoopAttributeStore(sc.hadoopConfiguration, rootPath)
}
