/*
 * Copyright 2019 Azavea
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

package geotrellis.spark.store

import geotrellis.proj4.WebMercator
import geotrellis.layer._
import geotrellis.store._
import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.raster.testkit._
import geotrellis.store.index.rowmajor._
import geotrellis.spark._
import geotrellis.spark.testkit._

import java.io.File
import org.apache.commons.io.FileUtils

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class OverzoomingValueReaderSpec extends AnyFunSpec with TestEnvironment with Matchers with RasterMatchers {

  scala.util.Try(FileUtils.deleteDirectory(new File("/tmp/OZVR_test")))

  val baseTile = IntArrayTile.ofDim(256,256).map{ (x,y,_) => x+y }
  val layoutScheme = ZoomedLayoutScheme(WebMercator, 256)
  val ld = layoutScheme.levelForZoom(0).layout
  val bounds = KeyBounds(SpatialKey(0,0), SpatialKey(0,0))
  val metadata = TileLayerMetadata(IntConstantNoDataCellType, ld, ld.extent, WebMercator, bounds)
  val layer = ContextRDD(sc.parallelize(Seq(SpatialKey(0,0) -> baseTile)), metadata)
  val writer = LayerWriter("file:///tmp/OZVR_test")
  writer.write(LayerId("test_layer", 0), layer, new RowMajorSpatialKeyIndex(bounds))

  describe("OverzoomingValueReader") {
    it("should work for simple case") {
      val vr = ValueReader("file:///tmp/OZVR_test").reader[SpatialKey, Tile](LayerId("test_layer", 0))
      val ovr = ValueReader("file:///tmp/OZVR_test").overzoomingReader[SpatialKey, Tile](LayerId("test_layer", 1))

      val tile00z0 = vr.read(SpatialKey(0,0))
      val tile11z1 = ovr.read(SpatialKey(1,1))

      val ex00 = ld.mapTransform(SpatialKey(0,0))
      val ex11 = layoutScheme.levelForZoom(1).layout.mapTransform(SpatialKey(1,1))

      val resampled = tile00z0.resample(ex00, RasterExtent(ex11, 256, 256), ResampleMethod.DEFAULT)

      // import geotrellis.raster.render._
      // val cm = ColorMap((0 to 511).toArray, ColorRamps.Plasma)
      // tile00z0.renderPng(cm).write("test00.png")
      // tile11z1.renderPng(cm).write("test11.png")
      // resampled.renderPng(cm).write("resampled.png")

      assertEqual(resampled, tile11z1)

    }
  }

}
