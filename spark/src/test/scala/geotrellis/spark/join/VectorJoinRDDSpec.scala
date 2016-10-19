package geotrellis.spark.join

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.partition._
import geotrellis.spark.tiling._
import geotrellis.vector._
import org.apache.spark._
import org.apache.spark.rdd._
import org.scalatest._


class VectorJoinRDDSpec extends FunSpec with Matchers with TestEnvironment {

  val polyA = Polygon(
    Line(Point(0,0), Point(1,0), Point(1,1), Point(0,1), Point(0,0)))
  val polyB = Polygon(
    List(Point(10,10), Point(11,10), Point(11,11), Point(10,11), Point(10,10)))
  val line = Line(Point(0,0), Point(5,5))

  it("Joins two RDDs of Geometries") {

    val left: RDD[Polygon] = sc.parallelize(Array(polyA, polyB))
    val right: RDD[Line] = sc.parallelize(Array(line))
    val pred = { (a: Geometry, b: Geometry) => a intersects b }

    val res: Vector[(Polygon, Line)] = VectorJoin(left, right, pred).collect.toVector

    res should contain only ((polyA, line))
  }

  it("Joins two RDDs of Geometries using Implicit Methods") {

    val left: RDD[Polygon] = sc.parallelize(Array(polyA, polyB))
    val right: RDD[Line] = sc.parallelize(Array(line))
    val pred = { (a: Geometry, b: Geometry) => a intersects b }

    val res: Vector[(Polygon, Line)] = left.vectorJoin(right, pred).collect.toVector

    res should contain only ((polyA, line))
  }

  it("Joins another two RDDs of Geometries using Implicit Methods") {

    val left: RDD[Line] = sc.parallelize(Array(line))
    val right: RDD[Polygon] = sc.parallelize(Array(polyA, polyB))
    val pred = { (a: Geometry, b: Geometry) => a intersects b }

    val res: Vector[(Line, Polygon)] = left.vectorJoin(right, pred).collect.toVector

    res should contain only ((line, polyA))
  }
}
