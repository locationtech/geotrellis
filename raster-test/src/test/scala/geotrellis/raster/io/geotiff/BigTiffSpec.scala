package geotrellis.raster.io.geotiff

import geotrellis.util._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster._
import geotrellis.raster.testkit._

import org.scalatest._

class BigTiffSpec extends FunSpec with RasterMatchers with GeoTiffTestUtils {
  describe("Reading BigTiffs") {
    //val path2 = "raster-test/data/geotiff-test-files/bigtiffs/ls8_int32-big.tif"
    //val path = "raster-test/data/geotiff-test-files/bigtiffs/bit_striped-big.tif"
    //val path2 = "raster-test/data/geotiff-test-files/bigtiffs/all-ones-big.tif"
    //val path2 = "raster-test/data/geotiff-test-files/bigtiffs/alaska-big.tif"
    val path2 = "raster-test/data/geotiff-test-files/bigtiffs/green-1-big.tif"

    val path = "raster-test/data/geotiff-test-files/ls8_int32.tif"
    val local = LocalBytesStreamer(path2, 25600000)
    val reader = StreamByteReader(local)
    println("\nNot Big")
    println(TiffTagsReader.read(path2))
    println("\nBig")
    println(TiffTagsReader.read(reader))
  }
}
