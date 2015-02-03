package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import org.apache.hadoop.fs.Path
import org.apache.spark._
import spray.json._

import java.io.PrintWriter

class HadoopMetaDataCatalog(sc: SparkContext, catalogRoot: Path, layerDataDir: LayerId => String, metaDataFileName: String)
  extends MetaDataCatalog[String] with Logging {
  val fs = catalogRoot.getFileSystem(sc.hadoopConfiguration)

  def metaDataPath(layerId: LayerId, subDir: String) =
    if (subDir == "")
      new Path(new Path(catalogRoot, layerDataDir(layerId)) , metaDataFileName)
    else
      new Path(new Path(new Path(catalogRoot, subDir), layerDataDir(layerId)) , metaDataFileName)

  def load(layerId: LayerId): (LayerMetaData, String) =
    (load(layerId, ""), "")

  def load(layerId: LayerId, subDir: String): LayerMetaData = {
    val path = metaDataPath(layerId, subDir)
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

    txt.parseJson.convertTo[LayerMetaData]
  }

  def save(id: LayerId, subDir: String, metaData: LayerMetaData, clobber: Boolean): Unit = {
    val metaPath = metaDataPath(id, subDir)
    logDebug(s"Saving ${id} to $metaPath")

    if(fs.exists(metaPath)) {
      if(clobber)
        fs.delete(metaPath, false)
      else
        sys.error(s"Metadata at $metaPath already exists")
    }

    val fdos = fs.create(metaPath)
    val out = new PrintWriter(fdos)
    try {
      out.println(metaData.toJson)
    } finally {
      out.close()
      fdos.close()
    }
  }
}
