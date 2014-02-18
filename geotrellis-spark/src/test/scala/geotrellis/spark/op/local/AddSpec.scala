package geotrellis.spark.op.local
import geotrellis.spark.TestEnvironment
import geotrellis.spark.rdd.RasterHadoopRDD
import org.apache.hadoop.fs.Path
import org.scalatest.matchers.ShouldMatchers
import org.apache.spark.SparkContext
import geotrellis.spark.utils.SparkUtils
import org.scalatest.fixture.FunSpec
import geotrellis.spark.TestEnvironmentFixture

class AddSpec extends TestEnvironmentFixture with ShouldMatchers {

  describe("Add Operation") {
    it("should add a constant to a raster") { sc =>

      val allOnes = new Path(inputHome, "all-ones/10")
      val allOnesRDD = RasterHadoopRDD.toRasterRDD(allOnes, sc)

      val d = 1
      val allTwosRDD = allOnesRDD + d

      // TODO - test doesn't have to save all twos
      /*val allTwos = new Path(outputLocal, "10")
      println(s"allTwos=$allTwos, and outputLocal=${outputLocal.toUri().toString()}")
      allTwosRDD.save(allTwos)*/

      allTwosRDD.map { case (tileId, raster) => raster.findMinMaxDouble }.collect.foreach(_ should be(2, 2))

    }
  }
}