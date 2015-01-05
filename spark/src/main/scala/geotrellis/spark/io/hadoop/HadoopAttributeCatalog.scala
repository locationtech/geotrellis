package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._

import spray.json._
import org.apache.hadoop.fs.Path
import org.apache.spark._
import java.io.PrintWriter

class HadoopAttributeCatalog(sc: SparkContext, catalogRoot: Path, layerDataDir: LayerId => String, metaDataFileName: String) extends AttributeCatalog {
  val fs = catalogRoot.getFileSystem(sc.hadoopConfiguration)

  def attributePath(layerId: LayerId, attributeName: String): Path = 
    new Path(new Path(catalogRoot, layerDataDir(layerId)), s"${attributeName}.json")

  def load[T: JsonFormat](layerId: LayerId, attributeName: String): T = {
    val path = attributePath(layerId, attributeName)
    val txt = HdfsUtils.getLineScanner(path, sc.hadoopConfiguration) match {
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

  def save[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
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

  def listLayers: List[LayerId] = {
    val path = catalogRoot.suffix("/*/*/metadata.json")   
    val files = HdfsUtils.listFiles(path,  sc.hadoopConfiguration)
    files map { file =>
      val name = file.getParent.getParent.getName
      val zoom = file.getParent.getName.toInt
      LayerId(name, zoom)
    }  
  }
}
