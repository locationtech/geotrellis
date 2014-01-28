package geotrellis.spark.metadata
import geotrellis.spark.tiling.Bounds
import geotrellis.spark.tiling.PixelBounds
import geotrellis.spark.tiling.TileBounds
import geotrellis.spark.utils.SparkUtils

import org.apache.hadoop.fs.Path
import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

import java.awt.image.DataBuffer

class PyramidMetadataSpec extends FunSpec with MustMatchers with ShouldMatchers {

  val conf = SparkUtils.createHadoopConfiguration

  describe("PyramidMetadata.save") {

    it("should correctly save and read the metadata") {
      val meta = PyramidMetadata(
        Bounds(1, 1, 1, 1),
        512,
        1,
        Double.NaN,
        DataBuffer.TYPE_FLOAT,
        10,
        Map("1" -> new RasterMetadata(PixelBounds(0, 0, 0, 0), TileBounds(0, 0, 0, 0))))

      val pyramidPath = new Path(java.nio.file.Files.createTempDirectory("pyramid_metadata_save_test").toUri())

      meta.save(pyramidPath, conf)

      val newMeta = PyramidMetadata(pyramidPath, conf)
      
      meta should be(newMeta)
    }

  }
}