package geotrellis.pointcloud.spark

import geotrellis.spark._
import geotrellis.spark.testkit._
import org.apache.hadoop.fs.Path
import org.scalatest.Suite

import java.io.File

trait PointCloudTestEnvironment extends TestEnvironment { self: Suite =>
  val testResources = new File("src/test/resources")
  val lasPath = new Path(s"file://${testResources.getAbsolutePath}/las")
}
