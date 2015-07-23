/*
 * Copyright (c) 2015 Azavea.
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

package geotrellis.vector.interpolation

import geotrellis.vector.io.json._
import geotrellis.testkit._
import geotrellis.vector._

import spray.json.DefaultJsonProtocol._
import spire.syntax.cfor._

import org.scalatest._

class KrigingVectorInterpolationSpec extends FunSpec
with TestEngine{

  def generatePoints(pointsData : Array[PointFeature[Double]]): Array[PointFeature[Double]] = {
    (1 to pointsData.size).filterNot(_ % 5 == 0).map { i => pointsData(i-1)}.toArray
  }
  def generateLogPoints(pointsData : Array[PointFeature[Double]]): Array[PointFeature[Double]] = {
    (1 to pointsData.size).filter(_ % 1 == 0).map { i => PointFeature(pointsData(i-1).geom, math.log(pointsData(i-1).data))}.toArray
  }

  describe("Kriging Simple Interpolation(vector) : Nickel") {
    val path = "raster-test/data/nickel.json"
    val f = scala.io.Source.fromFile(path)
    val collection = f.mkString.parseGeoJson[JsonFeatureCollection]
    f.close()
    it("should return correct prediction value") {
      val points : Array[PointFeature[Double]] = generateLogPoints(collection.getAllPointFeatures[Double]().toArray)
      val testPointFeatures : Array[PointFeature[Double]] = Array(PointFeature(Point(659000, 586000), 3.0488))
      val testPoints: Array[Point] = Array.tabulate(testPointFeatures.length){i => testPointFeatures(i).geom}
      val sv: Semivariogram = NonLinearSemivariogram(points, 30000, 0, Spherical)
      val svParam: Array[Double] = Array(Semivariogram.r, Semivariogram.s, Semivariogram.a)
      val krigingVal: Array[(Double, Double)] = new KrigingSimple(points, 5000, svParam, Spherical).predict(testPoints)
      val E = 1e-4
      var error: List[Double] = Nil
      cfor(0)(_ < testPoints.length, _ + 1) { i =>
        error = error :+ math.abs(krigingVal(i)._1 - testPointFeatures(i).data)
        krigingVal(i)._1 should be (testPointFeatures(i).data +- E)
      }
    }
  }

  describe("Kriging Ordinary Interpolation(vector) : Nickel") {
    val path = "raster-test/data/nickel.json"
    val f = scala.io.Source.fromFile(path)
    val collection = f.mkString.parseGeoJson[JsonFeatureCollection]
    f.close()
    it("should return correct prediction value") {
      val points : Array[PointFeature[Double]] = generateLogPoints(collection.getAllPointFeatures[Double]().toArray)
      val testPointFeatures : Seq[PointFeature[Double]] = Seq{PointFeature(Point(659000, 586000), 3.0461)}
      val testPoints: Array[Point] = Array.tabulate(testPointFeatures.length){i => testPointFeatures(i).geom}
      val sv: Semivariogram = NonLinearSemivariogram(points, 30000, 0, Spherical)
      val svParam: Array[Double] = Array(Semivariogram.r, Semivariogram.s, Semivariogram.a)
      val krigingVal: Array[(Double, Double)] = new KrigingOrdinary(points, 5000, svParam, Spherical).predict(testPoints)
      val E = 1e-4
      var error: List[Double] = Nil
      cfor(0)(_ < testPoints.length, _ + 1) { i =>
        error = error :+ math.abs(krigingVal(i)._1 - testPointFeatures(i).data)
        krigingVal(i)._1 should be (testPointFeatures(i).data +- E)
      }
    }
  }

  describe("Kriging Universal Interpolation(vector) : Cobalt") {

    val path = "raster-test/data/cobalt.json"
    val f = scala.io.Source.fromFile(path)
    val collection = f.mkString.parseGeoJson[JsonFeatureCollection]
    f.close()
    ignore("should return correct prediction value") {
      val points : Array[PointFeature[Double]] = collection.getAllPointFeatures[Double]().toArray
      val testPointFeatures : Array[PointFeature[Double]] = Array(PointFeature(Point(659000, 586000), 3.0461))
      val testPoints: Array[Point] = Array.tabulate(testPointFeatures.length){i => testPointFeatures(i).geom}
      val sv: Semivariogram = NonLinearSemivariogram(points, 0, 0, Spherical)
      val svParam: Array[Double] = Array(Semivariogram.r, Semivariogram.s, Semivariogram.a)
      println("The cobalt values are : " + svParam.mkString(" "))
      /*val krigingVal: Array[(Double, Double)] = new KrigingOrdinary(points, 5000, sv, svParam, Spherical).predict(testPoints)
      println("krigingVal's length = " + krigingVal.length)
      val E = 1e-4
      var error: List[Double] = Nil
      cfor(0)(_ < testPoints.length, _ + 1) { i =>
        println(testPoints(i) + " => " + krigingVal(i) + " vs " + testPointFeatures(i).data)
        error = error :+ math.abs(krigingVal(i)._1 - testPointFeatures(i).data)
        krigingVal(i)._1 should be (testPointFeatures(i).data +- E)
      }*/
    }
  }
}
