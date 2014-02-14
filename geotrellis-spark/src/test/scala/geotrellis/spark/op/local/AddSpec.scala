package geotrellis.spark.op.local
import geotrellis.spark.SparkEnvironment

import geotrellis.spark.TestEnvironment
import geotrellis.spark.rdd.RasterHadoopRDD

import org.apache.hadoop.fs.Path
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

class AddSpec extends TestEnvironment with MustMatchers with ShouldMatchers with SparkEnvironment {
  describe("Add one to all-ones") {

    val allOnes = new Path(inputHome, "all-ones/10")
    val allOnesRDD = RasterHadoopRDD.toRasterRDD(allOnes, sc)

    val d = 1
    val allTwosRDD = allOnesRDD + d

    // TODO - test doesn't have to save all twos
    val allTwos = new Path(outputLocal, "10")    
    println(s"allTwos=$allTwos, and outputLocal=${outputLocal.toUri().toString()}")
    allTwosRDD.save(allTwos)

    it("should be all twos") {

      //println(allTwosRDD.map { case (tileId, raster) => raster.findMinMaxDouble }.collect.mkString("\n"))
      allTwosRDD.map { case (tileId, raster) => raster.findMinMaxDouble }.collect.foreach(_ should be(2, 2))

    sc.stop
    }
  }
}