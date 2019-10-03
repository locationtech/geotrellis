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

package geotrellis.raster.density

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.Kernel

/**
  * Supplies functionality to operations that do convolution.
  */
trait KernelStamper {

  /**
    * Given a column, row, and value, apply the kernel at the given
    * point.
    */
  def stampKernel(col: Int, row: Int, z: Int): Unit

  /**
   * Given a (column, row) pair and a value, apply the kernel at the given point.
   */
  def stampKernel(tup: (Int, Int), z: Int): Unit = {
    val (col, row) = tup
    stampKernel(col, row, z)
  }

  /**
    * Given a column, row, and value, apply the kernel at the given
    * point.
    */
  def stampKernelDouble(col: Int, row: Int, z: Double): Unit

  /**
   * Given a (column, row) pair and a value, apply the kernel at the given point.
   */
  def stampKernelDouble(tup: (Int, Int), z: Double): Unit = {
    val (col, row) = tup
    stampKernelDouble(col, row, z)
  }

  def result: Tile
}

/**
  * The companion object for the [[KernelStamper]] trait.
  */
object KernelStamper {
  def apply(cellType: CellType, cols: Int, rows: Int, k: Kernel): KernelStamper =
    apply(ArrayTile.empty(cellType, cols, rows), k)

  def apply(tile: MutableArrayTile, k: Kernel): KernelStamper =
    if(k.cellType.isFloatingPoint) DoubleKernelStamper(tile, k)
    else IntKernelStamper(tile, k)
}

/**
  * A [[KernelStamper]] for double-valued tiles.
  */
case class DoubleKernelStamper(tile: MutableArrayTile, k: Kernel) extends KernelStamper {

  val ktile = k.tile
  val kernelcols = ktile.cols
  val kernelrows = ktile.rows

  val cellType = tile.cellType
  val Dimensions(cols, rows) = tile.dimensions

  /**
    * Given a column, row, and value, apply the kernel at the given
    * point.
    */
  def stampKernel(col: Int, row: Int, z: Int) = {
    if(z == 0) {
      val o = tile.get(col, row)
      tile.set(col, row,
        if(isNoData(o)) 0
        else o
      )
    } else {

      val rowmin = row - kernelrows / 2
      val rowmax = math.min(row + kernelrows / 2 + 1, rows)

      val colmin = col - kernelcols / 2
      val colmax = math.min(col + kernelcols / 2 + 1, cols)

      var kcol = 0
      var krow = 0

      var r = rowmin
      var c = colmin
      while(r < rowmax) {
        while(c < colmax) {
          if (r >= 0 && c >= 0 && r < rows && c < cols &&
            kcol >= 0 && krow >= 0 && kcol < kernelcols && krow < kernelrows) {

            val k = ktile.getDouble(kcol, krow)
            if (isData(k)) {
              val o = tile.get(c, r)
              val w =
                if (isNoData(o)) {
                  k * z
                } else {
                  o + k * z
                }
              tile.set(c, r, w.toInt)
            }
          }

          c += 1
          kcol += 1
        }

        kcol = 0
        c = colmin
        r += 1
        krow += 1
      }
    }
  }

  /**
    * Given a column, row, and value, apply the kernel at the given
    * point.
    */
  def stampKernelDouble(col: Int, row: Int, z: Double) = {
    if(z == 0.0) {
      val o = tile.getDouble(col, row)
      tile.setDouble(col, row,
        if(isNoData(o)) 0.0
        else o
      )
    } else {

      val rowmin = row - kernelrows / 2
      val rowmax = math.min(row + kernelrows / 2 + 1, rows)

      val colmin = col - kernelcols / 2
      val colmax = math.min(col + kernelcols / 2 + 1, cols)

      var kcol = 0
      var krow = 0

      var r = rowmin
      var c = colmin
      while(r < rowmax) {
        while(c < colmax) {
          if (r >= 0 && c >= 0 && r < rows && c < cols &&
            kcol >= 0 && krow >= 0 && kcol < kernelcols && krow < kernelrows) {

            val k = ktile.getDouble(kcol, krow)
            if (isData(k)) {
              val o = tile.getDouble(c, r)
              val w =
                if (isNoData(o)) {
                  k * z
                } else {
                  o + k * z
                }
              tile.setDouble(c, r, w)
            }
          }

          c += 1
          kcol += 1
        }

        kcol = 0
        c = colmin
        r += 1
        krow += 1
      }
    }
  }

  def result = tile
}

/**
  * A [[KernelStamper]] for integer-valued tiles.
  */
case class IntKernelStamper(tile: MutableArrayTile, k: Kernel) extends KernelStamper {

  val ktile = k.tile
  val kernelcols = ktile.cols
  val kernelrows = ktile.rows

  val cellType = tile.cellType
  val Dimensions(cols, rows) = tile.dimensions

  /**
    * Given a column, row, and value, apply the kernel at the given
    * point.
    */
  def stampKernel(col: Int, row: Int, z: Int) = {
    if(z == 0) {
      val o = tile.get(col, row)
      tile.set(col, row,
        if(isNoData(o)) 0
        else o
      )
    } else {

      val rowmin = row - kernelrows / 2
      val rowmax = math.min(row + kernelrows / 2 + 1, rows)

      val colmin = col - kernelcols / 2
      val colmax = math.min(col + kernelcols / 2 + 1, cols)

      var kcol = 0
      var krow = 0

      var r = rowmin
      var c = colmin
      while(r < rowmax) {
        while(c < colmax) {
          if (r >= 0 && c >= 0 && r < rows && c < cols &&
            kcol >= 0 && krow >= 0 && kcol < kernelcols && krow < kernelrows) {

            val k = ktile.get(kcol, krow)
            if (isData(k)) {
              val o = tile.get(c, r)
              val w =
                if (isNoData(o)) {
                  k * z
                } else {
                  o + k * z
                }
              tile.set(c, r, w.toInt)
            }
          }

          c += 1
          kcol += 1
        }

        kcol = 0
        c = colmin
        r += 1
        krow += 1
      }
    }
  }

  /**
    * Given a column, row, and value, apply the kernel at the given
    * point.
    */
  def stampKernelDouble(col: Int, row: Int, z: Double) = {
    if(z == 0.0) {
      val o = tile.getDouble(col, row)
      tile.setDouble(col, row,
        if(isNoData(o)) 0.0
        else o
      )
    } else {

      val rowmin = row - kernelrows / 2
      val rowmax = math.min(row + kernelrows / 2 + 1, rows)

      val colmin = col - kernelcols / 2
      val colmax = math.min(col + kernelcols / 2 + 1, cols)

      var kcol = 0
      var krow = 0

      var r = rowmin
      var c = colmin
      while(r < rowmax) {
        while(c < colmax) {
          if (r >= 0 && c >= 0 && r < rows && c < cols &&
            kcol >= 0 && krow >= 0 && kcol < kernelcols && krow < kernelrows) {

            val k = ktile.get(kcol, krow)
            if (isData(k)) {
              val o = tile.getDouble(c, r)
              val w =
                if (isNoData(o)) {
                  k * z
                } else {
                  o + k * z
                }
              tile.setDouble(c, r, w)
            }
          }

          c += 1
          kcol += 1
        }

        kcol = 0
        c = colmin
        r += 1
        krow += 1
      }
    }
  }

  def result = tile
}
