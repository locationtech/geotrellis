package geotrellis.spark.sql

import geotrellis.spark.TestEnvironment
import org.apache.spark.sql.SparkSession
import org.scalatest.Suite

trait SqlTestEnvironment extends TestEnvironment with KryoEncoderImplicits { self: Suite =>
  lazy val ssc: SparkSession =
    SparkSession
      .builder()
      .master(_sc.master) // to be sure that spark context was initiated
      .appName("Test SQL Context")
      .getOrCreate()
}
