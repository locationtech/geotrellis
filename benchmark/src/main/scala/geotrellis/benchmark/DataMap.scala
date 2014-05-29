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
import geotrellis.feature._
import geotrellis.raster._
import geotrellis.raster.op.local._

import com.google.caliper.Param

import scala.util.Random
import scala.annotation.tailrec

object DataMap extends BenchmarkRunner(classOf[DataMap])
class DataMap extends OperationBenchmark {
  //@Param(Array("64", "128", "256", "512", "1024", "2048", "4096"))
  @Param(Array("2048"))
  var size:Int = 0

  var ints:Array[Int] = null
  var doubles:Array[Double] = null
  var raster:Raster = null
  var bitData:BitArrayRasterData = null
  var byteData:ByteArrayRasterData = null
  var shortData:ShortArrayRasterData = null

  override def setUp() {
    val len = size * size
    ints = init(len)(Random.nextInt)
    doubles = init(len)(Random.nextDouble)
    val re = RasterExtent(Extent(0, 0, size, size), 1.0, 1.0, size, size)
    raster = Raster(init(len)(Random.nextInt), re)

    bitData = new BitArrayRasterData(init((len + 7) / 8)(Random.nextInt.toByte), size, size)
    byteData = new ByteArrayRasterData(init(len)(Random.nextInt.toByte), size, size)
    shortData = new ShortArrayRasterData(init(len)(Random.nextInt.toShort), size, size)
  }


  def timeIntArrayWhileLoop(reps:Int) = run(reps)(intArrayWhileLoop)
  def intArrayWhileLoop = {
    val goal = ints.clone
    var i = 0
    val len = goal.length
    while (i < len) {
      val z = goal(i)
      if (isData(z)) goal(i) = z * 2
      i += 1
    }
    goal
  }
 
  def timeIntArrayTailrec(reps:Int) = run(reps)(intArrayTailrec)
  def intArrayTailrec = {
    val goal = ints.clone
    val len = goal.length
    @inline @tailrec def loop(i:Int) {
      if (i < len) {
        val z = goal(i)
        if (isData(z)) goal(i) = z * 2
        loop(i + 1)
      }
    }
    loop(0)
    goal
  }
 
  def timeDoubleArrayWhileLoop(reps:Int) = run(reps)(doubleArrayWhileLoop)
  def doubleArrayWhileLoop = {
    val goal = doubles.clone
    var i = 0
    val len = goal.length
    while (i < len) {
      val z = goal(i)
      if (isData(z)) goal(i) = z * 2.0
      i += 1
    }
    goal
  }

  import spire.syntax.cfor._
  def timeIntArrayCforLoop(reps:Int) = run(reps)(intArrayCforLoop)
  def intArrayCforLoop = {
    val goal = ints.clone
    val len = goal.length
    cfor(0)(_ < len, _ + 1) { i =>
      val z = goal(i)
      if (isData(z)) goal(i) = z * 2
    }
    goal
  }

  import scalaxy.loops._

  def timeScalaxyLoop(reps:Int) = run(reps)(scalaxyLoop)
  def scalaxyLoop = {
    val goal = ints.clone
    val len = goal.length
    for(i <- 0 until len optimized)  {
      val z = goal(i)
      if (isData(z)) goal(i) = z * 2
    }
  }
 
  def timeRasterWhileLoop(reps:Int) = run(reps)(rasterWhileLoop)
  def rasterWhileLoop = {
    val rcopy = raster.toArrayRaster
    val goal = rcopy.data.mutable

    var i = 0
    val len = goal.length
    while (i < len) {
      val z = goal(i)
      if (isData(z)) goal(i) = goal(i) * 2
      i += 1
    }
    rcopy
  }
 
  def timeRasterMap(reps:Int) = run(reps)(rasterMap)
  def rasterMap = raster.map(z => if (isData(z)) z * 2 else NODATA)

  def timeRasterMapIfSet(reps:Int) = run(reps)(rasterMapIfSet)
  def rasterMapIfSet = raster.mapIfSet(z => z * 2)

