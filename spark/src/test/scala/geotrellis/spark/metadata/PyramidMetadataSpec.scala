package geotrellis.spark.metadata
import geotrellis.Extent
import geotrellis.spark.tiling.PixelExtent
import geotrellis.spark.tiling.TileExtent
import geotrellis.spark.utils.SparkUtils
import org.apache.hadoop.fs.Path
import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers
import java.awt.image.DataBuffer
import geotrellis.spark.TestEnvironment


class PyramidMetadataSpec extends TestEnvironment with MustMatchers with ShouldMatchers {

  val conf = SparkUtils.createHadoopConfiguration

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

      val pyramidPath = new Path(testLocalDir)
      meta.save(pyramidPath, conf)

      val newMeta = PyramidMetadata(pyramidPath, conf)
      
      meta should be(newMeta)
    }

  }
}