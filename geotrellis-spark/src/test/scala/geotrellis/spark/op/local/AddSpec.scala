package geotrellis.spark.op.local
import geotrellis.spark.SparkEnvironment
import geotrellis.spark.TestEnvironment
import geotrellis.spark.rdd.RasterHadoopRDD

import org.apache.hadoop.fs.Path
import org.scalatest.matchers.ShouldMatchers

class AddSpec extends TestEnvironment with ShouldMatchers with SparkEnvironment {
  
  // TODO - figure out how to support clauses. Right now, the check has to be done in 
  // the top level of sparkTest and not inside "it" clauses as is done in BDD style tests
  sparkTest("Add Operation") {

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