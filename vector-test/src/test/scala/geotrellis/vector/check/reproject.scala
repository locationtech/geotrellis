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

package geotrellis.vector.check

import geotrellis.vector._
import geotrellis.vector.reproject._
import geotrellis.proj4._

import org.scalacheck._
import Prop._
import Gen._
import Arbitrary._

object LatLngToWebMercatorCheck extends Properties("Reproject[LatLng,WebMercator]") {
  lazy val genLatLngPoint: Gen[Point] = 
    for {
      x <- choose(-180,179.9999)
      y <- choose(-90,89.9999)
    } yield Point(x, y)

  implicit lazy val arbLatLngPoint: Arbitrary[Point] =
    Arbitrary(genLatLngPoint)

  val proj4Transform = Transform(LatLng, WebMercator)
  val manualTransform = LatLng.toWebMercator _

  val tolerance = 0.00001

  property("alternateTransform[LatLng -> WebMercator]") = forAll { (p: Point) =>
    val point1 = proj4Transform(p.x, p.y)
    val point2 = manualTransform(p.x, p.y)
    val (dx, dy) = (math.abs(point1.x - point2.x), math.abs(point1.y - point2.y))
    if(dx > tolerance || dy > tolerance) {
      println(s"$point1 != $point2")
      false
    } else true
  }
}

object WebMercatorToLatLngCheck extends Properties("Reproject[WebMecator,LatLng]") {
  lazy val genWebMercatorPoint: Gen[Point] = 
    for {
      x <- choose(-20037508.342789244,20037507.229594335)
      y <- choose(-74299743.400575560,88985946.578292400)
    } yield Point(x, y)

  implicit lazy val arbWebMercatorPoint: Arbitrary[Point] =
    Arbitrary(genWebMercatorPoint)

  val proj4Transform = Transform(WebMercator, LatLng)
  val manualTransform = WebMercator.toLatLng _


  property("alternateTransform[WebMercator -> LatLng]") = forAll { (p: Point) =>
    val point1 = proj4Transform(p.x, p.y)
    val point2 = manualTransform(p.x, p.y)
    point1 == point2
  }
}
