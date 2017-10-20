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

package geotrellis.spark.clip

import geotrellis.raster.TileLayout
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.testkit._
import geotrellis.vector._
import geotrellis.vector.testkit._

import org.apache.spark.rdd.RDD

import org.scalatest.FunSpec

class ClipToGridSpec extends FunSpec with TestEnvironment {
  describe("ClipToGrid") {
    val layoutDefinition =
      LayoutDefinition(
        Extent(0, -20.0, 10.0, 0),
        TileLayout(10, 10, 1, 1)
      )

    def checkCorrect(actual: RDD[(SpatialKey, Geometry)], expected: Vector[(SpatialKey, Geometry)]): Unit = {
      val result = actual.collect().toVector
      val resultKeys = result.map(_._1).sortBy(k => (k.col, k.row))
      val expectedKeys = expected.map(_._1).sortBy(k => (k.col, k.row))
      resultKeys should be (expectedKeys)

      val resultMap = result.groupBy(_._1).map { case (k, seq) => (k, seq.map(_._2)) }.toMap
      val expectedMap = expected.groupBy(_._1).map { case (k, seq) => (k, seq.map(_._2)) }.toMap

      for((k, resultGeoms) <- resultMap) {
        val expectedGeoms = expectedMap(k)

        resultGeoms.sortBy { g => (g.envelope.xmin, g.envelope.ymax) }
          .zip(expectedGeoms.sortBy { g => (g.envelope.xmin, g.envelope.ymax) })
          .foreach { case (g1, g2) =>
            withClue(s"Failed for $k:") {
              g1 should matchGeom (g2, 0.0001)
            }
          }
      }
    }

    def withKey[T](k: SpatialKey)(f: SpatialKey => T): (SpatialKey, T) =
      (k, f(k))

    it("should clip a point") {
      val p = Point(0, -10.0)
      val rdd = sc.parallelize(Array(p))
      val result = ClipToGrid(rdd, layoutDefinition).collect().toVector
      result.size should be (1)
      result(0) should be ((SpatialKey(0, 5), p))
    }

    it("should clip a multipoint") {
      val p1 = Point(0, -10.0)
      val p2 = Point(0, -10.1)
      val p3 = Point(0, 0)

      val rdd = sc.parallelize(Array(MultiPoint(p1, p2, p3)))
      val result = ClipToGrid(rdd, layoutDefinition).collect().toVector.sortBy(_._2.envelope.ymin)
      result.size should be (2)
      result(0)._1 should be (SpatialKey(0, 5))
      result(0)._2 should be (an[MultiPoint])
      result(0)._2.asInstanceOf[MultiPoint] should matchGeom (MultiPoint(p1, p2))
      result(1)._1 should be (SpatialKey(0, 0))
      result(1)._2 should be (p3)
    }

    it("should clip a line") {
      val line =
        Line(
          (1.5, -14.2),
          (1.5, -12.5),
          (2.6, -12.5),
          (2.6, -15.1),
          (1.6, -15.1)
        )

      val rdd = sc.parallelize(Array(line))
      val actual = ClipToGrid(rdd, layoutDefinition)

      checkCorrect(actual,
        Vector(
          (SpatialKey(1, 6), Line((1.5, -14.0), (1.5, -12.5), (2.0, -12.5))),
          (SpatialKey(2, 6), Line((2.0, -12.5), (2.6, -12.5), (2.6, -14.0))),
          (SpatialKey(2, 7), Line((2.6, -14.0), (2.6, -15.1), (2.0, -15.1))),
          (SpatialKey(1, 7),
            MultiLine(
              Line((2.0, -15.1), (1.6, -15.1)),
              Line((1.5, -14.2), (1.5, -14.0))
            )
          )
        )
      )
    }


    it("should clip a line contained in one key") {
      val line =
        Line(
          (1.5, -14.2),
          (1.5, -14.1),
          (1.6, -14.1),
          (1.6, -15.1),
          (1.55, -15.1)
        )

      val rdd = sc.parallelize(Array(line))
      val actual = ClipToGrid(rdd, layoutDefinition)

      checkCorrect(actual, Vector((SpatialKey(1, 7), line)))
    }

    it("should clip a polygon") {
      val shell =
        Line(
          (1.5, -3.0),
          (6.5, -3.0),
          (6.5, -19.0),
          (1.5, -19.0),
          (1.5, -3.0)
        )

      val hole =
        Line(
          (3.5, -7.0),
          (5.5, -7.0),
          (5.5, -15.0),
          (3.5, -15.0),
          (3.5, -7.0)
        )

      val poly =
        Polygon(shell, hole)

      val shellExtent = Polygon(shell).envelope
      val holeExtent = Polygon(hole).envelope

      def outerPoly(k: SpatialKey): Polygon =
        try {
          layoutDefinition.mapTransform(k).intersection(shellExtent).get.toPolygon
        } catch {
          case e: Throwable => println(s"Failed at $k"); throw e
        }

      def innerPoly(k: SpatialKey): Polygon =
        try {
          (layoutDefinition.mapTransform(k).toPolygon - holeExtent.toPolygon).as[Polygon].get
        } catch {
          case e: Throwable => println(s"Failed at $k"); throw e
        }

      val rdd = sc.parallelize(Array(poly))
      val actual = ClipToGrid(rdd, layoutDefinition)

      checkCorrect(actual,
        Vector(
          // Outer - Upper
          withKey(SpatialKey(1, 1))(outerPoly(_)),
          withKey(SpatialKey(2, 1))(outerPoly(_)),
          withKey(SpatialKey(3, 1))(outerPoly(_)),
          withKey(SpatialKey(4, 1))(outerPoly(_)),
          withKey(SpatialKey(5, 1))(outerPoly(_)),
          withKey(SpatialKey(6, 1))(outerPoly(_)),

          // Outer - Right
          withKey(SpatialKey(6, 2))(outerPoly(_)),
          withKey(SpatialKey(6, 3))(outerPoly(_)),
          withKey(SpatialKey(6, 4))(outerPoly(_)),
          withKey(SpatialKey(6, 5))(outerPoly(_)),
          withKey(SpatialKey(6, 6))(outerPoly(_)),
          withKey(SpatialKey(6, 7))(outerPoly(_)),
          withKey(SpatialKey(6, 8))(outerPoly(_)),
          withKey(SpatialKey(6, 9))(outerPoly(_)),

          // Outer - Bottom
          withKey(SpatialKey(5, 9))(outerPoly(_)),
          withKey(SpatialKey(4, 9))(outerPoly(_)),
          withKey(SpatialKey(3, 9))(outerPoly(_)),
          withKey(SpatialKey(2, 9))(outerPoly(_)),
          withKey(SpatialKey(1, 9))(outerPoly(_)),

          // Outer - Left
          withKey(SpatialKey(1, 8))(outerPoly(_)),
          withKey(SpatialKey(1, 7))(outerPoly(_)),
          withKey(SpatialKey(1, 6))(outerPoly(_)),
          withKey(SpatialKey(1, 5))(outerPoly(_)),
          withKey(SpatialKey(1, 4))(outerPoly(_)),
          withKey(SpatialKey(1, 3))(outerPoly(_)),
          withKey(SpatialKey(1, 2))(outerPoly(_)),

          // Center - Upper
          withKey(SpatialKey(2, 2))(layoutDefinition.mapTransform.apply(_).toPolygon),
          withKey(SpatialKey(3, 2))(layoutDefinition.mapTransform.apply(_).toPolygon),
          withKey(SpatialKey(4, 2))(layoutDefinition.mapTransform.apply(_).toPolygon),
          withKey(SpatialKey(5, 2))(layoutDefinition.mapTransform.apply(_).toPolygon),

          // Center - Bottom
          withKey(SpatialKey(2, 8))(layoutDefinition.mapTransform.apply(_).toPolygon),
          withKey(SpatialKey(3, 8))(layoutDefinition.mapTransform.apply(_).toPolygon),
          withKey(SpatialKey(4, 8))(layoutDefinition.mapTransform.apply(_).toPolygon),
          withKey(SpatialKey(5, 8))(layoutDefinition.mapTransform.apply(_).toPolygon),

          // Center - Left
          withKey(SpatialKey(2, 7))(layoutDefinition.mapTransform.apply(_).toPolygon),
          withKey(SpatialKey(2, 6))(layoutDefinition.mapTransform.apply(_).toPolygon),
          withKey(SpatialKey(2, 5))(layoutDefinition.mapTransform.apply(_).toPolygon),
          withKey(SpatialKey(2, 4))(layoutDefinition.mapTransform.apply(_).toPolygon),
          withKey(SpatialKey(2, 3))(layoutDefinition.mapTransform.apply(_).toPolygon),

          // Inner - Upper
          withKey(SpatialKey(3, 3))(innerPoly(_)),
          withKey(SpatialKey(4, 3))(innerPoly(_)),
          withKey(SpatialKey(5, 3))(innerPoly(_)),

          // Inner - Right
          withKey(SpatialKey(5, 4))(innerPoly(_)),
          withKey(SpatialKey(5, 5))(innerPoly(_)),
          withKey(SpatialKey(5, 6))(innerPoly(_)),
          withKey(SpatialKey(5, 7))(innerPoly(_)),

          // Inner - Bottom
          withKey(SpatialKey(4, 7))(innerPoly(_)),
          withKey(SpatialKey(3, 7))(innerPoly(_)),

          // Inner - Left
          withKey(SpatialKey(3, 6))(innerPoly(_)),
          withKey(SpatialKey(3, 5))(innerPoly(_)),
          withKey(SpatialKey(3, 4))(innerPoly(_))
        )
      )
    }

  }
}
