package geotrellis.spark.join

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.partition._
import geotrellis.proj4._
import geotrellis.raster._

import geotrellis.vector._
import org.apache.spark._
import org.apache.spark.rdd._
import org.scalatest._

class VectorJoinSpec extends FunSpec with Matchers with TestEnvironment {

  it("Joins two RDDs of Geometries") {
    val polyA = Polygon(
      Line(Point(0,0), Point(1,0), Point(1,1), Point(0,1), Point(0,0)))
    val polyB = Polygon(
      List(Point(10,10), Point(11,10), Point(11,11), Point(10,11), Point(10,10)))
    val line = Line(Point(0,0), Point(5,5))

    val left: RDD[Polygon] = sc.parallelize(Array(polyA, polyB))
    val right: RDD[Line] = sc.parallelize(Array(line))
      // TODO: Actually TileLayout is too specific, we don't need to know anything about pixels
      // We only need to know how to partition space

    val res: Vector[(Polygon, Line)] = VectorJoin(
      left, right,
      LayoutDefinition(LatLng.worldExtent, TileLayout(10,10, 256, 256)),
      { (left: Geometry, right: Geometry) => left intersects right}
    ).collect.toVector

    res should contain only ((polyA, line))
  }
}
