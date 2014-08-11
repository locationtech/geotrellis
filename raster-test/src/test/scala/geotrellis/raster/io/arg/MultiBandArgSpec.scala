package geotrellis.raster.io.arg

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.testkit._

import org.scalatest._

class MultiBandArgSpec extends FunSpec
  with TestEngine
  with Matchers {
  describe("MultiBandArgReader and MultiBandArgWriter") {

    val extent = Extent(10.0, 11.0, 14.0, 15.0)

    val array1 = Array(NODATA, -1, 2, -3,
      4, -5, 6, -7,
      8, -9, 10, -11,
      12, -13, 14, -15)

    val tile1 = IntArrayTile(array1, 4, 4)

    val array2 = Array(NODATA, 4, -5, 6,
      -1, 2, -3, -7,
      12, -13, 14, -15,
      8, -9, 10, -11)

    val tile2 = IntArrayTile(array2, 4, 4)

    val array3 = Array(NODATA, NODATA, 2, -3,
      4, -5, 6, -7,
      8, NODATA, 10, NODATA,
      12, -13, 14, -15)

    val tile3 = IntArrayTile(array3, 4, 4)

    val array4 = Array(10, -1, 2, -3,
      4, -5, 6, -7,
      8, -9, 10, -11,
      12, -13, 14, -15)

    val tile4 = IntArrayTile(array4, 4, 4)

    val multiBandTile = MultiBandTile(Array(tile1, tile2, tile3, tile4))

    it("should write MultiBandTile and match with Read MultiBandTile") {
      MultibandArgWriter(TypeInt).write("raster-test/data/multiband-data/test-multibandtile.json", multiBandTile, extent, "test-multibandtile")
      val fromArgReader: MultiBandTile = MultibandArgReader.read("raster-test/data/multiband-data/test-multibandtile.json")

      fromArgReader should be(multiBandTile)
    }

    it("should read from metadata with a target RasterExtent") {
      val target = RasterExtent(Extent(20.0, 21.0, 24.0, 25.0), 4, 4)
      val fromArgReader: MultiBandTile = MultibandArgReader.read("raster-test/data/multiband-data/test-multibandtile.json", target)
      
      fromArgReader should be(multiBandTile.warp(extent, target))
    }
  }

}