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

package geotrellis.macros

import scala.quoted.*

object TileMacros {
  def intMap_impl[T: Type](self: Expr[MacroMappableTile[T]], f: Expr[(Int, Int, Int) => Int])(using Quotes): Expr[T] =
    '{ $self.mapIntMapper(new IntTileMapper { def apply(col: Int, row: Int, z: Int): Int = $f(col, row, z) }) }

  def doubleMap_impl[T: Type](self: Expr[MacroMappableTile[T]], f: Expr[(Int, Int, Double) => Double])(using Quotes): Expr[T] =
    '{ $self.mapDoubleMapper(new DoubleTileMapper { def apply(col: Int, row: Int, z: Double): Double = $f(col, row, z) }) }

  def intForeach_impl(self: Expr[MacroIterableTile], f: Expr[(Int, Int, Int) => Unit])(using Quotes): Expr[Unit] =
    '{ $self.foreachIntVisitor(new IntTileVisitor { def apply(col: Int, row: Int, z: Int): Unit = $f(col, row, z) }) }

  def doubleForeach_impl(self: Expr[MacroIterableTile], f: Expr[(Int, Int, Double) => Unit])(using Quotes): Expr[Unit] =
    '{ $self.foreachDoubleVisitor(new DoubleTileVisitor { def apply(col: Int, row: Int, z: Double): Unit = $f(col, row, z) }) }
}
