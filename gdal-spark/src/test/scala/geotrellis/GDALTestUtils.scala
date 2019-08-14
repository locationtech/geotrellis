package geotrellis

import java.io.File

object GDALTestUtils {
  def gdalGeoTiffPath(name: String): String = {
    def baseDataPath = "../gdal/src/test/resources"
    val path = s"$baseDataPath/$name"
    require(new File(path).exists, s"$path does not exist, unzip the archive?")
    path
  }

  def sparkGeoTiffPath(name: String): String = {
    def baseDataPath = "../spark/src/test/resources"
    val path = s"$baseDataPath/$name"
    require(new File(path).exists, s"$path does not exist, unzip the archive?")
    path
  }
}
