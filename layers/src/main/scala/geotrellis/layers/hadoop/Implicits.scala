package geotrellis.layers.hadoop

import geotrellis.raster.CellGrid
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.render.{Jpg, Png}
import geotrellis.util.MethodExtensions

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce._


object Implicits extends Implicits

trait Implicits {
  implicit class withJpgHadoopWriteMethods(val self: Jpg) extends JpgHadoopWriteMethods(self)

  implicit class withPngHadoopWriteMethods(val self: Png) extends PngHadoopWriteMethods(self)

  implicit class withGeoTiffHadoopWriteMethods[T <: CellGrid[Int]](val self: GeoTiff[T]) extends GeoTiffHadoopWriteMethods[T](self)

  implicit class withHadoopConfigurationMethods(val self: Configuration) extends MethodExtensions[Configuration] {
    def modify(f: Job => Unit): Configuration = {
      val job = Job.getInstance(self)
      f(job)
      job.getConfiguration
    }

    def withInputPath(path: Path): Configuration =
      modify(FileInputFormat.addInputPath(_, path))

    /** Creates a Configuration with all files in a directory (recursively searched)*/
    def withInputDirectory(path: Path): Configuration = {
      val allFiles = HdfsUtils.listFiles(path, self)
      if(allFiles.isEmpty) {
        sys.error(s"$path contains no files.")
      }
      HdfsUtils.putFilesInConf(allFiles.mkString(","), self)
    }

    /** Creates a configuration with a given directory, to search for all files
      * with an extension contained in the given set of extensions */
    def withInputDirectory(path: Path, extensions: Seq[String]): Configuration = {
      val searchPath = path.toString match {
        case p if extensions.exists(p.endsWith) => path
        case p =>
          val extensionsStr = extensions.mkString("{", ",", "}")
          new Path(s"$p/*$extensionsStr")
      }

      withInputDirectory(searchPath)
    }
  }
}
