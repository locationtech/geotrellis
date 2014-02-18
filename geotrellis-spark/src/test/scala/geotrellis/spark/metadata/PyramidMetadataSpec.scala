package geotrellis.spark.metadata
import geotrellis.Extent
import geotrellis.spark.TestEnvironment
import geotrellis.spark.tiling.PixelExtent
import geotrellis.spark.tiling.TileExtent
import org.scalatest.matchers.ShouldMatchers
import java.awt.image.DataBuffer
import org.scalatest.FunSpec


class PyramidMetadataSpec extends TestEnvironment with ShouldMatchers {

  describe("PyramidMetadata.save") {

    it("should correctly save and read the metadata") {
      val meta = PyramidMetadata(
        Extent(1, 1, 1, 1),
        512,
        1,
        Double.NaN,
        DataBuffer.TYPE_FLOAT,
        10,
        Map("1" -> new RasterMetadata(PixelExtent(0, 0, 0, 0), TileExtent(0, 0, 0, 0))))

      meta.save(outputLocal, conf)

      val newMeta = PyramidMetadata(outputLocal, conf)
      
      meta should be(newMeta)
    }

  }
}