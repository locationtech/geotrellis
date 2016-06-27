package geotrellis.raster.io.geotiff

import geotrellis.util._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.testkit._

import org.scalatest._

object SourceByteBuffers extends GeoTiffTestUtils {

  val singlebandStriped = ???
  val singlebandTiled = ???
  val multibandStriped = ???
  val multibandTiled = ???
}
/*

class ArraySegmentBytesSpec extends FunSpec 
  with Matchers
  with BeforeAndAfterAll
  with RasterMatchers
  with TileBuilders {

  val arraySegmentBytesMock = mock[ArraySegmentBytes]

  describe("Reading into the ArraySegmentBytes") {
    it("Should be able to read a singleband, striped GeoTiff") {
      val tiffTags = TiffTagsReader.read(SourceByteBuffers.singlebandStriped)
      val geoTiff = SinglebandGeoTiff(SourceByteBuffers.singlebandStriped)
      val actual = geoTiff.imageData.compressedBytes

      (arraySegmentBytesMock.size).expects(actual.size)
    }
  }
}
*/
