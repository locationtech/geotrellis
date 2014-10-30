package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop.json._

import org.apache.hadoop.fs.Path
import org.apache.spark._
import spray.json._

import java.io.PrintWriter
import scala.util.Try

class HadoopMetaDataCatalog(sc: SparkContext, metaDataFolder: Path) extends MetaDataCatalog[Path] {
  def metaDataPath(layerId: LayerId) = new Path(metaDataFolder, s"${layerId.name}_${layerId.zoom}_metadata.json")

  def load(layerId: LayerId): Try[(LayerMetaData, Path)] = 
    Try {
      val path = metaDataPath(layerId)
      val txt = HdfsUtils.getLineScanner(path, sc.hadoopConfiguration) match {
        case Some(in) =>
          try {
            in.mkString
          }
          finally {
            in.close
          }
        case None =>
          sys.error(s"Metadata file for layer $layerId not found at ${path.toUri.toString}")
      }
      txt.parseJson.convertTo[(LayerMetaData, Path)]
      }

  def save(layerMetaData: LayerMetaData, path: Path): Try[Unit] = 
    Try {
      val metaPath = metaDataPath(layerMetaData.id)
      val fs = metaPath.getFileSystem(sc.hadoopConfiguration)
      val fdos = fs.create(metaPath)
      val out = new PrintWriter(fdos)
      try {
        out.println((layerMetaData, path).toJson)
      } finally {
        out.close()
        fdos.close()
      }
    }
}

object HadoopMetaDataCatalog {
  final val METADATA_DIR = "metadata"
}
