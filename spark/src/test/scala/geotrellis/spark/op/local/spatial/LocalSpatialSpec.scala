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

import geotrellis.raster._
import geotrellis.raster.op.local._
import geotrellis.raster.rasterize.Rasterizer
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

      it ("should be masked by random polygon") {

        val tile = ArrayTile(
          Array(
            1,1,1,1,  1,1,1,1,  1,1,1,NODATA,
            1,1,1,1,  1,1,1,1,  1,1,1,1,

            1,1,1,1,  1,1,1,1,  1,1,1,1,
            1,1,1,1,  1,1,2,1,  1,1,1,1),
          12, 4)
        val rdd = createRasterRDD(sc, tile, TileLayout(3, 2, 4, 2))
        val RasterMetaData(_, worldExt, _, _) = rdd.metaData
        val height = worldExt.height.toInt
        val width = worldExt.width.toInt

        def triangle(size: Int, dx: Double, dy: Double): Line =
          Line(Seq((-size, -size), (size, -size), (size, size), (-size, -size))
               .map { case (x, y) => (x + dx, y + dy) })

        def check(mask: Polygon): Unit = {
          println(rdd.mask(mask).asciiDraw())
          println(tile.mask(worldExt, mask).asciiDraw())
          val masked = rdd.mask(mask).stitch.toArray()
          val expected = tile.mask(worldExt, mask).toArray()
          masked.zip(expected).foreach { case (m, e) =>
            m should be (e)
          }
        }

        for {
          _ <- 1 to 10
          size = Random.nextInt(3*height/4) + height/4
          dx = Random.nextInt(width - size) - width/2 - 0.1
          dy = Random.nextInt(height - size) - height/2 - 0.1
          border = triangle(size, dx, dy)
          hole = triangle(size/2, dx, dy)
        } check(Polygon(border, hole))
      }
    }
  }
}
