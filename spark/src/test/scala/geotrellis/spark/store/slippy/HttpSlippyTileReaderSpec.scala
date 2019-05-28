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

package geotrellis.spark.store.slippy

import geotrellis.tiling.SpatialKey

import org.scalatest._

class HttpSlippyTileReaderTest extends FunSpec {
  describe("HttpSlippyTileReader") {
    it("should return correct urls for given zoom level") {
      val reader =
        new HttpSlippyTileReader[String]("http://tile.openstreetmap.us/vectiles-highroad/{z}/{x}/{y}.mvt")({ case (key, bytes) => key.toString })

      assert(
        reader.getURLs(2) == Seq(
          "http://tile.openstreetmap.us/vectiles-highroad/2/0/0.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/0/1.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/0/2.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/0/3.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/1/0.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/1/1.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/1/2.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/1/3.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/2/0.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/2/1.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/2/2.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/2/3.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/3/0.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/3/1.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/3/2.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/3/3.mvt"
        )
      )
    }
  }
}
