package geotrellis.spark.raster.io

import geotrellis.raster.render.Jpg

import org.apache.commons.io.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

object JpgHadoopReader {
  def read(path: Path)(implicit sc: SparkContext): Jpg = read(path, sc.hadoopConfiguration)
  def read(path: Path, conf: Configuration): Jpg = HadoopRWMethods.read(path, conf) { is => Jpg(IOUtils.toByteArray(is)) }
}
