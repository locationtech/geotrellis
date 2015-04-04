package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import org.apache.hadoop.fs.FileStatus
import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf.Configuration
import org.apache.spark._
import spray.json._

import scala.collection.mutable

import java.io.PrintWriter

class HadoopLayerMetaDataCatalog(hadoopConfig: Configuration, rootPath: Path, metaDataFileName: String, clobber: Boolean = true)
  extends Store[LayerId, HadoopLayerMetaData] with Logging {
  val fs = rootPath.getFileSystem(hadoopConfig)

  /** Build the catalog by recursing down directories to a point where a
    * meta data file is found. Once a metadata file is found, read the file and
    * and include the metadata into the catalog. After finding the first metadata
    * file in a directory, stops recursing down that directory.
    */
  private val catalog: mutable.Map[LayerId, HadoopLayerMetaData] = {
    val fs = rootPath.getFileSystem(hadoopConfig)
    val result = mutable.Map[LayerId, HadoopLayerMetaData]()

    def recurse(path: Path): Unit = {
      val fileStatuses = fs.listStatus(path)
      val dirs = mutable.ListBuffer[FileStatus]()
      var found = false

      for (fst <- fileStatuses) {
        if (fst.isDirectory())
          dirs += fst
        else {
          val metaDataPath = fst.getPath
          val name = metaDataPath.getName
          if(name == metaDataFileName) {
            // Read this metadata
            val txt = 
              HdfsUtils.getLineScanner(metaDataPath, hadoopConfig) match {
                case Some(in) =>
                  try {
                    in.mkString
                  }
                  finally {
                    in.close
                    found = true
                  }
                case None =>
                  throw new CatalogError("Metadata file at $metaDataPath cannot be read.")
              }

            val md = txt.parseJson.convertTo[HadoopLayerMetaData]

            result(md.layerId) = md

          }

        }
      }

      if(!found) {
        dirs.foreach { fst => recurse(fst.getPath) }
      }
    }

    recurse(rootPath)
    result
  }

  def write(id: LayerId, metaData: HadoopLayerMetaData): Unit = {
    val path = new Path(metaData.path, metaDataFileName)
    logDebug(s"Saving ${id} to $path")

    if(fs.exists(path)) {
      if(clobber)
        fs.delete(path, false)
      else
        sys.error(s"Metadata at $path already exists")
    }

    val fdos = fs.create(path)
    val out = new PrintWriter(fdos)
    try {
      out.println(metaData.toJson)
    } finally {
      out.close()
      fdos.close()
    }
  }

  def read(layerId: LayerId): HadoopLayerMetaData = {
    catalog.get(layerId) match {
      case Some(md) => md
      case None =>
        throw new LayerNotFoundError(layerId)
    }
  }
}
