/*
 * Copyright 2019 Azavea
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

package geotrellis.raster.summary.polygonal

import cats.Monad

import scala.annotation.tailrec

/**
  * A Result ADT returned by [[PolygonalSummary]] operations
  *
  * There are two result types, [[NoIntersection]] and [[Summary]].
  * This ADT will always return Summary if the Raster and Polygon intersect.
  * Otherwise, NoIntersection will be returned.
  *
  * Provides helpers to convert to Option and Either if you don't need to match
  * on result type.
  */
sealed trait PolygonalSummaryResult[+A] {
  def toOption: Option[A]

  def toEither: Either[Any, A]

  implicit val noIntersectionMonad: Monad[PolygonalSummaryResult] = new Monad[PolygonalSummaryResult] {
    def flatMap[A, B](fa: PolygonalSummaryResult[A])
                     (f: A => PolygonalSummaryResult[B]): PolygonalSummaryResult[B] = {
      flatten(map(fa)(f))
    }

    def pure[A](x: A): PolygonalSummaryResult[A] = Summary(x)

    @tailrec
    def tailRecM[A, B](a: A)(f: A => PolygonalSummaryResult[Either[A,B]]): PolygonalSummaryResult[B] = {
      f(a) match {
        case NoIntersection => NoIntersection
        case Summary(Left(nextA)) => tailRecM(nextA)(f)
        case Summary(Right(b)) => Summary(b)
      }
    }
  }
}

case object NoIntersection extends PolygonalSummaryResult[Nothing] {
  def toOption = None

  def toEither = Left(NoIntersection)
}

case class Summary[A](value: A) extends PolygonalSummaryResult[A] {
  def toOption = Some(value)

  def toEither = Right(value)
}
