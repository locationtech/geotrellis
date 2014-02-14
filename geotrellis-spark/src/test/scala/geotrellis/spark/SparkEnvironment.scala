package geotrellis.spark

import geotrellis.spark.utils.SparkUtils

trait SparkEnvironment {

  val sparkMaster = "local"

  val sc = SparkUtils.createSparkContext(sparkMaster, this.getClass.getName)
  def stop = sc.stop
}