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

import scala.language.experimental.macros

object CallSites {
  def isNoData(i: Int): Boolean = macro NoDataMacros.isNoDataInt_impl
  def isNoData(d: Double): Boolean = macro NoDataMacros.isNoDataDouble_impl

  def isData(i: Int): Boolean = macro NoDataMacros.isDataInt_impl

  def b2f(n: Byte): Float = macro TypeConversionMacros.b2f_impl
  def ub2i(n: Byte): Int = macro TypeConversionMacros.ub2i_impl
  def i2d(n: Int): Double = macro TypeConversionMacros.i2d_impl
  def d2i(n: Double): Int = macro TypeConversionMacros.d2i_impl
}

class TestTile(val data: Array[Int]) extends MacroMappableTile[TestTile] with MacroIterableTile {
  def mapIntMapper(mapper: IntTileMapper): TestTile =
    new TestTile(data.zipWithIndex.map { case (v, i) => mapper(i, 0, v) })

  def mapDoubleMapper(mapper: DoubleTileMapper): TestTile =
    new TestTile(data.zipWithIndex.map { case (v, i) => mapper(i, 0, v.toDouble).toInt })

  def foreachIntVisitor(visitor: IntTileVisitor): Unit =
    data.zipWithIndex.foreach { case (v, i) => visitor(i, 0, v) }

  def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit =
    data.zipWithIndex.foreach { case (v, i) => visitor(i, 0, v.toDouble) }

  def map(f: (Int, Int, Int) => Int): TestTile = macro TileMacros.intMap_impl[TestTile]
  def mapDouble(f: (Int, Int, Double) => Double): TestTile = macro TileMacros.doubleMap_impl[TestTile]
  def foreach(f: (Int, Int, Int) => Unit): Unit = macro TileMacros.intForeach_impl
  def foreachDouble(f: (Int, Int, Double) => Unit): Unit = macro TileMacros.doubleForeach_impl
}
