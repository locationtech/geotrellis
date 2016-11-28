package geotrellis.spark

import java.io.File

import org.apache.hadoop.fs.Path
import org.scalatest.Suite

trait PointsTestEnvironment extends TestEnvironment { self: Suite =>
  val testResources = new File("src/test/resources")
  val lasPath = new Path(s"file://${testResources.getAbsolutePath}/las")
}
