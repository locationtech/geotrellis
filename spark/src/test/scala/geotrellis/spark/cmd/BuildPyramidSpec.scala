package geotrellis.spark.cmd
import org.apache.hadoop.fs.Path
import org.scalatest.FunSpec
import geotrellis.spark.TestEnvironment
import org.scalatest.matchers.ShouldMatchers
import org.apache.hadoop.fs.FileUtil
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.rdd.TileIdPartitioner
import geotrellis.spark.storage.RasterReader
import geotrellis.spark.tiling.TmsTiling
import org.apache.hadoop.io.SequenceFile

class BuildPyramidSpec extends FunSpec with TestEnvironment with RasterVerifyMethods {

  describe("Build Pyramid") {
    val allOnesOrig = new Path(inputHome, "all-ones")
    val allOnes = new Path(outputLocal, "all-ones")

    // copy over the all-ones base raster from the test directory
    FileUtil.copy(localFS, allOnesOrig, localFS, allOnes.getParent(),
      false /* deleteSource */ ,
      true /* overwrite */ ,
      conf)

    // build the pyramid
    val cmd = s"--pyramid ${allOnes.toString} --sparkMaster local"
    BuildPyramid.main(cmd.split(' '))

    val meta = PyramidMetadata(allOnes, conf)

    it("should create the correct metadata") {

      // first lets make sure that the base attributes (everything but the zoom levels in rasterMetadata) 
      // has not changed
      val actualMetaBase = meta.copy(
        rasterMetadata = meta.rasterMetadata.filterKeys(_ == meta.maxZoomLevel.toString))
      val expectedMetaBase = PyramidMetadata(allOnesOrig, conf)
      actualMetaBase should be(expectedMetaBase)

      // now lets make sure that the metadata has all the zoom levels in the rasterMetadata
      meta.rasterMetadata.keys.toList.map(_.toInt).sorted should be((1 to meta.maxZoomLevel).toList)
    }

    it("should have the right zoom level directory") {
      for (z <- 1 until meta.maxZoomLevel) {
        verifyZoomLevelDirectory(new Path(allOnes, z.toString))
      }
    }

    it("should have the right number of splits for each of the zoom levels") {
      for (z <- 1 until meta.maxZoomLevel) {
        verifyPartitions(new Path(allOnes, z.toString))
      }
    }

    it("should have the correct tiles (checking tileIds)") {
      for (z <- 1 until meta.maxZoomLevel) {
        verifyTiles(new Path(allOnes, z.toString), meta)
      }
    }

    it("should have its data files compressed") {
      for (z <- 1 until meta.maxZoomLevel) {
        verifyCompression(new Path(allOnes, z.toString))
      }
    }

    it("should have its block size set correctly") {
      for (z <- 1 until meta.maxZoomLevel) {
        verifyBlockSize(new Path(allOnes, z.toString))
      }
    }

  }
}