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

object CallSites {
  inline def isNoData(inline i: Int): Boolean = ${ NoDataMacros.isNoDataInt_impl('i) }
  inline def isNoData(inline d: Double): Boolean = ${ NoDataMacros.isNoDataDouble_impl('d) }

  inline def isData(inline i: Int): Boolean = ${ NoDataMacros.isDataInt_impl('i) }

  inline def b2f(inline n: Byte): Float = ${ TypeConversionMacros.b2f_impl('n) }
  inline def ub2i(inline n: Byte): Int = ${ TypeConversionMacros.ub2i_impl('n) }
  inline def i2d(inline n: Int): Double = ${ TypeConversionMacros.i2d_impl('n) }
  inline def d2i(inline n: Double): Int = ${ TypeConversionMacros.d2i_impl('n) }
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

  inline def map(inline f: (Int, Int, Int) => Int): TestTile =
    ${ TileMacros.intMap_impl[TestTile]('{ this.asInstanceOf[MacroMappableTile[TestTile]] }, 'f) }
  inline def mapDouble(inline f: (Int, Int, Double) => Double): TestTile =
    ${ TileMacros.doubleMap_impl[TestTile]('{ this.asInstanceOf[MacroMappableTile[TestTile]] }, 'f) }
  inline def foreach(inline f: (Int, Int, Int) => Unit): Unit =
    ${ TileMacros.intForeach_impl('this, 'f) }
  inline def foreachDouble(inline f: (Int, Int, Double) => Unit): Unit =
    ${ TileMacros.doubleForeach_impl('this, 'f) }
}
