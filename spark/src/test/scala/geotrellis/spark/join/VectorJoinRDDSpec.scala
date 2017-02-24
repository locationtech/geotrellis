/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.join

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.partition._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.spark.testkit._

import org.apache.spark._
import org.apache.spark.rdd._
import org.scalatest._

class VectorJoinRDDSpec extends FunSpec with Matchers with TestEnvironment {

  val polyA = Polygon(
    Line(Point(0,0), Point(1,0), Point(1,1), Point(0,1), Point(0,0)))
  val polyB = Polygon(
    List(Point(10,10), Point(11,10), Point(11,11), Point(10,11), Point(10,10)))
  val polyC = Polygon(
    List(Point(100,100), Point(101,100), Point(101,101), Point(100,101), Point(100,100)))
  val line1 = Line(Point(0,0), Point(5,5))
  val line2 = Line(Point(13,0), Point(33,0))

  it("Joins two RDDs of Geometries") {

    val left: RDD[Polygon] = sc.parallelize(Array(polyA, polyB, polyC))
    val right: RDD[Line] = sc.parallelize(Array(line1, line2, line2))
    val pred = { (a: Geometry, b: Geometry) => a intersects b }

    val res: Vector[(Polygon, Line)] = VectorJoin(left, right, pred).collect.toVector

    res should contain only ((polyA, line1))
  }

  it("Joins two RDDs of Geometries using Implicit Methods") {

    val left: RDD[Polygon] = sc.parallelize(Array(polyA, polyB, polyC))
    val right: RDD[Line] = sc.parallelize(Array(line1, line2, line2))
    val pred = { (a: Geometry, b: Geometry) => a intersects b }

    val res: Vector[(Polygon, Line)] = left.vectorJoin(right, pred).collect.toVector

    res should contain only ((polyA, line1))
  }

  it("Joins another two RDDs of Geometries using Implicit Methods (1/2)") {

    val left: RDD[Line] = sc.parallelize(Array(line2, line1))
    val right: RDD[Polygon] = sc.parallelize(Array(polyA, polyB, polyC, polyC, polyC, polyB))
    val pred = { (a: Geometry, b: Geometry) => a intersects b }

    val res: Vector[(Line, Polygon)] = left.vectorJoin(right, pred).collect.toVector

    res should contain only ((line1, polyA))
  }

  it("Joins another two RDDs of Geometries using Implicit Methods (2/2)") {

    val left: RDD[Line] = sc.parallelize(Array(line2, line2, line2, line1), 4)
    val right: RDD[Polygon] = sc.parallelize(Array(polyA, polyB, polyC, polyC, polyC, polyB), 6)
    val pred = { (a: Geometry, b: Geometry) => a intersects b }

    val res: Vector[(Line, Polygon)] = left.vectorJoin(right, pred).collect.toVector

    res should contain only ((line1, polyA))
  }
}
