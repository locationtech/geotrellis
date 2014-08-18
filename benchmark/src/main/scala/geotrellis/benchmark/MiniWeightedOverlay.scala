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

import geotrellis.raster._
import geotrellis.raster.op.local._
import geotrellis.engine._
import geotrellis.engine.op.local._

import com.google.caliper.Param

object MiniWeightedOverlay extends BenchmarkRunner(classOf[MiniWeightedOverlay])
class MiniWeightedOverlay extends OperationBenchmark {
  @Param(Array("256", "512", "1024"))
  var size: Int = 0

  @Param(Array("bit", "byte", "short", "int", "float", "double"))
//  @Param(Array("bit"))
  var cellType = ""

  val layers = 
    Map(
      ("bit", "wm_DevelopedLand"),
      ("byte", "SBN_car_share"),
      ("short", "travelshed-int16"),
      ("int", "travelshed-int32"),
      ("float", "aspect"), 
      ("double", "aspect-double")
    )

  var source: RasterSource = null

  override def setUp() {
    val name = layers(cellType)
    val re = getRasterExtent(name, size, size)

    source = (RasterSource(name, re) * 5) + (RasterSource(name, re) * 2)
  }

  def timeMiniWeightedOverlaySource(reps: Int) = run(reps)(miniWeightedOverlaySource)
  def miniWeightedOverlaySource = get(source)
}

object SmallIOBenchmark extends BenchmarkRunner(classOf[SmallIOBenchmark])
class SmallIOBenchmark extends OperationBenchmark {
  @Param(Array("256", "512", "1024"))
//  @Param(Array("256"))
  var size: Int = 0

  @Param(Array("bit", "byte", "short", "int", "float", "double"))
//  @Param(Array("float"))
  var cellType = ""

  val path = "/home/rob/proj/gt/geotrellis/benchmark/src/main/resources/data/aspect.arg"

  val layers = 
    Map(
      ("bit", "wm_DevelopedLand"),
      ("byte", "SBN_car_share"),
      ("short", "travelshed-int16"),
      ("int", "travelshed-int32"),
      ("float", "aspect"), 
      ("double", "aspect-double")
    )

  var source: RasterSource = null
  var op: Op[Tile] = null

  var re: RasterExtent = null
  var baseRe: RasterExtent = null

  override def setUp() {
    val name = layers(cellType)
    baseRe = get(geotrellis.engine.io.LoadRasterExtent(name))
    re = getRasterExtent(name, size, size)

    op = geotrellis.engine.io.LoadRaster(name, re)
    source = RasterSource(name, re)
  }

  def timeLoadTheRaster(reps: Int) = run(reps)(loadTheRaster)
  def loadTheRaster = get(source)

  def timeLoadTheRasterOp(reps: Int) = run(reps)(loadTheRasterOp)
  def loadTheRasterOp = get(op)

  def timeReader(reps: Int) = run(reps)(reader)
  def reader = geotrellis.raster.io.arg.ArgReader.read(path, TypeFloat, baseRe, re)

  def timeLoadTwoRaster(reps: Int) = run(reps)(loadTwoRaster)
  def loadTwoRaster = get(geotrellis.engine.io.LoadRaster(layers(cellType)))
}
