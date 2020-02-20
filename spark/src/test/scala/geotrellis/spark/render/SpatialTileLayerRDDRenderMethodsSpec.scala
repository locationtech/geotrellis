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

package geotrellis.spark.render

import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.spark._
import geotrellis.spark.render._
import geotrellis.spark.testkit.testfiles._
import geotrellis.spark.testkit._
import geotrellis.spark.store.hadoop._
import geotrellis.spark.testkit._

import org.scalatest._

class SpatialTileRDDRenderMethodsSpec extends FunSpec
    with TestEnvironment
    with TileBuilders
    with RasterMatchers
    with TileLayerRDDBuilders {
  lazy val sample = TestFiles.generateSpatial("all-ones")
  val tmpdir = System.getProperty("java.io.tmpdir")

  describe("Rendering colormap") {
    it("paints an rdd the same way it paints a single tile") {
      val tileNW =
        createValueTile(50, 1)
      val tileNE =
        createValueTile(50, 2)
      val tileSW =
        createValueTile(50, 3)
      val tileSE =
        createValueTile(50, 4)

      import geotrellis.raster.io.geotiff._
      val tiff = SinglebandGeoTiff(new java.io.File(inputHomeLocalPath, "elevation.tif").getAbsolutePath)

      val (raster, rdd) = createTileLayerRDD(tiff.raster.mapTile(_.toArrayTile), 100, 100, tiff.crs)

      val colorMap =
        ColorMap(
          Map(
            3.5 -> RGB(0,255,0).int,
            7.5 -> RGB(63, 255 ,51).int,
            11.5 -> RGB(102,255,102).int,
            15.5 -> RGB(178, 255,102).int,
            19.5 -> RGB(255,255,0).int,
            23.5 -> RGB(255,255,51).int,
            26.5 -> RGB(255,153, 51).int,
            31.5 -> RGB(255,128,0).int,
            35.0 -> RGB(255,51,51).int,
            40.0 -> RGB(255,0,0).int
          )
        )

      val expected = raster.tile.color(colorMap)
      val actual = rdd.color(colorMap).stitch

      assertEqual(actual, expected)
    }
  }
}
