package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._

import spray.json._
import org.apache.hadoop.fs.Path
import org.apache.spark._
import java.io.PrintWriter

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

  def read[T: RootJsonFormat](layerId: LayerId, attributeName: String): T = {
    val path = attributePath(layerId, attributeName)

    val txt = HdfsUtils.getLineScanner(path, hadoopConfiguration) match {
      case Some(in) =>
        try {
          in.mkString
        }
        finally {
          in.close
        }
      case None =>
        throw new LayerNotFoundError(layerId)
    }

    txt.parseJson.convertTo[T]

  }

  def write[T: RootJsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
    val path = attributePath(layerId, attributeName)

    if(fs.exists(path)) {
      fs.delete(path, false)
    }

    val fdos = fs.create(path)
    val out = new PrintWriter(fdos)
    try {
      out.println(value.toJson)
    } finally {
      out.close()
      fdos.close()
    }
  }
}
