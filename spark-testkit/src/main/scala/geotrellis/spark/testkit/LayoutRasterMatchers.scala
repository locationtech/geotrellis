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

package geotrellis.spark.testkit

import org.scalatest._
import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.layer.{SpatialKey, Bounds, LayoutDefinition}
import geotrellis.raster.testkit.RasterMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

import matchers._

trait LayoutRasterMatchers { self: Matchers with AnyFunSpec with RasterMatchers =>
  def containKey(key: SpatialKey) = Matcher[Bounds[SpatialKey]] { bounds =>
    MatchResult(bounds.includes(key),
      s"""$bounds does not contain $key""",
      s"""$bounds contains $key""")
  }

  def withGeoTiffClue[T](
    key: SpatialKey,
    layout: LayoutDefinition,
    actual: MultibandTile,
    expect: MultibandTile,
    crs: CRS
  )(fun: => T): T = {
    val extent = layout.mapTransform.keyToExtent(key)
    withGeoTiffClue[T](Raster(actual, extent), Raster(expect, extent), crs)(fun)
  }
}
