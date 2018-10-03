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

package geotrellis.raster.vectorize

import org.locationtech.jts.{geom => jts}
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.vector._
import org.scalatest._

class VectorizeSpec extends FunSpec
                      with RasterMatchers with TestFiles
                      with TileBuilders {
  val cw = 1
  val ch = 10
  val xmin = 0
  val ymax = 0

  // These are specific to cw = 1  ch = 10
  def d(i: Int) = i.toDouble
  def tl(col: Int, row: Int) = (d(col), d(row)* -10)        // Top left
  def tr(col: Int, row: Int) = (d(col + 1), (d(row))* -10)    // Top right
  def bl(col: Int, row: Int) = (d(col), d(row + 1)* -10)        // Bottom left
  def br(col: Int, row: Int) = (d(col + 1), (d(row) + 1)* -10)    // Bottom right

  def assertPolygon(polygon: Polygon, expectedCoords: List[(Double, Double)]) = {
    assertCoords(polygon.jtsGeom.getCoordinates.map(c => (c.x, c.y)), expectedCoords)
  }

  def assertCoords(coordinates: Seq[(Double, Double)], expectedCoords: List[(Double, Double)]) = {
    def coordString = {
      val sortedActual = coordinates.toSeq.sortBy(c => (c._1, -c._2)).toList
      val sortedExpected = expectedCoords.toSeq.sortBy(c => (c._1, -c._2)).toList
      var s = "Values: (Actual, Expected)\n"
      for(x <- 0 until math.max(sortedActual.length, sortedExpected.length)) {
        if(x < sortedActual.length) { s += s" ${sortedActual(x)}  " }
        if(x < sortedExpected.length) { s += s" ${sortedExpected(x)}  \n" }
        else { s += "\n" }
      }
      s
    }

    withClue (s"Coordinate size does not match expected.\n$coordString\n\t\t") {
      coordinates.length should be (expectedCoords.size + 1)
    }

    coordinates.map { c =>
      withClue (s"$c not in expected coordinate set: \n$coordString") {
        expectedCoords.contains(c) should be (true) }
    }
  }

  describe("ToVector") {
    it("should get correct vectors for simple case.") {
      val n = NODATA
      val arr =
        Array( n, 1, 1, n, n, n,
               n, 1, 1, n, n, n,
               n, n, n, 1, 1, n,
               n, n, n, 1, 1, n,
               n, n, n, n, n, n)

      val cols = 6
      val rows = 5
      val xmax = 6
      val ymin = -50

      val extent = Extent(xmin, ymin, xmax, ymax)

      val r = ArrayTile(arr, cols, rows)

      val geoms = r.toVector(extent)
      val onesCoords = List(
        (1.0, 0.0), (3.0, -0.0),
        (1.0, -20.0), (3.0, -20.0),
        (3.0, -20.0), (5.0, -20.0),
        (3.0, -40.0), (5.0, -40.0))

      val ones = geoms.filter(_.data == 1).toList
      ones.length should be (2)

      ones.map { polygon =>
        polygon.data should be (1)
        val coordinates = polygon.geom.jtsGeom.getCoordinates.map(c => (c.x, c.y))
        coordinates.length should be (5)
        coordinates.map { c =>
          withClue (s"$c in expected coordinate set: ") { onesCoords.contains(c) should be (true) }
        }
      }
    }

    it("should get correct vectors for broken square with hole.") {
      val n = NODATA
      val arr =
        Array( n, n, n, n, n, n,
               n, 1, 1, 1, n, n,
               n, 1, n, 1, n, n,
               n, 1, n, 1, n, n,
               n, n, 1, 1, n, n)

      val cols = 6
      val rows = 5
      val xmax = 6
      val ymin = -50

      val extent = Extent(xmin, ymin, xmax, ymax)
      val r = ArrayTile(arr, cols, rows)

      val geoms = r.toVector(extent)

      val onesCoords = List( tl(1, 1), tr(3, 1),
                             bl(1, 3), br(1, 3),
                             bl(2, 4), br(3, 4))

      val holeCoords = List( br(1, 1), bl(3, 1),
                             tl(2, 4), tl(3, 4) )

      val ones = geoms.filter(_.data == 1).toList
      ones.length should be (1)
      val poly = ones(0).geom
      val shell = poly.exterior
      val holes = poly.holes
      holes.length should be (1)
      val hole = holes(0)
      assertCoords(shell.jtsGeom.getCoordinates.map { c => (c.x, c.y) }, onesCoords)
      assertCoords(hole.jtsGeom.getCoordinates.map { c => (c.x, c.y) }, holeCoords)
    }

    it("should vectorize an off shape.") {
      val n = NODATA
      val arr =
        Array(
//             0  1  2  3  4  5  6  7  8  9  0  1
               n, n, n, n, n, n, n, n, n, n, n, n, // 0
               n, n, n, n, n, n, n, n, n, n, n, n, // 1
               n, n, n, n, n, n, n, n, n, n, n, n, // 2
               n, n, n, n, 1, 1, n, n, n, 1, 1, n, // 3
               n, n, n, 1, 1, 1, n, n, n, 1, 1, n, // 4
               n, n, 1, 1, 5, 1, n, n, 1, 1, n, n, // 5
               n, n, 1, 5, 5, 1, n, 1, 1, n, n, n, // 6
               n, n, 5, 5, 1, 1, n, 1, n, n, n, n, // 7
               n, n, 5, 5, 1, 1, 1, 1, n, n, n, n, // 8
               n, n, n, n, n, n, n, n, n, n, n, n, // 9
               n, n, n, n, n, n, n, n, n, n, n, n, // 0
               n, n, n, n, n, n, n, n, n, n, n, n, // 1
               n, n, n, n, n, n, n, n, n, n, n, n, // 2
               n, n, n, n, n, n, n, n, n, n, n, n) // 3

      val cols = 12
      val rows = 14
      val xmax = 12
      val ymin = -140

      val extent = Extent(xmin, ymin, xmax, ymax)
      val r = ArrayTile(arr, cols, rows)

      val geoms = r.toVector(extent)

      val onesCoords = List(  tl(4, 3), tr(5, 3),                       tl(9, 3), tr(10, 3),
                    tl(3, 4), tl(4, 4),
                            br(3, 4),  bl(5, 4),              tl(8, 5), tl(9, 5),
            tl(2, 5), tl(3, 5), br(3, 5),               tl(7, 6), tl(8, 6), br(9, 4), br(10, 4),
                    br(2, 5),          br(5, 7),     tl(7, 8), br(8, 5), br(9, 5),
            bl(2, 6), br(2, 6),          bl(5, 6),     br(7, 6), br(8, 6),
                               tl(4, 7),
                               bl(4, 8),            br(7, 8)
      )

      geoms.length should be (2)

      val ones = geoms.filter(_.data == 1).map(_.asInstanceOf[PolygonFeature[Int]]).toList
      ones.length should be (1)

      ones.map(assertPolygon(_, onesCoords))
    }

    it("should vectorize an shape with a nodata seperation line.") {
      val n = NODATA
      val arr =
        Array(
//             0  1  2  3  4
               1, 1, 1, 1, n,// 0
               1, 5, 5, 1, n,// 1
               5, 5, 1, 1, 1,// 2
               n, n, n, n, 1,// 3
               1, n, 1, n, 1,// 4
               1, 1, 1, 5, 1,// 5
               1, 5, 5, 1, 1,// 6
               1, 1, 1, 1, n)// 7

      val cols = 5
      val rows = 8
      val xmax = 5
      val ymin = -80

      val extent = Extent(xmin, ymin, xmax, ymax)
      val r = ArrayTile(arr, cols, rows)
      val geoms = r.toVector(extent)

      val shellCoords = List(
        tl(0, 0),                               tr(3, 0),
                br(0, 0),               bl(3, 0),
        bl(0, 1), br(0, 1),
                               tl(2, 2), tl(3, 2), tr(3, 2),           tr(4, 2),
                               bl(2, 2),                   bl(4, 2),

        tl(0, 4), tr(0, 4),       tl(2, 4), tr(2, 4),
                br(0, 4),       bl(2, 4),

                                                   tl(3, 6), tl(4, 6),
                                                           br(3, 6), br(4, 6),
        bl(0, 7),                                           br(3, 7)
      )

      val holeCoords = List(
        br(0, 5), br(2, 5),
        tr(0, 7), tr(2, 7)
      )

      withClue ("Number of polygons did not match expected: ") { geoms.length should be (4) }

      val ones = geoms.filter(_.data == 1).map(_.asInstanceOf[PolygonFeature[Int]]).toList
      ones.length should be (1)

      val poly = ones(0).geom
      val shell = poly.exterior
      val holes = poly.holes
      holes.length should be (1)
      val hole = holes(0)
      assertCoords(shell.jtsGeom.getCoordinates.map { c => (c.x, c.y) }, shellCoords)
      assertCoords(hole.jtsGeom.getCoordinates.map { c => (c.x, c.y) }, holeCoords)
    }

    it("should vectorize an shape with a hole.") {
      val n = NODATA
      val arr =
        Array(
//             0  1  2  3  4
               n, 1, 1, 1, n,// 0
               1, 1, n, 1, n,// 1
               1, n, n, 1, 1,// 2
               1, 1, 1, 1, 1 // 3
        )

      val cols = 5
      val rows = 4
      val xmax = 5
      val ymin = -40

      val extent = Extent(xmin, ymin, xmax, ymax)
      val r = ArrayTile(arr, cols, rows)

      val geoms = r.toVector(extent)

      val expectedShellCoords = List(

                tl(1, 0),     tr(3, 0),
        tl(0, 1), tl(1, 1),
                             tr(3, 2), tr(4, 2),
        bl(0, 3),                     br(4, 3)
      )

      val expectedHoleCoords = List(
                 tl(2, 1), tr(2, 1),
        tl(1, 2), bl(2, 1),
        bl(1, 2),         br(2, 2)
      )

      withClue ("Number of polygons did not match expected: ") { geoms.length should be (1) }

      val ones = geoms.filter(_.data == 1).map(_.asInstanceOf[PolygonFeature[Int]]).toList
      ones.length should be (1)
      val polygon = ones(0)

      polygon.data should be (1)
      val shellCoordinates = polygon.geom.jtsGeom.getExteriorRing.getCoordinates.map(c => (c.x, c.y))
      assertCoords(shellCoordinates, expectedShellCoords)

      polygon.geom.jtsGeom.getNumInteriorRing() should be (1)
      val holeCoordinates = polygon.geom.jtsGeom.getInteriorRingN(0).getCoordinates.map(c => (c.x, c.y))
      assertCoords(holeCoordinates, expectedHoleCoords)
    }

    it("should vectorize an shape with two holes.") {
      val n = NODATA
      val arr =
        Array(
//             0  1  2  3  4
               n, 1, 1, 1, n,// 0
               1, 1, n, 1, n,// 1
               1, n, n, 1, 1,// 2
               1, 1, 1, 1, 1,// 3
               1, 1, n, n, 1,// 4
               1, 1, 1, 1, 1 // 5
        )


      val cols = 5
      val rows = 6
      val xmax = 5
      val ymin = -60

      val extent = Extent(xmin, ymin, xmax, ymax)
      val r = ArrayTile(arr, cols, rows)


      val geoms = r.toVector(extent)

      val expectedShellCoords = List(
                tl(1, 0),     tr(3, 0),
        tl(0, 1), tl(1, 1),
                             tr(3, 2), tr(4, 2),



        bl(0, 5),                     br(4, 5)
      )

      val expectedHoleCoords = List(
                  tl(2, 1), tr(2, 1),
        tl(1, 2), bl(2, 1),
        bl(1, 2),           br(2, 2)
      )

      val expectedHoleCoords2 = List(
                     tl(2, 4), tr(3, 4),
                     bl(2, 4), br(3, 4)
      )

      withClue ("Number of polygons did not match expected: ") { geoms.length should be (1) }

      val ones = geoms.filter(_.data == 1).map(_.asInstanceOf[PolygonFeature[Int]]).toList
      ones.length should be (1)
      val polygon = ones(0)

      polygon.data should be (1)
      val shellCoordinates = polygon.geom.jtsGeom.getExteriorRing.getCoordinates.map(c => (c.x, c.y))
      assertCoords(shellCoordinates, expectedShellCoords)

      polygon.geom.jtsGeom.getNumInteriorRing() should be (2)

      val holes =
        Vector(0, 1)
          .map { i => polygon.geom.jtsGeom.getInteriorRingN(i).getCoordinates }
          .sortBy { coords => coords.map(c => c.x).min }
          .map(_.map(c => (c.x, c.y)))

      val holeCoordinates = polygon.geom.jtsGeom.getInteriorRingN(1).getCoordinates.map(c => (c.x, c.y))
      assertCoords(holes(0), expectedHoleCoords)
      val holeCoordinates2 = polygon.geom.jtsGeom.getInteriorRingN(0).getCoordinates.map(c => (c.x, c.y))
      assertCoords(holes(1), expectedHoleCoords2)
    }

    it("should vectorize an shape with two polys.") {
      val n = NODATA
      val arr =
        Array(
//             0  1  2  3  4
               n, 1, n, 1, n,// 0
               1, 1, n, 1, n,// 1
               n, n, n, 1, 1,// 2
               1, 1, n, n, 1,// 3
               1, 1, n, n, 1,// 4
               1, 1, n, 1, 1 // 5
        )


      val cols = 5
      val rows = 6
      val xmax = 5
      val ymin = -60

      val extent = Extent(xmin, ymin, xmax, ymax)
      val r = ArrayTile(arr, cols, rows)

      val geoms = r.toVector(extent)

      withClue ("Number of polygons did not match expected: ") { geoms.length should be (3) }
    }

    it("should vectorize a raster that was at one point not vectorizing properly") {
      val rs = loadTestArg("data/vectorbugger")
      val r = rs.tile
      val extent = rs.extent
      val vect = r.toVector(extent)
    }

    it("should vectorize another raster that was at one point not vectorizing properly") {
      println("Running test on vectorbugger2...")
      val rs = loadTestArg("data/vectorbugger2")
      val r = rs.tile
      val extent = rs.extent
      val vect = r.toVector(extent)
    }

    it("should vectorize yet another raster that was at one point not vectorizing properly") {
      println("Running test on vectorbugger3...")
      val rs = loadTestArg("data/vectorbugger3")
      val r = rs.tile
      val extent = rs.extent
      val vect = r.toVector(extent)
    }

    it("should vectorize a two cell polygon") {
      val n = NODATA
      val arr =
        Array(
//             0  1  2
               n, n, n,// 0
               n, 1, n,// 1
               n, 1, n,// 2
               n, n, n // 3
        )


      val cols = 3
      val rows = 4

      val extent = Extent(0, -4, 3, 0)
      val r = ArrayTile(arr, cols, rows)

      val toVector = r.toVector(extent)

      toVector.length should be (1)
      val geom = toVector.head.geom

      val coordinates = geom.jtsGeom.getCoordinates

      coordinates.length should be (5)

      val sorted = coordinates.map { c => (c.x, c.y) }.sorted.toList

      val expected = List( (1.0, -3.0), (1.0, -1.0), (1.0, -1.0), (2.0, -3.0), (2.0, -1.0) )

      sorted should be (expected)
    }

    it("should vectorize when last move is to left") {
      val n = NODATA
      val arr =
        Array(
//             0  1  2  3
               n, n, n, n,// 0
               n, 1, 1, n,// 1
               n, 1, n, n,// 2
               n, n, n, n // 3
        )


      val cols = 4
      val rows = 4

      val extent = Extent(0, -4, 4, 0)
      val r = ArrayTile(arr, cols, rows)

      val toVector = r.toVector(extent)

      toVector.length should be (1)
      val geom = toVector.head.geom

      val coordinates = geom.jtsGeom.getCoordinates

      coordinates.length should be (7)

      val sorted = coordinates.map { c => (c.x, c.y) }.sorted.toList

      val expected = List(
        (1.0, -3.0),
        (1.0, -1.0),
        (1.0, -1.0),
        (2.0, -3.0),
        (2.0, -2.0),
        (3.0, -2.0),
        (3.0, -1.0)
      )

      sorted should be (expected)
    }

    it("should vectorize when last two moves is down and left") {
      val n = NODATA
      val arr =
        Array(
//             0  1  2  3
               n, n, n, n,// 0
               n, n, 1, n,// 1
               n, 1, 1, n,// 2
               n, n, n, n // 3
        )


      val cols = 4
      val rows = 4

      val extent = Extent(0, -4, 4, 0)
      val r = ArrayTile(arr, cols, rows)

      val toVector = r.toVector(extent)

      toVector.length should be (1)
      val geom = toVector.head.geom

      val coordinates = geom.jtsGeom.getCoordinates

      coordinates.length should be (7)

      val sorted = coordinates.map { c => (c.x, c.y) }.sorted.toList

      val expected = List(
        (1.0, -3.0),
        (1.0, -2.0),
        (1.0, -2.0),
        (2.0, -2.0),
        (2.0, -1.0),
        (3.0, -3.0),
        (3.0, -1.0)
      )

      sorted should be (expected)
    }

    it("should handle a situation where the hole has multiple values that need to be considered one large hole") {
      val n = NODATA
      val arr =
        Array(
//             0  1  2  3  4  5  6
               3, 3, 3, 3, 3, 3, 3, // 0
               3, 1, 1, 1, 1, 1, 3, // 1
               3, 1, 2, 2, 2, 1, 3, // 2
               3, 1, 2, 7, 2, 1, 3, // 3
               3, 1, 2, 2, 1, 1, 3, // 4
               3, 1, 1, 1, 1, 1, 3, // 5
               3, 3, 3, 3, 3, 3, 3  // 5
        )


      val cols = 7
      val rows = 7

      val extent = Extent(0, -7, 7, 0)
      val r = ArrayTile(arr, cols, rows)

      val toVector = r.toVector(extent)

      toVector.length should be (4)
    }
  }

  describe("test case that has thrown topology exceptions in the past") {
    import geotrellis.raster.io.geotiff._

    val gt = SinglebandGeoTiff("raster/data/vectorize-bug-1.tif")
    val r = gt.raster
    val t = r.tile

    val polys = t.toVector(r.extent)
    polys.size should be (88)
  }
}
