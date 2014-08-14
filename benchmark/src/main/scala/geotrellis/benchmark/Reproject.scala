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
import geotrellis.vector.reproject._
import geotrellis.proj4._
import geotrellis.vector.json._

import scala.util.Random

import com.google.caliper.Param

object ReprojectBenchmark extends BenchmarkRunner(classOf[ReprojectBenchmark])
class ReprojectBenchmark extends OperationBenchmark {
  @Param(Array("1000", "2000", "4000", "8000", "16000", "32000"))
  var size:Int = 0

  var pointsLL: Array[Point] = null
  var pointsWM: Array[Point] = null


  val latLngToWebMercatorProj4 = Transform(LatLng, WebMercator)
  val latLngToWebMercatorManual = LatLng.toWebMercator _

  val webMercatorToLatLngProj4 = Transform(WebMercator, LatLng)
  val webMercatorToLatLngManual = WebMercator.toLatLng _


  override def setUp() {
    val transitPoly = GeoJson.fromFile[Polygon]("../raster-test/data/transitgeo.json")
    val verts = transitPoly.vertices
    val len = verts.size
    pointsLL = (for(i <- 0 until size) yield { verts(i % len) }).toArray
    pointsWM = (for(i <- 0 until size) yield { verts(i % len).reproject(LatLng, WebMercator) }).toArray
  }

  def timeReprojectLatLngWebMercatorProj4(reps:Int) = run(reps)(reprojectLatLngWebMercatorProj4)
  def reprojectLatLngWebMercatorProj4 = {
    var i = 0
    var dummy: Double = 0.0
    while(i < size) {
      val p = pointsLL(i)
      val (x, y) = latLngToWebMercatorProj4(p.x, p.y)
      dummy += x + y
      i += 1
    }
  }

  def timeReprojectLatLngWebMercatorManual(reps:Int) = run(reps)(reprojectLatLngWebMercatorManual)
  def reprojectLatLngWebMercatorManual = {
    var i = 0
    var dummy: Double = 0.0
    while(i < size) {
      val p = pointsLL(i)
      val (x, y) = latLngToWebMercatorManual(p.x, p.y)
      dummy += x + y
      i += 1
    }
  }

  def timeReprojectWebMercatorLatLngProj4(reps:Int) = run(reps)(reprojectWebMercatorLatLngProj4)
  def reprojectWebMercatorLatLngProj4 = {
    var i = 0
    var dummy: Double = 0.0
    while(i < size) {
      val p = pointsWM(i)
      val (x, y) = webMercatorToLatLngProj4(p.x, p.y)
      dummy += x + y
      i += 1
    }
  }

  def timeReprojectWebMercatorLatLngManual(reps:Int) = run(reps)(reprojectWebMercatorLatLngManual)
  def reprojectWebMercatorLatLngManual = {
    var i = 0
    var dummy: Double = 0.0
    while(i < size) {
      val p = pointsWM(i)
      val (x, y) = webMercatorToLatLngManual(p.x, p.y)
      dummy += x + y
      i += 1
    }
  }

}

