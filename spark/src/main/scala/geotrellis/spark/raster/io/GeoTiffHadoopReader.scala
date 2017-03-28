package geotrellis.spark.raster.io

import geotrellis.raster.io.geotiff.{MultibandGeoTiff, SinglebandGeoTiff}
import geotrellis.raster.io.geotiff.reader._
import geotrellis.vector.Extent

import org.apache.commons.io.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

object GeoTiffHadoopReader {
  def readSingleband(path: Path)(implicit sc: SparkContext): SinglebandGeoTiff = readSingleband(path, decompress = true, streaming = false, None, sc.hadoopConfiguration)
  def readSingleband(path: Path, decompress: Boolean, streaming: Boolean, extent: Option[Extent], conf: Configuration): SinglebandGeoTiff = {
    HadoopRWMethods.read(path, conf) { is =>
      val geoTiff = GeoTiffReader.readSingleband(IOUtils.toByteArray(is), decompress, streaming)
      extent match {
        case Some(e) => geoTiff.crop(e)
        case _ => geoTiff
      }
    }
  }

  def readMultiband(path: Path)(implicit sc: SparkContext): MultibandGeoTiff = readMultiband(path, decompress = true, streaming = false, None, sc.hadoopConfiguration)
  def readMultiband(path: Path, decompress: Boolean, streaming: Boolean, extent: Option[Extent], conf: Configuration): MultibandGeoTiff = {
    HadoopRWMethods.read(path, conf) { is =>
      val geoTiff = GeoTiffReader.readMultiband(IOUtils.toByteArray(is), decompress, streaming)
      extent match {
        case Some(e) => geoTiff.crop(e)
        case _ => geoTiff
      }
    }
  }
}
