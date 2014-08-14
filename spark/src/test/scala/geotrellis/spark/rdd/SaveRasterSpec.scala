package geotrellis.spark.rdd

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.metadata._
import geotrellis.spark.testfiles.AllOnes

import org.apache.hadoop.fs.Path
import org.scalatest._
import org.scalatest.FunSpec

class SaveRasterSpec
  extends FunSpec
  with TestEnvironment
  with SharedSparkContext
  with MetadataMatcher 
  with OnlyIfCanRunSpark {
  describe("Passing Context and Partitioner through operations tests") {
    ifCanRunSpark {
      val allOnes = AllOnes(inputHome, conf)

      it("should produce the expected PyramidMetadata and TileIdPartitioner") {
        val ones = sc.hadoopRasterRDD(allOnes.path)
        val twos = ones + ones
        val twosPath = new Path(outputLocal, ones.opCtx.zoom.toString)
        twos.saveAsHadoopRasterRDD(twosPath)
        
        // compare metadata
        val newMeta = PyramidMetadata(outputLocal, conf)
        shouldBe(allOnes.meta, newMeta)
      }
    }
  }
}
