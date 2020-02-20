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

package geotrellis.raster.summary.polygonal.visitors

import cats.Monoid
import cats.syntax.monoid._
import geotrellis.raster._
import geotrellis.raster.summary.GridVisitor

import scala.reflect.ClassTag

/**
  * A helper for constructing visitors for Raster[Tile] as long as R conforms to Cats.Monoid
  *
  * In other words, as long as you can define a empty state and a combine operation on
  * your type R, this visitor can automatically generate a Visitor for polygonal summaries
  * on your type R.
  *
  * @param `monoid$R`
  * @param `classTag$R`
  * @tparam R
  */
abstract class TileCombineVisitor[R : Monoid : ClassTag] extends GridVisitor[Raster[Tile], R] {
  private var accumulator = Monoid[R].empty

  def result: R = accumulator

  def visit(raster: Raster[Tile], col: Int, row: Int) = {
    val newValue: R = fromDouble(raster.tile.getDouble(col, row))
    accumulator = accumulator.combine(newValue)
  }

  def fromDouble(value: Double): R
}
