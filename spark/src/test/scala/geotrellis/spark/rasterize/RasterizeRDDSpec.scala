/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.rasterize

import org.scalatest._
import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.TestEnvironment
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.wkt._
import geotrellis.vector.io.json._

import java.nio.file.Files;
import java.nio.file.Paths;

class RasterizeRDDSpec extends FunSpec with Matchers
    with TestEnvironment {

  def readFile(path: String): String =
    new String(Files.readAllBytes(Paths.get(path)));

  val septaRailLines = {
    val s = readFile("vector-test/data/septaRail.geojson")
    s.parseGeoJson[JsonFeatureCollection].getAllLines
  }

  val septaExtent = septaRailLines.map(_.envelope).reduce(_ combine _)

  it("rasterize lines"){
    val linesRdd = sc.parallelize(septaRailLines, 10)
    val layout = TileLayout(3,3,256,256)
    val ld = LayoutDefinition(septaExtent, layout)

    val rasterizedRdd = linesRdd.rasterizeWithValue(1, IntConstantNoDataCellType, ld)
    val actual = rasterizedRdd.stitch()

    // rasterizing a single 768x768 tile would actuall produce numerical differencies
    // with one of the diagonal lines because of the floating point math
    // this method tests that map-side combine works correctly.
    val expected: Tile = {
      for {
        tileCol <- 0 until 3
        tileRow <- 0 until 3
      } yield {
        val sk = SpatialKey(tileCol, tileRow)
        val keyExtent = ld.mapTransform(sk)
        sk -> Rasterizer.rasterizeWithValue(
          MultiLine(septaRailLines),
          RasterExtent(keyExtent, 256, 256),
          1)
      }
    }.stitch

    tilesEqual(actual.tile, expected)
  }

  it("rasterize polygon"){
    val wkt = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/wkt/huc10-conestoga.wkt")).getLines.mkString
    val huc10 = WKT.read(wkt).asInstanceOf[MultiPolygon]

    val layout = TileLayout(3,3,256,256)
    val ld = LayoutDefinition(huc10.envelope, layout)

    val polyRdd = sc.parallelize(huc10.polygons)
    val rasterizedRdd = polyRdd.rasterizeWithValue(1, IntConstantNoDataCellType, ld)
    val actual = rasterizedRdd.stitch()

    val expected: Tile = {
      for {
        tileCol <- 0 until 3
        tileRow <- 0 until 3
      } yield {
        val sk = SpatialKey(tileCol, tileRow)
        val keyExtent = ld.mapTransform(sk)
        sk -> Rasterizer.rasterizeWithValue(
          huc10,
          RasterExtent(keyExtent, 256, 256),
          1)
      }
    }.stitch
    info("MD: " + rasterizedRdd.metadata.tileLayout.toString)
    info("Expected" + expected.dimensions.toString)
    info("Actual: " + actual.tile.dimensions.toString)
    tilesEqual(actual.tile, expected)
  }
}
