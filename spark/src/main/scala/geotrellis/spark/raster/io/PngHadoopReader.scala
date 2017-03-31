package geotrellis.spark.raster.io

import geotrellis.raster.render.Png

import org.apache.commons.io.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

object PngHadoopReader {
  def read(path: Path)(implicit sc: SparkContext): Png = read(path, sc.hadoopConfiguration)
  def read(path: Path, conf: Configuration): Png = HadoopRWMethods.read(path, conf) { is => Png(IOUtils.toByteArray(is)) }
}
