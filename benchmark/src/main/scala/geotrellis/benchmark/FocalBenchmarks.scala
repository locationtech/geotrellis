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

import geotrellis.vector._
import geotrellis.engine._
import geotrellis.raster.op.focal._
import geotrellis.raster._

import com.google.caliper.Benchmark
import com.google.caliper.Param
import com.google.caliper.Runner 
import com.google.caliper.SimpleBenchmark
import geotrellis.raster.op.focal.{Square, Circle}
import geotrellis.raster.op.global

import scala.math.{min, max}
import scala.util.Random

object FocalOperationsBenchmark extends BenchmarkRunner(classOf[FocalOperationsBenchmark])
class FocalOperationsBenchmark extends OperationBenchmark {

  var re: RasterExtent = null
  var r: Tile = null

  var rs: RasterSource = null

  var tiledRS256: RasterSource = null
  var tiledRS512: RasterSource = null

  override def setUp() {
    val name = "SBN_inc_percap"

    val extent = Extent(-8475497.88485957, 4825540.69147447,
                   -8317922.884859569, 4954765.69147447)
    re = RasterExtent(extent, 75.0, 75.0, 2101, 1723)
    r = RasterSource(name, re).get

    rs = RasterSource(name, re).cached

    tiledRS256 = {
      val tileCols = (re.cols + 256 - 1) / 256
      val tileRows = (re.rows + 256 - 1) / 256
      val tileLayout = TileLayout(tileCols, tileRows, 256, 256)
      RasterSource(CompositeTile.wrap(r, tileLayout, cropped = false), extent)
    }
    tiledRS512 = {
      val tileCols = (re.cols + 512 - 1) / 512
      val tileRows = (re.rows + 512 - 1) / 512
      val tileLayout = TileLayout(tileCols, tileRows, 512, 512)
      RasterSource(CompositeTile.wrap(r, tileLayout, cropped = false), extent)
    }
  }

  def timeConway(reps: Int) = run(reps)(get(r.focalConway()))

  def timeHillshade(reps: Int) = run(reps)(get(r.hillshade(re.cellSize)))
  def timeSlope(reps: Int) = run(reps)(get(r.slope(re.cellSize, 1.0)))
  def timeAspect(reps: Int) = run(reps)(get(r.aspect(re.cellSize)))

  def timeMax(reps: Int) = run(reps)(get(r.focalMax(Square(1))))

//  def timeStandardDeviationSquare1(reps: Int) = run(reps)(get(r.focalStandardDeviation(Square(1))))
//  def timeStandardDeviationSquare3(reps: Int) = run(reps)(get(r.focalStandardDeviation(Square(3))))
//  def timeStandardDeviationCircle1(reps: Int) = run(reps)(get(r.focalStandardDeviation(Circle(1))))
//  def timeStandardDeviationCircle2(reps: Int) = run(reps)(get(r.focalStandardDeviation(Circle(2))))

  // Mean operation, compare it against the FastFocalMean
  def timeMeanSquare1(reps: Int) = run(reps)(get(Mean(r, Square(1))))
  def timeMeanSquare3(reps: Int) = run(reps)(get(Mean(r, Square(3))))
  def timeMeanSquare7(reps: Int) = run(reps)(get(Mean(r, Square(7))))

  def timeMeanSquare3Tiled256(reps: Int) = run(reps)(get(tiledRS256.focalMean(Square(3))))
  def timeMeanSquare3Tiled512(reps: Int) = run(reps)(get(tiledRS512.focalMean(Square(3))))

  def timeFastMean1(reps: Int) = run(reps)(get(FastFocalMean(r, 1)))
  def timeFastMean3(reps: Int) = run(reps)(get(FastFocalMean(r, 3)))
  def timeFastMean7(reps: Int) = run(reps)(get(FastFocalMean(r, 7)))

  def timeMeanCircle1(reps: Int) = run(reps)(get(Mean(r, Circle(1))))
  def timeMeanCircle2(reps: Int) = run(reps)(get(Mean(r, Circle(2))))
  def timeMeanCircle3(reps: Int) = run(reps)(get(Mean(r, Circle(3))))
  def timeMeanCircle5(reps: Int) = run(reps)(get(Mean(r, Circle(5))))

//  def timeMeanCircle3Tiled256(reps: Int) = run(reps)(get(Mean(tiledR256, Circle(3))))

  def timeMedian(reps: Int) = run(reps)(get(r.focalMedian(Square(1))))
  def timeMedianCircle1(reps: Int) = run(reps)(get(r.focalMedian(Circle(1))))

  def timeMinSquare1(reps: Int) = run(reps)(get(Min(r, Square(1))))
//  def timeMinSquare1Tiled256(reps: Int) = run(reps)(get(Min(tiledR256, Square(1))))
  def timeMinSquare2(reps: Int) = run(reps)(get(Min(r, Square(2))))

  def timeMinCircle1(reps: Int) = run(reps)(get(Min(r, Circle(1))))
  def timeMinCircle2(reps: Int) = run(reps)(get(Min(r, Circle(2))))

  def timeMode(reps: Int) = run(reps)(get(Mode(r, Square(1))))
  def timeModeCircle3(reps: Int) = run(reps)(get(Mode(r, Circle(3))))

  def timeMoranMoran(reps: Int) = run(reps)(get(r.tileMoransI(Square(1)))) //

  def timeSum(reps: Int) = run(reps)(get(r.focalSum(Square(1))))
//  def timeSumTiled256(reps: Int) = run(reps)(get(Sum(tiledR256, Square(1))))

  def timeSumSquare22(reps: Int) = run(reps)(get(r.focalSum(Square(22))))
//  def timeSumSquare22Tiled512(reps: Int) = run(reps)(Sum(tiledR512, Square(22)))

  def timeConvolve(reps: Int) = run(reps)(get(global.Convolve(r, Kernel.gaussian(5, 4.0, 50.0))))
}