  def timeBitDataWhileLoop(reps:Int) = run(reps)(bitDataWhileLoop)
  def bitDataWhileLoop = {
    val data = bitData.copy
    var i = 0
    val len = data.length
    while (i < len) {
      val z = data(i)
      if (isData(z)) data(i) = data(i) * 2
      i += 1
    }
    data
  }

  def timeBitDataMap(reps:Int) = run(reps)(bitDataMap)
  def bitDataMap = bitData.map(z => if (isData(z)) z * 2 else NODATA)

  def timeByteDataWhileLoop(reps:Int) = run(reps)(byteDataWhileLoop)
  def byteDataWhileLoop = {
    val data = byteData.copy
    var i = 0
    val len = data.length
    while (i < len) {
      val z = data(i)
      if (isData(z)) data(i) = data(i) * 2
      i += 1
    }
    data
  }
  
  def timeByteDataMap(reps:Int) = run(reps)(byteDataMap)
  def byteDataMap = byteData.map(z => if (isData(z)) z * 2 else NODATA)

  def timeShortDataWhileLoop(reps:Int) = run(reps)(shortDataWhileLoop)
  def shortDataWhileLoop = {
    val data = shortData.copy
    var i = 0
    val len = data.length
    while (i < len) {
      val z = data(i)
      if (isData(z)) data(i) = data(i) * 2
      i += 1
    }
    data
  }
  
  def timeShortDataMap(reps:Int) = run(reps)(shortDataMap)
  def shortDataMap = shortData.map(z => if (isData(z)) z * 2 else NODATA)
}

/** Result: Array.fill is really slow and should not be used */
object ArrayFill extends BenchmarkRunner(classOf[ArrayFill])
class ArrayFill extends OperationBenchmark {

//  @Param(Array("2048", "4096","8192"))
  @Param(Array("256"))
  var size:Int = 0

  def timeScalaArrayFillBytes(reps:Int) = run(reps)(scalaArrayFillBytes)
  def scalaArrayFillBytes = {
    val arr = Array.fill[Byte](size*size)(byteNODATA)
  }

  def timeJavaArraysFillBytes(reps:Int) = run(reps)(javaArraysFillBytes)
  def javaArraysFillBytes = {
    val arr = Array.ofDim[Byte](size*size)
    java.util.Arrays.fill(arr,byteNODATA)
  }

  def timeFillerBytes(reps:Int) = run(reps)(fillerBytes)
  def fillerBytes = {
    Array.ofDim[Byte](size*size).fill(byteNODATA)
  }

  def timeScalaArrayFillFloats(reps:Int) = run(reps)(scalaArrayFillFloats)
  def scalaArrayFillFloats = {
    val arr = Array.fill[Float](size*size)(Float.NaN)
  }

  def timeJavaArraysFillFloats(reps:Int) = run(reps)(javaArraysFillFloats)
  def javaArraysFillFloats = {
    val arr = Array.ofDim[Float](size*size)
    java.util.Arrays.fill(arr,Float.NaN)
  }

  def timeFillerFloats(reps:Int) = run(reps)(fillerFloats)
  def fillerFloats = {
    Array.ofDim[Float](size*size).fill(Float.NaN)
  }

  def timeScalaArrayFillDoubles(reps:Int) = run(reps)(scalaArrayFillDoubles)
  def scalaArrayFillDoubles = {
    val arr = Array.fill[Double](size*size)(Double.NaN)
  }

  def timeJavaArraysFillDoubles(reps:Int) = run(reps)(javaArraysFillDoubles)
  def javaArraysFillDoubles = {
    val arr = Array.ofDim[Double](size*size)
    java.util.Arrays.fill(arr,Double.NaN)
  }

  def timeFillerDoubles(reps:Int) = run(reps)(fillerDoubles)
  def fillerDoubles = {
    Array.ofDim[Double](size*size).fill(Double.NaN)
  }

}
