package geotrellis.spark

import geotrellis.spark.utils._

/** This is a test you can run to make sure a 
  * GeoTrelis cluster is up and running 
  * 
  * run spark://HOST:PORT
  */
object TestCluster {
  def main(args: Array[String]): Unit = {
    if(args.length != 1) {
      println("Please run with only one argument, the spark master URI")
    }

    val sc = SparkUtils.createSparkContext(args(0), "test-cluster")

  }
}
