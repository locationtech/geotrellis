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
import geotrellis.spark._
import geotrellis.spark.testfiles._
import geotrellis.vector.{Line, Polygon}
import org.scalatest.FunSpec

class LocalSpatialSpec extends FunSpec
    with TestEnvironment
    with TestFiles
    with RasterRDDMatchers
    with RasterRDDBuilders
    with OnlyIfCanRunSpark {
  describe("Local Operations") {
    ifCanRunSpark {

      it ("should be masked by polygon") {

        val tile = ArrayTile(
          Array(
            1,1,1,1,  1,1,1,1,  1,1,1,NODATA,
            1,1,1,1,  1,1,1,1,  1,1,1,1,

            1,1,1,1,  1,1,1,1,  1,1,1,1,
            1,1,1,1,  1,1,1,1,  1,1,1,1),
          12, 4)

        val rdd = createRasterRDD(sc, tile, TileLayout(3, 2, 4, 2))
        val m = 180
        val square = Polygon(Line(
          (-m, -m), (m, -m), (m, m), (-m, m), (-m, -m)
        ))
        val square2 = Polygon(Line(
          (0, 0), (m, 0), (m, m), (0, m), (0, 0)
        ))

        println(rdd.asciiDraw())
        println(rdd.mask(square).asciiDraw())
        println(rdd.mask(square2).asciiDraw())
      }
    }
  }
}
