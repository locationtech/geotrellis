package geotrellis.spark.io.hadoop.geotiff

import geotrellis.spark.io.hadoop.{HdfsRangeReader, HdfsUtils}
import geotrellis.raster.io.geotiff.reader.TiffTagsReader

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import java.net.URI

object HadoopGeoTiffInput {
  /**
    *
    * Returns a list of URIs matching given regexp
    *
    * @name - group name
    * @uri - regexp
    * @conf - hadoopConfiguration
    */
  def list(name: String, uri: URI, conf: Configuration): List[GeoTiffMetadata] = {
    HdfsUtils
      .listFiles(new Path(uri), conf)
      .map { p =>
        val tiffTags = TiffTagsReader.read(HdfsRangeReader(p, conf))
        GeoTiffMetadata(tiffTags.extent, tiffTags.crs, name, uri)
      }
  }
}
