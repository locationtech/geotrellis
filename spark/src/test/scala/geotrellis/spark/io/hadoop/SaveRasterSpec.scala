package geotrellis.spark.io.hadoop

import geotrellis.spark._
<<<<<<< HEAD
import geotrellis.spark.testfiles.AllOnes
=======
import geotrellis.spark.rdd._
import geotrellis.spark.testfiles.AllOnesTestFile
import geotrellis.spark.op.local._
>>>>>>> upstream/master

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
      val allOnes = AllOnesTestFile(inputHome, conf)

      it("should produce the expected metadata") {
        val ones = sc.hadoopRasterRDD(allOnes.path)
        val twos = ones //+ ones TODO uncomment this
        val twosPath = new Path(outputLocal, "twos/" + twos.metaData.level.id.toString)
        twos.saveAsHadoopRasterRDD(twosPath)

        // compare metadata
        val newMetaData = HadoopUtils.readLayerMetaData(twosPath, conf)
        newMetaData should be (allOnes.metaData)

        // compare tiles
        val rdd = sc.hadoopRasterRDD(twosPath)
        val actualTiles = rdd.collect
        val expectedTiles = twos.collect
      }
    }
  }
}
