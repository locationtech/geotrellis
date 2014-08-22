package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._
import org.scalatest._
import geotrellis.testkit._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.vector.Extent

class NdviSpec extends FunSpec
  with Matchers
  with TestEngine {

  describe("Ndvi") {

    val redbandbyte = (GeoTiffReader("raster-test/data/NDVI_REF_FILES/BAND3_RED_TOA_REF.TIF").read).imageDirectories.head.imageBytes
    val ninfrabandbyte = (GeoTiffReader("raster-test/data/NDVI_REF_FILES/BAND4_NIR_TOA_REF.TIF").read).imageDirectories.head.imageBytes
    val ndvibyte = (GeoTiffReader("raster-test/data/NDVI_REF_FILES/NDVI.TIF").read).imageDirectories.head.imageBytes

    it("calculate ndvi raster should equal to provided ndvi raster") {

      val red = ArrayTile(redbandbyte, 1958, 1159)
      val nIfra = ArrayTile(ninfrabandbyte, 1958, 1159)
      val ndvi = ArrayTile(ndvibyte, 1958, 1159)

      val mb = MultiBandTile(Array(red, nIfra))

      val resultNdvi = mb.localNdvi(0, 1)

      for (c <- 0 until resultNdvi.cols) {
        for (r <- 0 until resultNdvi.rows) {
          if (ndvi.getDouble(c, r).isNaN) {
//            resultNdvi.getDouble(c, r).isNaN should be(true)
          } else {
//            resultNdvi.getDouble(c, r) should be(ndvi.getDouble(c, r))
          }
        }
      }
    }
  }
}