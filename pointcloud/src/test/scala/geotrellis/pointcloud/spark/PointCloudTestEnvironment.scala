package geotrellis.pointcloud.spark

import java.io.File

import geotrellis.spark._
import geotrellis.spark.testkit._
import org.apache.hadoop.fs.Path
import org.scalatest.Suite

trait PointCloudTestEnvironment extends TestEnvironment { self: Suite =>
  val testResources = new File("src/test/resources")
  val lasPath = new Path(s"file://${testResources.getAbsolutePath}/las")
}
