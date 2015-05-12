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

class HadoopAttributeStore(hadoopConfiguration: Configuration, attributeDir: Path) extends AttributeStore {
  type ReadableWritable[T] = RootJsonFormat[T]

  val fs = attributeDir.getFileSystem(hadoopConfiguration)

  // Create directory if it doesn't exist
  if(!fs.exists(attributeDir)) {
    fs.mkdirs(attributeDir)
  }

  def attributePath(layerId: LayerId, attributeName: String): Path = {
    val fname = s"${layerId.name}___${layerId.zoom}___${attributeName}.json"    
    new Path(attributeDir, fname)
  }

  def attributeWildcard(attributeName: String): Path = 
    new Path(s"*___${attributeName}.json")


  private def readFile[T: ReadableWritable](path: Path): Option[(LayerId, T)] = {
    HdfsUtils
      .getLineScanner(path, hadoopConfiguration)
      .map{ in =>  
        val txt = 
          try {
            in.mkString
          }
          finally {
            in.close
          }
        txt.parseJson.convertTo[(LayerId, T)]
      }
  }

  def read[T: ReadableWritable](layerId: LayerId, attributeName: String): T =
    readFile[T](attributePath(layerId, attributeName)) match {
      case Some((id, value)) => value
      case None => throw new LayerNotFoundError(layerId)
    }

  def readAll[T: RootJsonFormat](attributeName: String): Map[LayerId,T] = {
    HdfsUtils
      .listFiles( attributeWildcard(attributeName), hadoopConfiguration)    
      .map{ path: Path => 
        readFile[T](path) match {
          case Some(tup) => tup
          case None => sys.error(s"Unable to read '$attributeName' attribute from $path")
        }
      }
      .toMap
  }

  def write[T: ReadableWritable](layerId: LayerId, attributeName: String, value: T): Unit = {
    val path = attributePath(layerId, attributeName)

    if(fs.exists(path)) {
      fs.delete(path, false)
    }

    val fdos = fs.create(path)
    val out = new PrintWriter(fdos)
    try {
      val s = (layerId, value).toJson.toString
      out.println(s)
    } finally {
      out.close()
      fdos.close()
    }
  }
}
