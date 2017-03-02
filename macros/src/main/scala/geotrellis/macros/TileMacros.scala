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

import spire.macros.InlineUtil

import scala.reflect.macros.whitebox.Context

trait MacroIterableTile {
  def foreachIntVisitor(visitor: IntTileVisitor): Unit
  def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit
}

trait MacroMappableTile[T] {
  def mapIntMapper(mapper: IntTileMapper): T
  def mapDoubleMapper(mapper: DoubleTileMapper): T
}

trait IntTileMapper {
  def apply(col: Int, row: Int, z: Int): Int
}

trait DoubleTileMapper {
  def apply(col: Int, row: Int, z: Double): Double
}

trait IntTileVisitor {
  def apply(col: Int, row: Int, z: Int): Unit
}

trait DoubleTileVisitor {
  def apply(col: Int, row: Int, z: Double): Unit
}

object TileMacros {
  def intMap_impl[T <: MacroMappableTile[T]](c: Context)(f: c.Expr[(Int, Int, Int) => Int]): c.Expr[T] = {
    import c.universe._
    val self = c.Expr[MacroMappableTile[T]](c.prefix.tree)
    val tree = q"""$self.mapIntMapper(new geotrellis.macros.IntTileMapper { def apply(col: Int, row: Int, z: Int): Int = $f(col, row, z) })"""
    new InlineUtil[c.type](c).inlineAndReset[T](tree)
  }

  def doubleMap_impl[T <: MacroMappableTile[T]](c: Context)(f: c.Expr[(Int, Int, Double) => Double]): c.Expr[T] = {
    import c.universe._
    val self = c.Expr[MacroMappableTile[T]](c.prefix.tree)
    val tree = q"""$self.mapDoubleMapper(new geotrellis.macros.DoubleTileMapper { def apply(col: Int, row: Int, z: Double): Double = $f(col, row, z) })"""
    new InlineUtil[c.type](c).inlineAndReset[T](tree)
  }

  def intForeach_impl(c: Context)(f: c.Expr[(Int, Int, Int) => Unit]): c.Expr[Unit] = {
    import c.universe._
    val self = c.Expr[MacroIterableTile](c.prefix.tree)
    val tree = q"""$self.foreachIntVisitor(new geotrellis.macros.IntTileVisitor { def apply(col: Int, row: Int, z: Int): Unit = $f(col, row, z) })"""
    new InlineUtil[c.type](c).inlineAndReset[Unit](tree)
  }

  def doubleForeach_impl(c: Context)(f: c.Expr[(Int, Int, Double) => Unit]): c.Expr[Unit] = {
    import c.universe._
    val self = c.Expr[MacroIterableTile](c.prefix.tree)
    val tree = q"""$self.foreachDoubleVisitor(new geotrellis.macros.DoubleTileVisitor { def apply(col: Int, row: Int, z: Double): Unit = $f(col, row, z) })"""
    new InlineUtil[c.type](c).inlineAndReset[Unit](tree)
  }
}
