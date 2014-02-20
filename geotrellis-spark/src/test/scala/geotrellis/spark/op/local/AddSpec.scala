package geotrellis.spark.op.local
import geotrellis.spark.TestEnvironment
import geotrellis.spark.rdd.RasterHadoopRDD
import org.apache.hadoop.fs.Path
import org.scalatest.matchers.ShouldMatchers
import org.apache.spark.SparkContext
import geotrellis.spark.utils.SparkUtils
import org.scalatest.fixture.FunSpec
import geotrellis.spark.TestEnvironmentFixture
import org.scalatest.Ignore

class AddSpec extends TestEnvironmentFixture with ShouldMatchers {

  describe("Add Operation") {
    val onesPath = new Path(inputHome, "all-ones/10")

    it("should add a constant to a raster") { sc =>

      val ones = RasterHadoopRDD.toRasterRDD(onesPath, sc)

      val d = 1
      val twos = ones + d

      // TODO - test doesn't have to save all twos
      /*val allTwos = new Path(outputLocal, "10")
      println(s"allTwos=$allTwos, and outputLocal=${outputLocal.toUri().toString()}")
      allTwosRDD.save(allTwos)*/

      val res = twos.map { case (tileId, raster) => raster.findMinMaxDouble }.collect
      res.foreach(_ should be(2, 2))
      res.length should be(ones.count)

    }

    it("should add multiple rasters") { sc =>

      val left = RasterHadoopRDD.toRasterRDD(onesPath, sc)

      val threes = left + left + left 

      val res = threes.map { case (tileId, raster) => raster.findMinMaxDouble }.collect
      res.foreach(_ should be(3, 3))
      res.length should be(left.count)
    }
  }
}