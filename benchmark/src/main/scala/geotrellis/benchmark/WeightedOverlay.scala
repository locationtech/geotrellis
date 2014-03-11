/**************************************************************************
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
 **************************************************************************/

package geotrellis.benchmark

import geotrellis._
import geotrellis.source._
import geotrellis.raster.op._
import geotrellis.raster.op.local._
import geotrellis.statistics.op.stat._
import geotrellis.io._
import geotrellis.render.op._

import com.google.caliper.Param

import scala.util.Random

object WeightedOverlay extends BenchmarkRunner(classOf[WeightedOverlay])
class WeightedOverlay extends OperationBenchmark {
  val n = 4
  val names = Array("SBN_farm_mkt", "SBN_RR_stops_walk", "SBN_inc_percap", "SBN_street_den_1k")
  val weights = Array(2, 1, 5, 2)
  val colors = Array(0x0000FF, 0x0080FF, 0x00FF80, 0xFFFF00, 0xFF8000, 0xFF0000)

  @Param(Array("256","512", "1024", "2048", "4096"))
  var size:Int = 0

  var op:Op[Png] = null
  var source:ValueSource[Png] = null
  var sourceSeq:ValueSource[Png] = null

  override def setUp() {
    val re = getRasterExtent(names(0), size, size)
    val total = weights.sum
    val rs = (0 until n).map(i => Multiply(LoadRaster(names(i), re), weights(i)))
    val weightedAdd = Add(rs: _*)
    val divided = Divide(weightedAdd, total)
    op = 
      divided flatMap { r =>
        GetHistogram(r) flatMap { h =>
          val breaksOp = GetColorBreaks(h, colors)
          RenderPng(r, breaksOp, 0)
        }
      }

    source = 
      (0 until n).map(i => RasterSource(names(i),re) * weights(i))
                 .reduce(_+_)
                 .localDivide(total)
                 .renderPng(colors)

    sourceSeq = 
      (0 until n).map(i => RasterSource(names(i),re) * weights(i))
                 .localAdd
                 .localDivide(total)
                 .renderPng(colors)
  }

  def timeWeightedOverlayOp(reps:Int) = run(reps)(weightedOverlayOp)
  def weightedOverlayOp = get(op)

  def timeWeightedOverlaySource(reps:Int) = run(reps)(weightedOverlaySource)
  def weightedOverlaySource = get(source)

  def timeWeightedOverlaySourceSeq(reps:Int) = run(reps)(weightedOverlaySourceSeq)
  def weightedOverlaySourceSeq = get(sourceSeq)
}

object WeightedOverlayOverTypes extends BenchmarkRunner(classOf[WeightedOverlayOverTypes])
class WeightedOverlayOverTypes extends OperationBenchmark {
  @Param(Array("256","512", "1024"))
  var size:Int = 0

  @Param(Array("bit","byte","short","int","float","double"))
  var rasterType = ""

  val layers = 
    Map(
      ("bit","wm_DevelopedLand"),
      ("byte", "SBN_car_share"),
      ("short","travelshed-int16"),
      ("int","travelshed-int32"),
      ("float","aspect"), 
      ("double","aspect-double")
    )

  @Param(Array("4","8","16"))
  var layerCount = 0

  val colors = Array(0x0000FF, 0x0080FF, 0x00FF80, 0xFFFF00, 0xFF8000, 0xFF0000)

  var op:Op[Png] = null
  var source:ValueSource[Png] = null
  var sourceSeq:ValueSource[Png] = null

  override def setUp() {
    val weights = (0 until layerCount).map(i => Random.nextInt).toArray
    val re = getRasterExtent(layers(rasterType), size, size)
    val total = weights.sum
    val rs = (0 until layerCount).map(i => Multiply(LoadRaster(layers(rasterType), re), weights(i)))
    val weightedAdd = Add(rs: _*)
    val divided = Divide(weightedAdd, total)
    op = 
      divided flatMap { r =>
        GetHistogram(r) flatMap { h =>
          val breaksOp = GetColorBreaks(h, colors)
          RenderPng(r, breaksOp, 0)
        }
      }

    source = 
      (0 until layerCount).map(i => RasterSource(layers(rasterType),re) * weights(i))
                          .reduce(_+_)
                          .localDivide(total)
                          .renderPng(colors)

    sourceSeq = 
      (0 until layerCount).map(i => RasterSource(layers(rasterType),re) * weights(i))
                          .localAdd
                          .localDivide(total)
                          .renderPng(colors)
  }

  def timeWeightedOverlayOp(reps:Int) = run(reps)(weightedOverlayOp)
  def weightedOverlayOp = get(op)

  def timeWeightedOverlaySource(reps:Int) = run(reps)(weightedOverlaySource)
  def weightedOverlaySource = get(source)

  def timeWeightedOverlaySourceSeq(reps:Int) = run(reps)(weightedOverlaySourceSeq)
  def weightedOverlaySourceSeq = get(sourceSeq)
}
