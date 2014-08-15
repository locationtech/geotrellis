package geotrellis.spark.rdd

import geotrellis.spark._
import geotrellis.spark.rdd._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.testfiles.AllOnes

import org.apache.hadoop.fs.Path
import org.scalatest._

class SaveRasterSpec
  extends FunSpec
     with Matchers
     with TestEnvironment
     with SharedSparkContext
     with OnlyIfCanRunSpark {
  describe("Passing Context and Partitioner through operations tests") {
    ifCanRunSpark {
      val allOnes = AllOnes(inputHome, conf)

      it("should produce the expected PyramidMetadata and TileIdPartitioner") {
        val ones = sc.hadoopRasterRDD(allOnes.path)
        val twos = ones + ones
        val twosPath = new Path(outputLocal, "twos/" + twos.metaData.zoomLevel.level.toString)
        twos.saveAsHadoopRasterRDD(twosPath)
        
        // compare metadata
        val newMetaData = HadoopUtils.readLayerMetaData(outputLocal, conf)
        newMetaData should be (allOnes.metaData)

        // compare tiles
        val rdd = sc.hadoopRasterRDD(outputLocal)
        val actualTiles = rdd.collect
        val expectedTiles = twos.collect
      }
    }
  }
}
