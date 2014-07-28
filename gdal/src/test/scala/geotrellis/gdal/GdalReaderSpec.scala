package geotrellis.gdal

import geotrellis.raster._
import geotrellis.raster.io.GeoTiff

import org.scalatest._

class GdalReaderSpec extends FunSpec with Matchers {
  val path = "raster-test/data/slope.tif"

  describe("reading a GeoTiff") {
    it("should match one read with GeoTools") {
      println("Reading with GDAL...")
      val (gdalRaster, gdalRasterExtent) = GdalReader.read(path)
      println("Reading with GeoTools....")
      val (geotoolsRaster, geotoolsRasterExtent) = GeoTiff.readRaster(path)
      println("Done.")

      val gdRe = gdalRasterExtent
      val gtRe = geotoolsRasterExtent

      val gdExt = gdRe.extent
      val gtExt = gtRe.extent

      gdExt.xmin should be (gtExt.xmin +- 0.00001)
      gdExt.xmax should be (gtExt.xmax +- 0.00001)
      gdExt.ymin should be (gtExt.ymin +- 0.00001)
      gdExt.ymax should be (gtExt.ymax +- 0.00001)

      gdRe.cols should be (gtRe.cols)
      gdRe.rows should be (gtRe.rows)

      gdalRaster.cellType should be (geotoolsRaster.cellType)

      println("Comparing rasters...")
      for(col <- 0 until gdRe.cols) {
        for(row <- 0 until gdRe.rows) {
          val actual = gdalRaster.getDouble(col, row)
          val expected = geotoolsRaster.getDouble(col, row)
          withClue(s"At ($col, $row): GDAL - $actual  GeoTools - $expected") {
            isNoData(actual) should be (isNoData(expected))
            if(isData(actual))
              actual should be (expected)
          }
        }
      }
    }
  }
}
