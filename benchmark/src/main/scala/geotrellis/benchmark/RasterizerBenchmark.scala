/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.benchmark

import geotrellis._
import geotrellis.process._
import geotrellis.raster._
import geotrellis.raster.op._
import geotrellis.feature._

import com.google.caliper.Benchmark
import com.google.caliper.Param
import com.google.caliper.Runner 
import com.google.caliper.SimpleBenchmark

import scala.math.{min, max}
import scala.util.Random


object RasterizerBenchmark extends BenchmarkRunner(classOf[RasterizerBenchmark])
class RasterizerBenchmark extends OperationBenchmark {
  var r: Raster = _
  var re: RasterExtent = _
  var data: IntArrayRasterData = _
  var poly: feature.PolygonFeature[Int] = _

  @Param(Array("512","1024","2048","4096","8192"))
  var rasterSize: Int = 0

  override def setUp() {
    r = randomRasterN(rasterSize)
    // rasters go from 0,0 to 10n,10n so we'll stick
    // a triangle in here

    val p1 = Point(0,0)
    val p2 = Point(10*rasterSize,0)
    val p3 = Point(10*rasterSize/2, 10*rasterSize)
    poly = PolygonFeature(Polygon(Line(p1,p2,p3,p1)), 1)
  }

  def rasterize() {
    feature.rasterize.Rasterizer.foreachCellByFeature(poly.geom, re)(
      new feature.rasterize.Callback {
        def apply(col: Int, row: Int) {
          data.set(col,row,4)
        }
      })
  }

  //Because of a refactor Callback is not getting a geom as a param, since it can close over it if it really wanted
  //this renders the following benchmark pointless, but lets preserve this file in case other cases emerge
  def rasterizeUsingValue() {
    feature.rasterize.Rasterizer.foreachCellByFeature(poly.geom, re)(
      new feature.rasterize.Callback {
        def apply(col: Int, row: Int) {
          data.set(col,row, poly.data)
        }
      })
  }


  def timeRasterizer(reps:Int) = run(reps)(rasterize())
  def timeRasterizerUsingValue(reps:Int) = run(reps)(rasterize())

  def randomRasterN(n: Int) = {
    val a = Array.ofDim[Int](n*n).map(a => Random.nextInt(255))
    val e = Extent(0,0,10*n,10*n)
    re = RasterExtent(e, 10,10,n,n)
    data = IntArrayRasterData(a, n, n)

    Raster(data, re)
  }
}
