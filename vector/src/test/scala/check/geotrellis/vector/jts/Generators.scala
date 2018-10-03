/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.vector.check.jts

import org.locationtech.jts.geom._
import org.scalacheck.Gen._
import org.scalacheck._

object Generators {
  val factory = new GeometryFactory()

  lazy val genCoordinate: Gen[Coordinate] = 
    for {
      x <- choose(-99999999999999999999.0,99999999999999999999.0)
      y <- choose(-99999999999999999999.0,99999999999999999999.0)
    } yield new Coordinate(x, y)

  lazy val genShortCoordinates: Gen[Coordinate] =
    for {
      x <- choose(-1.0,1.0)
      y <- choose(-1.0,1.0)
    } yield new Coordinate(x, y)

  lazy val genPoint:Gen[Point] = 
    genCoordinate.map(factory.createPoint(_))

  lazy val genMultiPoint:Gen[MultiPoint] = 
    for {
      size <- Gen.choose(1,100)
      coords <- Gen.containerOfN[Set,Coordinate](size,genCoordinate)
    } yield { factory.createMultiPoint(coords.toArray) }

  lazy val genLongLineString:Gen[LineString] =
    for {
      size <-Gen.choose(2,40)
      s <- Gen.containerOfN[Set,Coordinate](size,genCoordinate)
    } yield { factory.createLineString(s.toArray) }

  lazy val genShortLineString:Gen[LineString] =
    for {
      size <-Gen.choose(2,40)
      s <- Gen.containerOfN[Set,Coordinate](size,genCoordinate)
    } yield { factory.createLineString(s.toArray) }

  lazy val genLinearRing:Gen[LinearRing] =
    genPolygon.map { p =>
      factory.createLinearRing(p.getExteriorRing.getCoordinateSequence)//.asInstanceOf[LinearRing]
    }

  lazy val genLineString:Gen[LineString] = 
    Gen.frequency((1,genLongLineString),(1,genShortLineString),(1,genLinearRing))

  lazy val genMultiLineString:Gen[MultiLineString] = 
    for {
      size <- Gen.choose(1,20)
      lineStrings <- Gen.containerOfN[Set,LineString](size,genLineString)
    } yield { factory.createMultiLineString(lineStrings.toArray) }

  // Doesn't yet deal with interior rings
  lazy val genPolygon:Gen[Polygon] =
    for {
      size <-Gen.choose(6,50)
      shareSize <- Gen.choose(3,size)
      subSize1 <- Gen.choose(3,size)
      subSize2 <- Gen.choose(3,size)
      fullSet <- Gen.containerOfN[Set,Coordinate](size,genCoordinate)
      sharedSet <- Gen.pick(shareSize,fullSet)
      subSet1 <- Gen.pick(subSize1,fullSet)
      subSet2 <- Gen.pick(subSize2,fullSet)
    } yield {
      val polyOne = 
        factory.createMultiPoint((subSet1 ++ sharedSet).toArray).convexHull.asInstanceOf[Polygon]
      val polyTwo =
        factory.createMultiPoint((subSet2 ++ sharedSet).toArray).convexHull.asInstanceOf[Polygon]
      polyOne.intersection(polyTwo).asInstanceOf[Polygon]
    }

  implicit lazy val arbCoordinate: Arbitrary[Coordinate] = 
    Arbitrary(genCoordinate)

  implicit lazy val arbPoint: Arbitrary[Point] =
    Arbitrary(genPoint)

  implicit lazy val arbMultiPoint: Arbitrary[MultiPoint] =
    Arbitrary(genMultiPoint)

  implicit lazy val arbLineString: Arbitrary[LineString] =
    Arbitrary(genLineString)

  implicit lazy val arbMultiLineString: Arbitrary[MultiLineString] =
    Arbitrary(genMultiLineString)

  implicit lazy val arbPolygon: Arbitrary[Polygon] =
    Arbitrary(genPolygon)

  // MultiPoint with a set of lines arbitrarily made up of the points
  // of the MultiPoint
  case class LineInMultiPoint(mp:MultiPoint,ls:LineString)
  lazy val genLineInMultiPoint:Gen[LineInMultiPoint] =
    for {
      size <- Gen.choose(2,100)
      lineSize <- Gen.choose(2,size)
      coords <- Gen.containerOfN[Set,Coordinate](size,genCoordinate)
      lineCoords <- Gen.pick(lineSize,coords)
    } yield { 
      val mp = factory.createMultiPoint(coords.toArray) 
      val l = factory.createLineString(lineCoords.toArray)
      LineInMultiPoint(mp,l)
    }

  implicit lazy val arbLineInMultiPoint:Arbitrary[LineInMultiPoint] =
    Arbitrary(genLineInMultiPoint)

  case class ClosedLineString(ls:LineString)
  lazy val genClosedLineString:Gen[ClosedLineString] =
    Gen.frequency((1,genLongLineString),(1,genShortLineString))
       .map { l => 
         val coords = l.getCoordinates
         ClosedLineString(factory.createLineString((coords(coords.length-1) ::coords.toList).toArray))
       }

  implicit lazy val arbClosedRing:Arbitrary[ClosedLineString] =
    Arbitrary(genClosedLineString)

}
