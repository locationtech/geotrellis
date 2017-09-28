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

package geotrellis.raster.resample

import geotrellis.proj4.{LatLng, Transform, WebMercator}
import geotrellis.raster.{Raster, RasterExtent, SinglebandRaster, Tile}
import org.openjdk.jmh.annotations.{Mode ⇒ JMHMode, _}
import geotrellis.bench._

@BenchmarkMode(Array(JMHMode.AverageTime))
@State(Scope.Thread)
class ResampleBench {
  @Param(Array("NearestNeighbor", "Bilinear", "CubicConvolution", "CubicSpline", "Lanczos", "Average", "Mode"  ))
  var methodName: String = _

  var method: ResampleMethod = _
  var raster: Raster[Tile] = _
  var rasterExtent: RasterExtent = _
  var trans: Transform = _
  var invTrans: Transform = _

  @Setup(Level.Trial)
  def setup(): Unit = {
    val geotiff = readSinglebandGeoTiff("aspect-tif.tif")
    method = methodName match {
      case "NearestNeighbor" ⇒ NearestNeighbor
      case "Bilinear" ⇒ Bilinear
      case "CubicConvolution" ⇒ CubicConvolution
      case "CubicSpline" ⇒ CubicSpline
      case "Lanczos" ⇒ Lanczos
      case "Average" ⇒ Average
      case "Mode" ⇒ Mode
    }
    trans = Transform(LatLng, WebMercator)
    invTrans = Transform(WebMercator, LatLng)
    raster = geotiff.raster
    rasterExtent = raster.rasterExtent
  }

  @Benchmark
  def reproject: SinglebandRaster =
    raster.reproject(rasterExtent, trans, invTrans, method)
}
