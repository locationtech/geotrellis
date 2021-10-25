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

package geotrellis.vector.check

import geotrellis.vector._

import org.scalacheck._
import Gen._

object Generators {
  lazy val genPoint: Gen[Point] =
    for {
      x <- choose(-99999999999999999999.0,99999999999999999999.0)
      y <- choose(-99999999999999999999.0,99999999999999999999.0)
    } yield Point(x, y)

  lazy val genLine:Gen[LineString] =
    for {
      size <-Gen.choose(2,40)
      points <- Gen.containerOfN[Seq,Point](size,genPoint)
    } yield LineString(points.toList)

  // Doesn't yet deal with interior rings
  lazy val genPolygon:Gen[Polygon] =
    for {
      size <-Gen.choose(6,50)
      shareSize <- Gen.choose(3,size)
      subSize1 <- Gen.choose(3,size)
      subSize2 <- Gen.choose(3,size)
      fullSet <- Gen.containerOfN[Seq,Point](size,genPoint)
      sharedSet <- Gen.pick(shareSize,fullSet).map(_.toSeq)
      subSet1 <- Gen.pick(subSize1,fullSet).map(_.toSeq)
      subSet2 <- Gen.pick(subSize2,fullSet).map(_.toSeq)
    } yield {
      val polyOne = MultiPoint(subSet1 ++ sharedSet).convexHull.asInstanceOf[PolygonOrNoResult].as[Polygon].get
      val polyTwo = MultiPoint(subSet2 ++ sharedSet).convexHull.asInstanceOf[PolygonOrNoResult].as[Polygon].get
      (polyOne union polyTwo).asInstanceOf[TwoDimensionsTwoDimensionsUnionResult] match {
        case PolygonResult(p) => p
        case _ => sys.error("Should have resulted in a polygon.")
      }
    }

  implicit lazy val arbPoint: Arbitrary[Point] =
    Arbitrary(genPoint)

  implicit lazy val arbLine: Arbitrary[LineString] =
    Arbitrary(genLine)

  implicit lazy val arbPolygon: Arbitrary[Polygon] =
    Arbitrary(genPolygon)
}
