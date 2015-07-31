/*
 * Copyright (c) 2014 DigitalGlobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.op.local.spatial

import geotrellis.raster.op.local._
import geotrellis.spark._
import geotrellis.spark.testfiles._
import geotrellis.vector._
import org.scalatest.FunSpec

import scala.util.Random

class LocalSpatialSpec extends FunSpec
                               with TestEnvironment
                               with TestFiles
                               with RasterRDDMatchers
                               with RasterRDDBuilders
                               with OnlyIfCanRunSpark {

  describe("Local Operations") {
    ifCanRunSpark {

      val rdd = AllOnesTestFile
      val tile = rdd.stitch
      val worldExt = rdd.metaData.extent
      val height = worldExt.height.toInt
      val width = worldExt.width.toInt

      def inMirror(bound: Int): Int = inRange(-bound to bound)
      def inRange(bounds: Range): Int = Random.nextInt(bounds.max - bounds.min) + bounds.min

      def triangle(size: Int, dx: Int, dy: Int): Line =
        Line(Seq[(Double, Double)]((0, 0), (size, 0), (size, size), (0, 0))
             .map { case (x, y) => (x + dx, y + dy) })

      def randomPolygons(number: Int = 50)(maxWidth: Int, maxHeight: Int): Seq[Polygon] = {
        val max = Math.min(maxWidth, maxHeight)
        val min = max / 10
        for {
          _ <- 1 to number
          size = inRange(min to max)
          placeLeft = Math.max(0, max - size)
          dx = inMirror(placeLeft) - size / 2
          dy = inMirror(placeLeft) - size / 2
          border = triangle(size, dx, dy)
          hole = triangle(size / 3, dx + size / 2, dy + size / 3)
        } yield Polygon(border, hole)
      }

      it ("should be masked by random polygons") {
        randomPolygons()(width, height) foreach { poly =>
          val masked = rdd.mask(poly).stitch
          val expected = tile.mask(worldExt, poly)
          masked.toArray() shouldEqual expected.toArray()
        }
      }

      it ("should be masked by complex polygons") {
        val cases = Seq(
          Polygon(Line((-5, -16), (44, -16), (44, 33), (-5, -16)), Line((19, 0), (35, 0), (35, 16), (19, 0))),
          Polygon(Line((-84, -41), (40, -41), (40, 83), (-84, -41)), Line((-22, 0), (19, 0), (19, 41), (-22, 0))),
          Polygon(Line((-7, 0), (28, 0), (28, 35), (-7, 0)), Line((10, 11), (21, 11), (21, 22), (10, 11)))
        )
        cases foreach { poly =>
          val masked = rdd.mask(poly).stitch
          val expected = tile.mask(worldExt, poly)
          masked.toArray() shouldEqual expected.toArray()
        }
      }

      it ("should be masked by random multipolygons") {
        val polygons = randomPolygons()(width, height)
        val multipolygons = polygons.zip(polygons.reverse).map { case (a, b) =>
          MultiPolygon(a, b)
        }
        multipolygons foreach { multipoly =>
          val masked = rdd.mask(multipoly).stitch
          val expected = tile.mask(worldExt, multipoly)
          masked.toArray() shouldEqual expected.toArray()
        }
      }

      it ("should be masked by complex multipolygons") {
        val cases = Seq(
          MultiPolygon(Polygon(Line((29, 15), (110, 15), (110, 96), (29, 15)), Line((69, 42), (96, 42), (96, 69), (69, 42))),
            Polygon(Line((-77, -78), (46, -78), (46, 45), (-77, -78)), Line((-16, -37), (25, -37), (25, 4), (-16, -37)))),
          MultiPolygon(Polygon(Line((-41, -17), (0, -17), (0, 24), (-41, -17)), Line((-21, -4), (-8, -4), (-8, 9), (-21, -4))),
            Polygon(Line((-83, -76), (-13, -76), (-13, -6), (-83, -76)), Line((-48, -53), (-25, -53), (-25, -30), (-48, -53))))
        )
        cases foreach { multipoly =>
          val masked = rdd.mask(multipoly).stitch
          val expected = tile.mask(worldExt, multipoly)
          masked.toArray() shouldEqual expected.toArray()
        }
      }

      it ("should be masked by random extents") {
        val extents = randomPolygons()(width, height).map(_.envelope)
        extents foreach { extent =>
          val masked = rdd.mask(extent).stitch
          val expected = tile.mask(worldExt, extent)
          masked.toArray() shouldEqual expected.toArray()
        }
      }
    }
  }
}
