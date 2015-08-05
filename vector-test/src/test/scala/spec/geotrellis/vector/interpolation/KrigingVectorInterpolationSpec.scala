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

import java.io.BufferedReader

import geotrellis.vector.io.json._
import geotrellis.testkit._
import geotrellis.vector._
import org.parboiled.common.FileUtils

import spray.json.DefaultJsonProtocol._
import spire.syntax.cfor._

import org.scalatest._

import scala.util.parsing.json.JSON

class KrigingVectorInterpolationSpec extends FunSpec
with TestEngine{
  def generateLogPoints(pointsData : Array[PointFeature[Double]]): Array[PointFeature[Double]] = {
    (1 to pointsData.length).map { i => PointFeature(pointsData(i-1).geom, math.log(pointsData(i-1).data)) }.toArray
  }

  describe("Kriging Simple Interpolation(vector) : Nickel") {
    val path = "raster-test/data/nickel.json"
    val f = scala.io.Source.fromFile(path)
    val collection = f.mkString.parseGeoJson[JsonFeatureCollection]
    f.close()
    it("should return correct prediction value") {
      val points : Array[PointFeature[Double]] = generateLogPoints(collection.getAllPointFeatures[Double]().toArray)
      val testPointFeatures : Array[PointFeature[Double]] = Array(PointFeature(Point(659000, 586000), 3.0488))
      val testPoints: Array[Point] = Array.tabulate(testPointFeatures.length){ i => testPointFeatures(i).geom }
      val sv: Semivariogram = NonLinearSemivariogram(points, 30000, 0, Spherical)
      val krigingVal: Array[(Double, Double)] = new KrigingSimple(points, 5000, sv).predict(testPoints)
      val E = 1e-4
      cfor(0)(_ < testPoints.length, _ + 1) { i =>
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
      val krigingVal: Array[(Double, Double)] = new KrigingOrdinary(points, 5000, sv).predict(testPoints)
      val E = 1e-4
      cfor(0)(_ < testPoints.length, _ + 1) { i =>
        krigingVal(i)._1 should be (testPointFeatures(i).data +- E)
      }
    }
  }

  describe("Kriging Universal Interpolation(vector) : Venice") {
    it("should return correct prediction value") {
      val points: Array[PointFeature[Double]] = Array(
        PointFeature(Point(720, 436), -0.99), PointFeature(Point(538, 397), -2.50), PointFeature(Point(518, 395), -1.18),
        PointFeature(Point(612, 365), 0.43), PointFeature(Point(562, 287), -1.66), PointFeature(Point(544, 248), -1.18),
        PointFeature(Point(626, 565), -0.43), PointFeature(Point(630, 551), -0.61), PointFeature(Point(568, 560), -0.83),
        PointFeature(Point(566, 524), -1.51), PointFeature(Point(496, 555), -0.57), PointFeature(Point(874, 468), -0.02),
        PointFeature(Point(450, 558), 0.08), PointFeature(Point(456, 502), -3.92), PointFeature(Point(464, 543), -1.83),
        PointFeature(Point(426, 490), -6.11), PointFeature(Point(408, 424), -4.98), PointFeature(Point(408, 417), -4.93),
        PointFeature(Point(430, 419), -5.92), PointFeature(Point(374, 570), 5.21), PointFeature(Point(338, 521), 6.63),
        PointFeature(Point(354, 480), -0.67), PointFeature(Point(770, 487), -0.89), PointFeature(Point(342, 424), -1.66),
        PointFeature(Point(396, 387), -4.79), PointFeature(Point(296, 421), 0.340), PointFeature(Point(270, 431), 2.29),
        PointFeature(Point(270, 409), 0.69), PointFeature(Point(302, 392), -0.83), PointFeature(Point(336, 409), -1.06),
        PointFeature(Point(346, 397), -1.53), PointFeature(Point(330, 375), -0.70), PointFeature(Point(350, 343), -0.46),
        PointFeature(Point(706, 404), -0.80), PointFeature(Point(322, 317), -0.28), PointFeature(Point(658, 477), -1.29),
        PointFeature(Point(662, 431), -1.36), PointFeature(Point(586, 392), -3.27), PointFeature(Point(572, 387), -2.50),
        PointFeature(Point(538, 385), -2.71))

      val attributesSample = Array.ofDim[Double](points.length, 3)
      cfor(0)(_ < points.length, _ + 1) { i =>
        val c1: Double = 0.01 * (0.873 * (points(i).geom.x - 418) - 0.488 * (points(i).geom.y - 458))
        val c2: Double = 0.01 * (0.488 * (points(i).geom.x - 418) + 0.873 * (points(i).geom.y - 458))
        val dl: Double = math.exp(-1.5 * c1 * c1 - c2 * c2)
        val dv: Double = math.exp(-1 * math.pow(math.sqrt(math.pow(points(i).geom.x - 560, 2) + math.pow(points(i).geom.y - 390, 2)) / 35, 8))
        val ev: Double = math.exp(-1 * c1)
        attributesSample(i) = Array(ev, dl, dv)
      }
      val s1: Range = 150 until 901 by 25
      val s2: Range = 650 until 199 by -25
      val location: Array[Point] = (for {x <- s1; y <- s2} yield Point(x, y)).toArray
      val attributes = Array.ofDim[Double](location.length, 3)
      cfor(0)(_ < location.length, _ + 1) { i =>
        val c1: Double = 0.01 * (0.873 * (location(i).x - 418) - 0.488 * (location(i).y - 458))
        val c2: Double = 0.01 * (0.488 * (location(i).x - 418) + 0.873 * (location(i).y - 458))
        val Dl: Double = math.exp(-1.5 * c1 * c1 - c2 * c2)
        val Dv: Double = math.exp(-1 * math.pow(math.sqrt(math.pow(location(i).x - 560, 2) + math.pow(location(i).y - 390, 2)) / 35, 8))
        val Ev: Double = math.exp(-1 * c1)
        attributes(i) = Array(Ev, Dl, Dv)
      }
      val E: Double = 0.0001

      val krigingVal: Array[(Double, Double)] = new KrigingUniversal(points, attributesSample, attributes, 50, Spherical).predict(location)
      val path = "raster-test/data/venice.json"
      val f = scala.io.Source.fromFile(path)
      val collection = f.mkString.parseGeoJson[JsonFeatureCollection]
      f.close()
      val veniceData = collection.getAllPointFeatures[Double]().toArray
      cfor(0)(_ < krigingVal.length, _ + 1) { i =>
        krigingVal(i)._1 should be (veniceData(i).data +- E)
      }
    }
  }

  describe("Kriging Geo Interpolation(vector) : Venice") {
    it("should return correct prediction value") {
      val points: Array[PointFeature[Double]] =   Array(PointFeature(Point(720, 436), -0.99), PointFeature(Point(538, 397), -2.50),
                                                        PointFeature(Point(518, 395), -1.18), PointFeature(Point(612, 365), 0.43),
                                                        PointFeature(Point(562, 287), -1.66), PointFeature(Point(544, 248), -1.18),
                                                        PointFeature(Point(626, 565), -0.43), PointFeature(Point(630, 551), -0.61),
                                                        PointFeature(Point(568, 560), -0.83), PointFeature(Point(566, 524), -1.51),
                                                        PointFeature(Point(496, 555), -0.57), PointFeature(Point(874, 468), -0.02),
                                                        PointFeature(Point(450, 558), 0.08), PointFeature(Point(456, 502), -3.92),
                                                        PointFeature(Point(464, 543), -1.83), PointFeature(Point(426, 490), -6.11),
                                                        PointFeature(Point(408, 424), -4.98), PointFeature(Point(408, 417), -4.93),
                                                        PointFeature(Point(430, 419), -5.92), PointFeature(Point(374, 570), 5.21),
                                                        PointFeature(Point(338, 521), 6.63), PointFeature(Point(354, 480), -0.67),
                                                        PointFeature(Point(770, 487), -0.89), PointFeature(Point(342, 424), -1.66),
                                                        PointFeature(Point(396, 387), -4.79), PointFeature(Point(296, 421), 0.340),
                                                        PointFeature(Point(270, 431), 2.29), PointFeature(Point(270, 409), 0.69),
                                                        PointFeature(Point(302, 392), -0.83), PointFeature(Point(336, 409), -1.06),
                                                        PointFeature(Point(346, 397), -1.53), PointFeature(Point(330, 375), -0.70),
                                                        PointFeature(Point(350, 343), -0.46), PointFeature(Point(706, 404), -0.80),
                                                        PointFeature(Point(322, 317), -0.28), PointFeature(Point(658, 477), -1.29),
                                                        PointFeature(Point(662, 431), -1.36), PointFeature(Point(586, 392), -3.27),
                                                        PointFeature(Point(572, 387), -2.50), PointFeature(Point(538, 385), -2.71))
      val attributesSample = Array.ofDim[Double](points.length, 3)
      cfor(0)(_ < points.length, _ + 1) { i =>
        val c1: Double = 0.01 * (0.873 * (points(i).geom.x - 418) - 0.488 * (points(i).geom.y - 458))
        val c2: Double = 0.01 * (0.488 * (points(i).geom.x - 418) + 0.873 * (points(i).geom.y - 458))
        val Dl: Double = math.exp(-1.5 * c1 * c1 - c2 * c2)
        val Dv: Double = math.exp(-1 * math.pow(math.sqrt(math.pow(points(i).geom.x - 560, 2) + math.pow(points(i).geom.y - 390, 2)) / 35, 8))
        val Ev: Double = math.exp(-1 * c1)
        attributesSample(i) = Array(Ev, Dl, Dv)
      }

      val testPointFeatures : Array[PointFeature[Double]] = Array(PointFeature(Point(659000, 586000), 3.0461))
      val testPoints: Array[Point] = Array.tabulate(testPointFeatures.length){i => testPointFeatures(i).geom}
      val s1: Range = 150 until 901 by 25
      val s2: Range = 650 until 199 by -25
      val location: Array[Point] = (for { x <- s1; y <- s2 } yield Point(x, y)).toArray
      val attributes = Array.ofDim[Double](location.length, 3)
      cfor(0)(_ < location.length, _ + 1) { i =>
        val c1: Double = 0.01 * (0.873 * (location(i).x - 418) - 0.488 * (location(i).y - 458))
        val c2: Double = 0.01 * (0.488 * (location(i).x - 418) + 0.873 * (location(i).y - 458))
        val Dl: Double = math.exp(-1.5 * c1 * c1 - c2 * c2)
        val Dv: Double = math.exp(-1 * math.pow(math.sqrt(math.pow(location(i).x - 560, 2) + math.pow(location(i).y - 390, 2)) / 35, 8))
        val Ev: Double = math.exp(-1 * c1)
        attributes(i) = Array(Ev, Dl, Dv)
      }
      val E: Double = 1.4
      val testingPointsGeo: Array[Point] = Array(Point(350, 425), Point(350, 400), Point(375, 475), Point(375, 450), Point(375, 425), Point(375, 400), Point(375, 375), Point(400, 500), Point(400, 375), Point(400, 350), Point(425, 525), Point(425, 375), Point(425, 350), Point(450, 525), Point(450, 375), Point(450, 350), Point(475, 525), Point(475, 500), Point(475, 425), Point(475, 400), Point(475, 375), Point(500, 525), Point(500, 500), Point(500, 475), Point(500, 450), Point(500, 425), Point(500, 400), Point(525, 500), Point(525, 475), Point(550, 400), Point(550, 375), Point(575, 400), Point(575, 375))
      val krigingValInsideVenice: Array[(Double, Double)] = new KrigingGeo(points, attributesSample, attributes, 50, Spherical).predict(location)
      var j = 0
      cfor(0)(i => i < location.length && j < testingPointsGeo.length, _ + 1) { i =>
        if(location(i).geom == testingPointsGeo(j)) {
          krigingValInsideVenice(i)._1 should be (-3.0 +- E)
          j = j + 1
        }
      }
    }
  }
}
