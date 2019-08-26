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

package geotrellis.raster.reproject

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.BenchmarkParams

import geotrellis.bench.init
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.vector._

import scala.util.Random

@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Thread)
class RasterizingReprojectBench  {
  import RasterizingReprojectBench._

  @Param(Array("32", "64", "128", "256", "512", "1024", "2048", "4096", "8192"))
  var size: Int = _

  var raster: ProjectedRaster[Tile] = _
  var destRE: ProjectedRasterExtent = _

  val transform: Transform = Transform(srcCrs, destCrs)
  val inverse: Transform = Transform(destCrs, srcCrs)

  @Setup(Level.Trial)
  def setup(params: BenchmarkParams): Unit = {
    val len = size * size
    raster = ProjectedRaster(ArrayTile(init(len)(Random.nextInt), size, size), srcExtent, srcCrs)
    destRE = ProjectedRasterExtent(raster.projectedExtent.reproject(destCrs), destCrs, size, size)
  }

  @Benchmark
  def standardReproject: Raster[Tile] = {
    raster.raster.reproject(destRE: RasterExtent, transform, inverse)
  }

  @Benchmark
  def rasterizingReproject: ProjectedRaster[Tile] = {
    raster.regionReproject(destRE, NearestNeighbor)
  }
}

object RasterizingReprojectBench {
  val srcCrs = LatLng
  val destCrs = ConusAlbers
  val srcExtent = Extent(-109, 37, -102, 41)
}
