/*
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright 2018 Astraea, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     [http://www.apache.org/licenses/LICENSE-2.0]
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package geotrellis.raster

/**
 * A tile that wraps another tile. Originally intended for delayed reading, but useful in other special use cases.
 *
 * @since 8/22/18
 */
abstract class DelegatingTile extends Tile {
  protected def delegate: Tile

  def cellType: CellType =
    delegate.cellType

  def cols: Int =
    delegate.cols

  def rows: Int =
    delegate.rows

  def mutable: MutableArrayTile =
    delegate.mutable

  def convert(cellType: CellType): Tile =
    delegate.convert(cellType)

  override def withNoData(noDataValue: Option[Double]): Tile =
    delegate.withNoData(noDataValue)

  def interpretAs(newCellType: CellType): Tile =
    delegate.interpretAs(newCellType)

  def get(col: Int, row: Int): Int =
    delegate.get(col, row)

  def getDouble(col: Int, row: Int): Double =
    delegate.getDouble(col, row)

  def toArrayTile(): ArrayTile =
    delegate.toArrayTile()

  def toArray(): Array[Int] =
    delegate.toArray()

  def toArrayDouble(): Array[Double] =
    delegate.toArrayDouble()

  def toBytes(): Array[Byte] =
    delegate.toBytes()

  def foreach(f: Int ⇒ Unit): Unit =
    delegate.foreach(f)

  def foreachDouble(f: Double ⇒ Unit): Unit =
    delegate.foreachDouble(f)

  def map(f: Int ⇒ Int): Tile =
    delegate.map(f)

  def combine(r2: Tile)(f: (Int, Int) ⇒ Int): Tile =
    delegate.combine(r2)(f)

  def mapDouble(f: Double ⇒ Double): Tile =
    delegate.mapDouble(f)

  def combineDouble(r2: Tile)(f: (Double, Double) ⇒ Double): Tile =
    delegate.combineDouble(r2)(f)

  def foreachIntVisitor(visitor: IntTileVisitor): Unit =
    delegate.foreachIntVisitor(visitor)

  def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit =
    delegate.foreachDoubleVisitor(visitor)

  def mapIntMapper(mapper: IntTileMapper): Tile =
    delegate.mapIntMapper(mapper)

  def mapDoubleMapper(mapper: DoubleTileMapper): Tile =
    delegate.mapDoubleMapper(mapper)

  override def toString: String = s"DelegatingTile($cols,$rows,$cellType)"
}
