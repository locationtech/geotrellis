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

package geotrellis.raster.summary

import org.openjdk.jmh.annotations.{Mode => JMHMode, _}
import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.summary.polygonal._
import geotrellis.raster.summary.polygonal.visitors.MaxVisitor
import geotrellis.raster.io.geotiff.{MultibandGeoTiff, SinglebandGeoTiff}

@BenchmarkMode(Array(JMHMode.AverageTime))
@State(Scope.Thread)
class PolygonalSummaryBench {
  var raster: Raster[Tile] = _
  var geom: Geometry = _

  var multibandRaster: Raster[MultibandTile] = _
  var multibandGeom: Geometry = _
  val geotiffPath = "/Users/andrew/src/geotrellis/bench/src/main/resources"

  @Setup(Level.Trial)
  def setup(): Unit = {
    val geotiff = SinglebandGeoTiff(s"${geotiffPath}/singleband.tif")
    raster = Raster(geotiff.tile.toArrayTile, geotiff.extent)
    geom  = geotiff.extent.toPolygon

    val multibandGeoTiff = MultibandGeoTiff(s"${geotiffPath}/multiband.tif")
    multibandRaster = Raster(multibandGeoTiff.tile.toArrayTile, multibandGeoTiff.extent)
    multibandGeom  = multibandGeoTiff.extent.toPolygon
  }

  @Benchmark
  def singlebandVisitor: PolygonalSummaryResult[Option[Double]] =
    raster.polygonalSummary(geom, MaxVisitor)

  @Benchmark
  def multibandVisitor: PolygonalSummaryResult[Array[Option[Double]]] =
    multibandRaster.polygonalSummary(multibandGeom, MaxVisitor)
}
