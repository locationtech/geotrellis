/***
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
 ***/

package geotrellis.benchmark

import geotrellis._
import geotrellis.process._
import geotrellis.source._
import geotrellis.raster.op._
import geotrellis.raster._
import geotrellis.io._

import com.google.caliper.Benchmark
import com.google.caliper.Param
import com.google.caliper.Runner 
import com.google.caliper.SimpleBenchmark

import scala.math.{min, max}
import scala.util.Random

object FocalOperationsBenchmark extends BenchmarkRunner(classOf[FocalOperationsBenchmark])
class FocalOperationsBenchmark extends OperationBenchmark {

  var r:Raster = null

  var rs:RasterSource = null

  var tiledRS256:RasterSource = null
  var tiledRS512:RasterSource = null

  override def setUp() {
    val name = "SBN_inc_percap"

    val e = Extent(-8475497.88485957, 4825540.69147447,
                   -8317922.884859569, 4954765.69147447)
    val re = RasterExtent(e, 75.0, 75.0, 2101, 1723)
    r = get(LoadRaster(name, re))

    rs = RasterSource(name,re).cached

    tiledRS256 = {
      val tileLayout = TileLayout.fromTileDimensions(re,256,256)
      RasterSource(TileRaster.wrap(r,tileLayout,cropped = false))
    }
    tiledRS512 = {
      val tileLayout = TileLayout.fromTileDimensions(re,512,512)
      RasterSource(TileRaster.wrap(r,tileLayout,cropped = false))
    }
  }

  def timeConway(reps:Int) = run(reps)(get(focal.Conway(r)))

  def timeHillshade(reps:Int) = run(reps)(get(focal.Hillshade(r)))
  def timeSlope(reps:Int) = run(reps)(get(focal.Slope(r,1.0)))
  def timeAspect(reps:Int) = run(reps)(get(focal.Aspect(r)))

  def timeMax(reps:Int) = run(reps)(get(focal.Max(r,focal.Square(1))))

  def timeStandardDeviationSquare1(reps:Int) = run(reps)(get(focal.StandardDeviation(r, focal.Square(1))))
  def timeStandardDeviationSquare3(reps:Int) = run(reps)(get(focal.StandardDeviation(r, focal.Square(3))))
  def timeStandardDeviationCircle1(reps:Int) = run(reps)(get(focal.StandardDeviation(r, focal.Circle(1))))
  def timeStandardDeviationCircle2(reps:Int) = run(reps)(get(focal.StandardDeviation(r, focal.Circle(2))))

  // Mean operation, compare it against the FastFocalMean
  def timeMeanSquare1(reps:Int) = run(reps)(get(focal.Mean(r, focal.Square(1))))
  def timeMeanSquare3(reps:Int) = run(reps)(get(focal.Mean(r, focal.Square(3))))
  def timeMeanSquare7(reps:Int) = run(reps)(get(focal.Mean(r, focal.Square(7))))

  def timeMeanSquare3Tiled256(reps:Int) = run(reps)(get(tiledRS256.focalMean(focal.Square(3))))
  def timeMeanSquare3Tiled512(reps:Int) = run(reps)(get(tiledRS512.focalMean(focal.Square(3))))

  def timeFastMean1(reps:Int) = run(reps)(get(FastFocalMean(r, 1)))
  def timeFastMean3(reps:Int) = run(reps)(get(FastFocalMean(r, 3)))
  def timeFastMean7(reps:Int) = run(reps)(get(FastFocalMean(r, 7)))

  def timeMeanCircle1(reps:Int) = run(reps)(get(focal.Mean(r, focal.Circle(1))))
  def timeMeanCircle2(reps:Int) = run(reps)(get(focal.Mean(r, focal.Circle(2))))
  def timeMeanCircle3(reps:Int) = run(reps)(get(focal.Mean(r, focal.Circle(3))))
  def timeMeanCircle5(reps:Int) = run(reps)(get(focal.Mean(r, focal.Circle(5))))

//  def timeMeanCircle3Tiled256(reps:Int) = run(reps)(get(focal.Mean(tiledR256, focal.Circle(3))))

  def timeMedian(reps:Int) = run(reps)(get(focal.Median(r,focal.Square(1))))
  def timeMedianCircle1(reps:Int) = run(reps)(get(focal.Median(r,focal.Circle(1))))

  def timeMinSquare1(reps:Int) = run(reps)(get(focal.Min(r, focal.Square(1))))
//  def timeMinSquare1Tiled256(reps:Int) = run(reps)(get(focal.Min(tiledR256, focal.Square(1))))
  def timeMinSquare2(reps:Int) = run(reps)(get(focal.Min(r, focal.Square(2))))

  def timeMinCircle1(reps:Int) = run(reps)(get(focal.Min(r, focal.Circle(1))))
  def timeMinCircle2(reps:Int) = run(reps)(get(focal.Min(r, focal.Circle(2))))

  def timeMode(reps:Int) = run(reps)(get(focal.Mode(r,focal.Square(1))))
  def timeModeCircle3(reps:Int) = run(reps)(get(focal.Mode(r,focal.Circle(3))))

  def timeMoranMoran(reps:Int) = run(reps)(get(focal.RasterMoransI(r,focal.Square(1)))) // 

  def timeSum(reps:Int) = run(reps)(get(focal.Sum(r,focal.Square(1))))
//  def timeSumTiled256(reps:Int) = run(reps)(get(focal.Sum(tiledR256, focal.Square(1))))

  def timeSumSquare22(reps:Int) = run(reps)(get(focal.Sum(r,focal.Square(22))))
//  def timeSumSquare22Tiled512(reps:Int) = run(reps)(focal.Sum(tiledR512,focal.Square(22)))

  def timeConvolve(reps:Int) = run(reps)(get(global.Convolve(r,Kernel.gaussian(5,5.0,4.0,50.0))))
}
