package geotrellis.spark.rdd

import geotrellis.spark.SharedSparkContext
import geotrellis.spark.TestEnvironment
import geotrellis.spark.metadata.MetadataMatcher
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.testfiles.AllOnes

import org.apache.hadoop.fs.Path
import org.scalatest._
import org.scalatest.FunSpec

class SaveRasterSpec
  extends FunSpec
  with TestEnvironment
  with SharedSparkContext
  with MetadataMatcher {

  describe("Passing Context and Partitioner through operations tests") {

    val allOnes = AllOnes(inputHome, conf)

    it("should produce the expected PyramidMetadata and TileIdPartitioner") {
      val ones = RasterRDD(allOnes.path, sc)
      val twos = ones + ones
      val twosPath = new Path(outputLocal, ones.opCtx.zoom.toString)
      twos.save(twosPath)
      
      // compare metadata
      val newMeta = PyramidMetadata(outputLocal, conf)
      shouldBe(allOnes.meta, newMeta)
    }
  }
}
